package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hgames.lib.collection.pair.MutablePair;
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.DungeonGenerator.ZoneType;
import com.hgames.rhogue.generation.map.lifetime.Lifetime;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.grid.GridIterators;
import com.hgames.rhogue.rng.ProbabilityTable;
import com.hgames.rhogue.zone.ListZone;
import com.hgames.rhogue.zone.Rectangle;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * How to generate rooms.
 * 
 * @author smelC
 */
public class RoomComponent implements GeneratorComponent {

	private final DungeonGenerator gen;
	private final GenerationData gdata;

	/**
	 * Provider for choosing the top left coordinate of rooms built and custom stuff
	 * to do before registering a zone built.
	 */
	private RoomGeneratorHelper rgh;

	/**
	 * For allocation-less iterations in method
	 * {@link #adjustSizeWithRoomGenerators}. Freed once it becomes useless
	 */
	private List<IRoomGenerator> roomGenerators;

	private static final MutablePair<IRoomGenerator, Zone> RGZ = MutablePair.createEmpty();

	RoomComponent(DungeonGenerator gen, GenerationData gdata) {
		this.gen = gen;
		this.gdata = gdata;
		this.roomGenerators = new ArrayList<IRoomGenerator>(gen.roomGenerators.getDomain());
	}

	@Override
	public boolean generate(final DungeonGenerator __, final GenerationData ____) {
		assert gen == __;
		assert gdata == ____;
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
				rgh = new RoomGeneratorHelper() {
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
					final boolean done = generateRoom(overwritten);
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
			rgh = new RoomGeneratorHelper() {

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
				final boolean done = generateRoom(overwritten);
				if (!done)
					/* Cannot place any more room */
					break;
			}
		}
		return true;
	}

	/**
	 * @param rg
	 *            The room generator used to generate {@code zone}.
	 * @param zone
	 *            The zone to add.
	 * @param boundingBox
	 *            {@code zone}'s bounding box, if required.
	 * @param ztype
	 * @param sym
	 *            The symbol to put in {@code zone}.
	 */
	public void addZone(IRoomGenerator rg, Zone zone, /* @Nullable */ Rectangle boundingBox, ZoneType ztype,
			DungeonSymbol sym) {
		final Dungeon dungeon = gdata.dungeon;
		assert !Dungeons.anyOnEdge(dungeon, zone.iterator()) : "Zone is on the dungeon's edge: " + zone;
		// infoLog("Generated room: " + zone);
		rgh.prepareRegistration(zone);
		/* Record the zone */
		gen.addZone(gdata, zone, boundingBox, rg, ztype);
		/* Punch it */
		final DungeonBuilder builder = dungeon.getBuilder();
		builder.setSymbols(zone.iterator(), sym);
		gen.draw(dungeon);
	}

	/**
	 * @author smelC
	 */
	private interface RoomGeneratorHelper extends CoordsProvider {

		void prepareRegistration(Zone z);

	}

