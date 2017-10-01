package com.hgames.rhogue.generation.map;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.DungeonGenerator.ZoneType;
import com.hgames.rhogue.generation.map.lifetime.Lifetime;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.grid.GridIterators;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.ListZone;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * How to generate rooms.
 * 
 * @author smelC
 */
public class RoomComponent implements GeneratorComponent {

	@Override
	public boolean generate(final DungeonGenerator gen, final GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		if (gen.startWithWater) {
			/*
			 * Generation tuned to generate close to water pools. That's what allow to
			 * connect rooms and pools (the normal generation below is too restrictive to do
			 * that, since it only considers wall-only areas).
			 */
			final EnumSet<DungeonSymbol> overwritten = EnumSet.of(DungeonSymbol.WALL, DungeonSymbol.DEEP_WATER);
			final int nbPools = dungeon.waterPools == null ? 0 : dungeon.waterPools.size();
			final Set<ListZone> needCleanUp = new HashSet<ListZone>();
			nextPool: for (int i = 0; i < nbPools; i++) {
				final Zone waterPool = dungeon.waterPools.get(i);
				final int nbr = Math.max(1, waterPool.size() / 16);
				final RoomGeneratorHelper tlStarts = new RoomGeneratorHelper() {
					@Override
					public Iterator<Coord> getCoords() {
						final List<Coord> internalBorder = waterPool.getInternalBorder();
						return internalBorder.iterator();
					}

					@Override
					public void prepareRegistration(Zone z) {
						/*
						 * We need to remove the members of the room that overlap with water, hereby
						 * shrinking water pools.
						 */
						for (Coord inRoom : z) {
							for (int j = 0; j < nbPools; j++) {
								final ListZone pool = dungeon.waterPools.get(j);
								final boolean rmed = pool.getState().remove(inRoom);
								if (rmed) {
									needCleanUp.add(pool);
									gdata.cellToEncloser[inRoom.x][inRoom.y] = null;
								}
							}
						}
						gen.draw(dungeon);
					}
				};
				for (int j = 0; j < nbr; j++) {
					final boolean done = generateRoom(gen, gdata, tlStarts, overwritten);
					if (!done)
						continue nextPool;
				}
			}
			gen.cleanWaterPools(gdata, needCleanUp);
		}

		{
			/* Normal room generation */
			final EnumSet<DungeonSymbol> overwritten = EnumSet.of(DungeonSymbol.WALL);
			final int width = gen.width;
			final int height = gen.height;
			final RoomGeneratorHelper tlStarts = new RoomGeneratorHelper() {
				@Override
				public Iterator<Coord> getCoords() {
					return new GridIterators.RectangleRandomStartAndDirection(width, height, gen.rng.nextInt(width),
							gen.rng.nextInt(height), gen.rng.getRandomElement(Direction.CARDINALS));
				}

				@Override
				public void prepareRegistration(Zone z) {
					/* Nothing to do */
				}
			};
			while (true) {
				final boolean done = generateRoom(gen, gdata, tlStarts, overwritten);
				if (!done)
					/* Cannot place any more room */
					break;
			}
		}
		return true;
	}

	/**
	 * @author smelC
	 */
	private interface RoomGeneratorHelper extends CoordsProvider {

		void prepareRegistration(Zone z);

	}

	/**
	 * @param gdata
	 * @param rgh
	 *            Providers for choosing the top left coordinate of rooms built and
	 *            custom stuff to do before registering a zone built.
	 * @param overwritten
	 *            The symbols that the rooms can overwrite.
	 * @return Whether a room could be generated.
	 */
	private boolean generateRoom(DungeonGenerator gen, GenerationData gdata, RoomGeneratorHelper rgh,
			EnumSet<DungeonSymbol> overwritten) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		int frustration = 0;
		/*
		 * This bound is quite important. Increasing it makes dungeon generation slower,
		 * but creates more packed dungeons (more small rooms).
		 */
		/*
		 * Try a bit harder when #startWithWater is set, as these dungeons typically
		 * waster some space.
		 */
		final RNG rng = gen.rng;
		outer: while (frustration < 8 + (gen.startWithWater ? 4 : 0)) {
			frustration++;
			/* +1 to account for the surrounding wall */
			int maxWidth = getMaxRoomSideSize(gen, true, rng.nextInt(10) == 0) + 1;
			int maxHeight = getMaxRoomSideSize(gen, false, rng.nextInt(10) == 0) + 1;
			final int max = Math.max(maxWidth, maxHeight);
			final int min = Math.min(maxWidth, maxHeight);
			if (min < max / 2) {
				/* Correct that, because such very long or wide rooms look weird */
				if (maxHeight < maxWidth)
					maxHeight = maxWidth / 2;
				else
					maxWidth = maxHeight / 2;
			}

			/* Top-left coordinate */
			final Iterator<Coord> tlPlacer = rgh.getCoords();
			while (true) {
				if (!tlPlacer.hasNext())
					continue outer;
				final Coord tlCandidate = tlPlacer.next();
				if (Dungeons.isOnEdge(dungeon, tlCandidate))
					continue;
				/* To avoid the room to be on the edge */
				final int mw = Math.min(maxWidth, gen.width - (tlCandidate.x + 2));
				final int mh = Math.min(maxHeight, gen.height - (tlCandidate.y + 2));
				if (mw == 0 || mh == 0)
					/* Cannot do */
					continue;
				if (!gen.allowWidthOrHeightOneRooms && (mw == 1 || mh == 1))
					/* Should not do */
					continue;
				assert 2 <= mw && 2 <= mh;
				/* Bottom-right cell */
				final Coord brCandidate = Coord.get(tlCandidate.x + mw, tlCandidate.y + mh);
				final Coord blCandidate = Coord.get(tlCandidate.x, brCandidate.y);
				/*
				 * .extend() to avoid generating adjacent rooms. This is a smart trick (as
				 * opposed to extending the rooms already created).
				 */
				if (!Dungeons.isOnly(dungeon, Rectangle.Utils.cells(new Rectangle.Impl(blCandidate, mw, mh).extend()),
						overwritten, true))
					continue;
				assert dungeon.isValid(brCandidate);
				assert !Dungeons.isOnEdge(dungeon, brCandidate);
				final Zone zone = generateRoomAt(gen, gdata, blCandidate, mw, mh);
				/*
				 * 'zone' must be used now, since the generator's usage has been recorded in
				 * 'generateRoomAt'.
				 */
				if (zone != null) {
					assert !Dungeons.anyOnEdge(dungeon, zone.iterator()) : "Zone is on the dungeon's edge: " + zone;
					// infoLog("Generated room: " + zone);
					rgh.prepareRegistration(zone);
					/* Record the zone */
					gen.addZone(gdata, zone, new Rectangle.Impl(blCandidate, mw, mh), ZoneType.ROOM);
					/* Punch it */
					builder.setSymbols(zone.iterator(), DungeonSymbol.FLOOR);
					gen.draw(dungeon);
					return true;
				}
			}
			/* Unreachable */
			// assert false;
		}
		return false;
	}

	private /* @Nullable */ Zone generateRoomAt(DungeonGenerator gen, GenerationData gdata, Coord bottomLeft,
			int maxWidth, int maxHeight) {
		assert 1 <= maxWidth;
		assert 1 <= maxHeight;
		final RNG rng = gen.rng;
		final IRoomGenerator rg = gen.roomGenerators.get(rng);
		if (rg == null)
			return null;
		// infoLog("Trying " + maxWidth + "x" + maxHeight + " room at " +
		// bottomLeft);
		final Zone zeroZeroZone = rg.generate(gdata.dungeon, bottomLeft, maxWidth, maxHeight);
		if (zeroZeroZone == null)
			return null;
		final Zone zone = zeroZeroZone.translate(bottomLeft);
		assert zone.size() == zeroZeroZone.size();
		assert !Dungeons.anyOnEdge(gdata.dungeon, zone.iterator()) : "Room is on the dungeon's edge: " + zone;
		{
			/* Remember that generator is getting used */
			final Lifetime lifetime = gen.rgLifetimes.get(rg);
			if (lifetime == null)
				throw new IllegalStateException(IRoomGenerator.class.getSimpleName() + " has no "
						+ Lifetime.class.getSimpleName() + " instance attached");
			lifetime.recordUsage();
			if (lifetime.shouldBeRemoved()) {
				/* Remove generator */
				gen.roomGenerators.remove(rg);
				gen.rgLifetimes.remove(rg);
				lifetime.removeCallback();
			}
		}
		return zone;
	}

	// FIXME CH Add a parameter to control variance
	protected int getMaxRoomSideSize(DungeonGenerator gen, boolean widthOrHeight, boolean spiceItUp) {
		final RNG rng = gen.rng;
		int min = widthOrHeight ? gen.minRoomWidth : gen.minRoomHeight;
		if (min == 1 && !gen.allowWidthOrHeightOneRooms)
			min++;
		/*
		 * +1, because #maxRoomWidth and #maxRoomHeight are inclusive, where RNG#between
		 * isn't.
		 */
		final int max = (widthOrHeight ? gen.maxRoomWidth : gen.maxRoomHeight) + 1;
		final int result = rng.between(min, max);
		return result * (spiceItUp ? 2 : 1);
	}

}