	/**
	 * @param overwritten
	 *            The symbols that the rooms can overwrite.
	 * @return Whether a room could be generated.
	 */
	private boolean generateRoom(EnumSet<DungeonSymbol> overwritten) {
		final Dungeon dungeon = gdata.dungeon;
		final IDungeonGeneratorListener listener = gen.listener;
		final ProbabilityTable<IRoomGenerator> rgTable = getRoomGeneratorTable(overwritten);
		int frustration = 0;
		/*
		 * This bound is quite important. Increasing it makes dungeon generation slower,
		 * but creates more packed dungeons (more small rooms).
		 */
		/*
		 * Try a bit harder when #startWithWater is set, as these dungeons typically
		 * waste some space.
		 */
		final RNG rng = gen.rng;
		outer: while (frustration < 8 + (gen.startWithWater ? 4 : 0)) {
			frustration++;
			/* +1 to account for the surrounding wall */
			int maxWidth = getMaxRoomSideSize(true, rng.nextInt(10) == 0) + 1;
			int maxHeight = getMaxRoomSideSize(false, rng.nextInt(10) == 0) + 1;
			{
				final int max = Math.max(maxWidth, maxHeight);
				final int min = Math.min(maxWidth, maxHeight);
				if (min < max / 2) {
					/* Correct that, because such very long or wide rooms look weird */
					if (maxHeight < maxWidth)
						maxHeight = maxWidth / 2;
					else
						maxWidth = maxHeight / 2;
				}
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
				final boolean done = generateRoomAt(rgTable, blCandidate, mw, mh);
				if (!done)
					continue;
				assert RGZ.getFst() != null && RGZ.getSnd() != null;
				/*
				 * 'zone' must be used now, since the generator's usage has been recorded in
				 * 'generateRoomAt'.
				 */
				final IRoomGenerator rg = RGZ.getFst();
				final Zone zone = RGZ.getSnd();
				addZone(rg, zone, new Rectangle.Impl(blCandidate, mw, mh), ZoneType.ROOM, DungeonSymbol.FLOOR);
				if (listener != null)
					listener.placedRoom(dungeon, rg, zone);
				return true;
			}
			/* Unreachable */
			// assert false;
		}
		return false;
	}

	/** @return Whether a room was generated (recorded in {@link #RGZ}) */
	private boolean generateRoomAt(ProbabilityTable<IRoomGenerator> rgTable, Coord bottomLeft, int maxWidth_,
			int maxHeight_) {
		assert 1 <= maxWidth_;
		assert 1 <= maxHeight_;
		final RNG rng = gen.rng;
		final IRoomGenerator rg = rgTable.get(rng);
		if (rg == null)
			return false;
		// infoLog("Trying " + maxWidth + "x" + maxHeight + " room at " +
		// bottomLeft);

		final int maxWidth;
		final int maxHeight;
		{
			// Check that maxWidth and maxHeight meet the generator's specification
			final int minw = rg.getMinSideSize(true);
			if (0 <= minw && maxWidth_ < minw)
				/*
				 * The maximum width given by the caller is smaller than the generator's
				 * minimum. There's no way to meet the constraints.
				 */
				return false;
			final int minh = rg.getMinSideSize(false);
			if (0 <= minh && maxHeight_ < minh)
				/*
				 * The maximum height given by the caller is smaller than the generator's
				 * minimum. There's no way to meet the constraints.
				 */
				return false;
			/*
			 * Now union the constraints of the caller and of the room generator for the
			 * maximum sizes.
			 */
			final int rgMaxW = rg.getMaxSideSize(true);
			maxWidth = 0 <= rgMaxW ? Math.min(maxWidth_, rgMaxW) : maxWidth_;
			final int rgMaxH = rg.getMaxSideSize(false);
			maxHeight = 0 <= rgMaxH ? Math.min(maxHeight_, rgMaxH) : maxHeight_;
		}

		final Zone zeroZeroZone = rg.generate(rng, this, bottomLeft, maxWidth, maxHeight);
		if (zeroZeroZone == null)
			return false;
		final Zone zone = zeroZeroZone.translate(bottomLeft);
		assert zone.size() == zeroZeroZone.size();
		assert !Dungeons.anyOnEdge(gdata.dungeon, zone.iterator()) : "Room is on the dungeon's edge: " + zone;
		final ILogger logger = gen.logger;
		{
			/* Remember that generator is getting used */
			final Lifetime lifetime = gen.rgLifetimes.get(rg);
			if (lifetime == null) {
				if (logger != null && logger.isErrEnabled())
					logger.errLog(Tags.GENERATION,
							IRoomGenerator.class.getSimpleName() + " has no " + Lifetime.class.getSimpleName()
									+ " instance attached. This is a severe bug, let's hope for the best.");
			} else {
				lifetime.recordUsage();
				if (lifetime.shouldBeRemoved()) {
					/* Remove generator */
					gen.roomGenerators.remove(rg);
					gen.rgLifetimes.remove(rg);
					lifetime.removeCallback();
					if (logger != null && logger.isInfoEnabled())
						logger.infoLog(Tags.GENERATION, "Removed room generator: " + rg + ", because its "
								+ Lifetime.class.getSimpleName() + " is over.");
				}
			}
		}
		RGZ.clear();
		RGZ.setFst(rg);
		RGZ.setSnd(zone);
		return true;
	}

	private ProbabilityTable<IRoomGenerator> getRoomGeneratorTable(EnumSet<DungeonSymbol> overwritten) {
		final Collection<IRoomGenerator> domain = gen.roomGenerators.getDomain();
		Iterator<IRoomGenerator> it = domain.iterator();
		boolean needChange = false;
		while (it.hasNext() && !needChange) {
			final IRoomGenerator candidate = it.next();
			final /* @Nullable */ EnumSet<DungeonSymbol> neighbors = candidate.getAcceptedNeighbors();
			if (neighbors != null && !neighbors.containsAll(overwritten)) {
				/*
				 * Room generator refuses a possibly overwritten symbol, we need to remove it.
				 */
				needChange = true;
				break;
			}
		}
		if (!needChange)
			return gen.roomGenerators;
		/* Reboot iterator */
		it = domain.iterator();
		final ProbabilityTable<IRoomGenerator> result = ProbabilityTable.create();
		while (it.hasNext()) {
			final IRoomGenerator candidate = it.next();
			final /* @Nullable */ EnumSet<DungeonSymbol> neighbors = candidate.getAcceptedNeighbors();
			if (neighbors == null || neighbors.containsAll(overwritten))
				/* 'candidate' is compatible with 'overwritten' */
				result.add(candidate, gen.roomGenerators.weight(candidate));
			/* else skip it */
		}
		return result;
	}

	private int getMaxRoomSideSize(boolean widthOrHeight, boolean spiceItUp) {
		final RNG rng = gen.rng;
		int min = widthOrHeight ? gen.minRoomWidth : gen.minRoomHeight;
		if (min == 1 && !gen.allowWidthOrHeightOneRooms)
			min++;
		/*
		 * +1, because #maxRoomWidth and #maxRoomHeight are inclusive, whereas
		 * RNG#between isn't.
		 */
		int max = (widthOrHeight ? gen.maxRoomWidth : gen.maxRoomHeight) + 1;
		/* Now try to honor the possible IRoomGenerator specified sizes */
		min = adjustSizeWithRoomGenerators(widthOrHeight, true, min, max);
		max = adjustSizeWithRoomGenerators(widthOrHeight, false, min, max);

		final int result = rng.between(min, max);
		return result * (spiceItUp ? 2 : 1);
	}

	private int adjustSizeWithRoomGenerators(boolean widthOrHeight, boolean minOrMax, int min, int max) {
		int result = minOrMax ? min : max;
		if (roomGenerators == null)
			/* Remaining generators do not specify constraints */
			return result;
		final int nbrgs = roomGenerators.size();

		boolean isAConstraintPossible = false;

		for (int i = 0; i < nbrgs; i++) {
			final IRoomGenerator rg = roomGenerators.get(i);
			final int local = minOrMax ? rg.getMinSideSize(widthOrHeight) : rg.getMaxSideSize(widthOrHeight);
			if (local < 0)
				/* No constraint */
				continue;
			if (!gen.roomGenerators.getDomain().contains(rg))
				/* Room generator got unplugged */
				continue;
			/*
			 * Assigned before checking inInterval, because this value should be correct no
			 * matter the current values of min/max.
			 */
			isAConstraintPossible |= true;
			if (minOrMax) {
				if (result < local && local <= max) {
					/* Constraint of this generator changes something and can be honored */
					result = local;
				}
			} else if (min <= local && local < result) {
				/* Constraint of this generator changes something and can be honored */
				result = local;
			}
		}

		if (!isAConstraintPossible)
			/*
			 * To speed up later calls. Correct because the list of IRoomGenerator can only
			 * shrink.
			 */
			roomGenerators = null;

		return result;
	}

}
