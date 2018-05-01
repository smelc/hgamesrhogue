package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hgames.lib.choice.DoublePriorityCell;
import com.hgames.lib.choice.PriorityCell3;
import com.hgames.lib.collection.list.Lists;
import com.hgames.lib.collection.pair.Pair;
import com.hgames.lib.collection.set.Sets;
import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.DungeonGenerator.ICorridorControl;
import com.hgames.rhogue.generation.map.DungeonGenerator.ZoneType;
import com.hgames.rhogue.generation.map.corridor.ICorridorBuilder;
import com.hgames.rhogue.zone.Rectangle;
import com.hgames.rhogue.zone.SingleCellZone;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * Generation of corridors.
 * 
 * @author smelC
 */
public class CorridorsComponent extends SkeletalComponent {

	protected final Collection<Zone> rooms;
	protected final List<Zone> dests;
	protected final ICorridorControl control;

	private static final PriorityCell3<Zone, Coord, Coord> ZCC_CELL = PriorityCell3.createEmpty();
	private static final DoublePriorityCell<Coord> DP_CELL = DoublePriorityCell.createEmpty();
	private static final Comparator<Pair<Double, Zone>> ORDERER = new Comparator<Pair<Double, Zone>>() {
		@Override
		public int compare(Pair<Double, Zone> o1, Pair<Double, Zone> o2) {
			return Double.compare(o1.getFst().doubleValue(), o2.getFst().doubleValue());
		}
	};

	/**
	 * @param rooms
	 *            The rooms from which to generate corridors.
	 * @param dests
	 *            The possible destinations.
	 * @param control
	 */
	CorridorsComponent(Collection<Zone> rooms, List<Zone> dests, ICorridorControl control) {
		this.rooms = rooms;
		this.dests = dests;
		this.control = control;
	}

	@Override
	public boolean generate(DungeonGenerator gen, GenerationData gdata) {
		final int lenLimit = control.getLengthLimit();
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		final int nbr = rooms.size();
		/* A Zone, to the other zones; ordered by the distance of the centers */
		final Map<Zone, List<Pair<Double, Zone>>> zoneToOtherZones = new LinkedHashMap<Zone, List<Pair<Double, Zone>>>(
				nbr);
		final int nbd = dests.size();
		boolean someChance = false;
		for (Zone z : rooms) {
			assert Dungeons.isRoom(dungeon, z);
			if (!acceptsOneMoreConnection(gen, dungeon, z))
				continue;
			final Coord zc = z.getCenter();
			assert !zoneToOtherZones.keySet().contains(z);
			final List<Pair<Double, Zone>> otherZones = new ArrayList<Pair<Double, Zone>>(Math.max(0, nbr - 1));
			for (int j = 0; j < nbd; j++) {
				final Zone other = dests.get(j);
				if (!acceptsOneMoreConnection(gen, dungeon, other))
					continue;
				assert Dungeons.isRoom(dungeon, other);
				if (other == z)
					continue;
				final Coord oc = other.getCenter();
				final double dist = zc.distance(oc);
				if (lenLimit < dist) {
					/* Too far away */
					continue;
				}
				otherZones.add(Pair.<Double, Zone>of(Double.valueOf(dist), other));
			}
			if (!otherZones.isEmpty()) {
				zoneToOtherZones.put(z, otherZones);
				someChance |= true;
			}
		}
		if (!someChance)
			return false;
		final boolean perfect = control.getPerfect();
		boolean needWaterPoolsCleanup = false;
		final Set<Coord> buf = Sets.newHashSet();
		int result = 0;
		final Coord[] startEndBuffer = new Coord[2];
		final List<Coord> buf1 = Lists.newArrayList();
		final List<Coord> buf2 = Lists.newArrayList();
		for (Zone z : zoneToOtherZones.keySet()) {
			final List<Pair<Double, Zone>> candidateDests = zoneToOtherZones.get(z);
			if (candidateDests == null)
				continue;
			final int nbcd = candidateDests.size();
			Collections.sort(candidateDests, ORDERER);
			int connections = dungeon.getNeighbors(z).size();
			final int maxConnections = gen.getRoomGenerator(dungeon, z).getMaxConnections();
			for (int j = 0; j < nbcd && connections < maxConnections; j++) {
				final Zone dest = candidateDests.get(j).getSnd();
				if (!acceptsOneMoreConnection(gen, dungeon, dest))
					continue;
				if (Dungeons.areConnected(dungeon, z, dest, 6))
					continue;
				final boolean success = generateCorridor(gen, gdata, z, dest, buf1, buf2, startEndBuffer);
				if (!success)
					continue;
				final Zone built = ZCC_CELL.get1();
				final Coord zDoor = ZCC_CELL.get2();
				final Coord destDoor = ZCC_CELL.get3();
				assert zDoor != null && destDoor != null;
				assert built.contains(zDoor) && built.contains(destDoor);
				assert !Dungeons.getSymbols(dungeon, built).contains(DungeonSymbol.CHASM);
				// (NO_CORRIDOR_BBOX) (if built is a ZoneUnion)
				if (perfect) {
					assert com.hgames.lib.collection.Collections.subset(Dungeons.getSymbols(dungeon, built),
							EnumSet.of(DungeonSymbol.WALL));
				} else {
					/* Corridor can go through DEEP_WATER */
					assert com.hgames.lib.collection.Collections.subset(Dungeons.getSymbols(dungeon, built),
							EnumSet.of(DungeonSymbol.WALL, DungeonSymbol.DEEP_WATER));
					if (buf != null)
						buf.clear();
					final boolean rm = builder.removeFromWaterPools(built, buf);
					if (rm) {
						needWaterPoolsCleanup = true;
						for (Coord c : buf) {
							gdata.cellToEncloser[c.x][c.y] = null;
							/*
							 * To preserve invariant that unbound cell have the WALL symbol.
							 */
							builder.setSymbol(c, DungeonSymbol.WALL);
						}
						gen.draw(dungeon);
					}
				}
				final Zone recorded = gen.addZone(gdata, built, null, null, ZoneType.CORRIDOR);
				builder.addConnection(z, recorded);
				builder.addConnection(dest, recorded);
				// Punch corridor
				for (Coord c : built) {
					if (c.equals(zDoor) || c.equals(destDoor))
						/* Done after the loop */
						continue;
					final boolean shallowWater = buf != null && buf.contains(c);
					builder.setSymbol(c, shallowWater ? DungeonSymbol.SHALLOW_WATER : DungeonSymbol.FLOOR);
				}
				/*
				 * Must be done after the loop above, because we check adequacy of walkable
				 * cells which is set partially by the loop.
				 */
				punchCorridorExtremity(gen, gdata, z, recorded, zDoor, buf != null && buf.contains(zDoor));
				punchCorridorExtremity(gen, gdata, dest, recorded, destDoor, buf != null && buf.contains(destDoor));
				gen.draw(dungeon);
				result++;
				connections++;
			}
		}
		if (needWaterPoolsCleanup)
			gen.cleanWaterPools(gdata, null);
		return 0 < result;
	}

	/**
	 * @param gen
	 * @param gdata
	 * @param room
	 *            The room being connected.
	 * @param corridor
	 *            The corridor built.
	 * @param c
	 * @param shallowWater
	 *            If shallow water must be put on {@code c}.
	 */
	private void punchCorridorExtremity(DungeonGenerator gen, GenerationData gdata, Zone room, Zone corridor, Coord c,
			boolean shallowWater) {
		final Dungeon dungeon = gdata.dungeon;
		assert dungeon.getRooms().contains(room);
		boolean door = false;
		if (!shallowWater) {
			door = forceDoors(gen, dungeon, room);
			if (!door) {
				final IRNG rng = gen.rng;
				door |= rng.nextInt(101) <= gen.doorProbability;
			}
			door &= isDoorCandidate(gdata, dungeon, c, null);
		}
		assert !(shallowWater && door);
		/* Do it */
		final DungeonBuilder builder = dungeon.getBuilder();
		final DungeonSymbol sym = shallowWater ? DungeonSymbol.SHALLOW_WATER
				: (door ? DungeonSymbol.DOOR : DungeonSymbol.FLOOR);
		builder.setSymbol(c, sym);
		if (door) {
			final IDungeonGeneratorListener listener = gen.listener;
			if (listener != null)
				listener.punchedDoor(dungeon, gen.getRoomGenerator(dungeon, room), room, c, null, corridor);
		}
	}

	/** @return Whether a corridor was found */
	private boolean generateCorridor(DungeonGenerator gen, GenerationData gdata, Zone src, Zone dest, List<Coord> buf1,
			List<Coord> buf2, Coord[] startEndBuffer) {
		/* This is a bit tartelette aux concombres */
		boolean found = getZonesConnectionEndpoints(gdata, src, dest, buf1, buf2, false);
		boolean alternativeAvailable = control.force();
		if (!found && alternativeAvailable) {
			/* Try an alternative */
			found = getZonesConnectionEndpoints(gdata, src, dest, buf1, buf2, true);
			alternativeAvailable = false;
		}
		if (!found)
			return false;
		assert !buf1.isEmpty() && !buf2.isEmpty();
		generateCorridor0(gen, buf1, buf2, startEndBuffer);
		Zone result = ZCC_CELL.get1();
		if (result == null && alternativeAvailable) {
			/* Alternative endpoints weren't try before. Try them now. */
			found = getZonesConnectionEndpoints(gdata, src, dest, buf1, buf2, true);
			alternativeAvailable = false;
			if (found) {
				assert !buf1.isEmpty() && !buf2.isEmpty();
				generateCorridor0(gen, buf1, buf2, startEndBuffer);
				result = ZCC_CELL.get1();
			}
		}
		return result != null;
	}

	/** Result is in {@link #ZCC_CELL}. */
	private void generateCorridor0(DungeonGenerator gen, List<Coord> connections1, List<Coord> connections2,
			Coord[] startEndBuffer) {
		assert !connections1.isEmpty() && !connections2.isEmpty();
		ZCC_CELL.clear();
		final ICorridorBuilder builder = control.getBuilder();
		final int limit = control.getLengthLimit();
		final int b1sz = connections1.size();
		final int b2sz = connections2.size();
		for (int k = 0; k < b1sz; k++) {
			final Coord zEndpoint = connections1.get(k);
			for (int l = 0; l < b2sz; l++) {
				final Coord destEndpoint = connections2.get(l);
				final Zone built = builder.build(gen.rng, zEndpoint, destEndpoint, startEndBuffer);
				if (built == null) {
					// builder.setSymbol(zEndpoint, DungeonSymbol.HIGH_GRASS);
					// builder.setSymbol(destEndpoint, DungeonSymbol.HIGH_GRASS);
					continue;
				}
				if (limit < Integer.MAX_VALUE && limit < built.size())
					continue;
				assert !built.contains(zEndpoint);
				assert !built.contains(destEndpoint);
				final Coord cStart = startEndBuffer[0];
				final Coord cEnd = startEndBuffer[1];
				assert built.contains(cStart) : "Corridor built: " + built + " doesn't contain corridor doorway: "
						+ cStart;
				assert built.contains(cEnd) : "Corridor built: " + built + " doesn't contain corridor endway: " + cEnd;
				/* Favor turnless corridors */
				final int prio = ((built instanceof Rectangle || built.size() == 1) ? 1 : 2) * built.size();
				ZCC_CELL.union(built, cStart, cEnd, prio);
			}
		}
	}

	/**
	 * @param alternative
	 *            Whether to try alternatives.
	 * @return Whether something was found.
	 */
	private boolean getZonesConnectionEndpoints(GenerationData gdata, Zone z1, Zone z2, List<Coord> buf1,
			List<Coord> buf2, boolean alternative) {
		if (z1 == z2) {
			assert false;
			return false;
		}
		final Dungeon dungeon = gdata.dungeon;
		assert Dungeons.hasRoomOrCorridor(dungeon, z1);
		assert Dungeons.hasRoomOrCorridor(dungeon, z2);
		final Direction fromz1toz2 = toGoFromZoneToZone(z1.getCenter(), z2.getCenter(), alternative);
		assert fromz1toz2 != Direction.NONE;
		getConnectionCandidates(gdata, z1, fromz1toz2, buf1);
		if (buf1.isEmpty())
			return false;
		getConnectionCandidates(gdata, z2, fromz1toz2.opposite(), buf2);
		if (buf2.isEmpty())
			return false;
		return true;
	}

	/**
	 * Fills {@code buf} with the candidates for building a connection towards
	 * {@code dir} in {@code z}. They should all belong to {@code z}
	 * {@link Zone#getInternalBorder() internal border}.
	 * 
	 * @param gdata
	 * @param z
	 * @param dir
	 * @return Whether something was found.
	 */
	private boolean getConnectionCandidates(GenerationData gdata, Zone z, Direction dir, List<Coord> buf) {
		if (dir == Direction.NONE)
			throw new IllegalStateException();
		buf.clear();
		final boolean zIsARectangle = z instanceof Rectangle;
		if (zIsARectangle && dir.isCardinal()) {
			// (NO-BBOX)
			// Rectangle.Utils.getBorder requires a cardinal direction
			Rectangle.Utils.getBorder((Rectangle) z, dir, buf);
		} else if (z instanceof SingleCellZone) {
			// (NO-BBOX)
			buf.add(z.getCenter());
		} else {
			final Rectangle bbox = zIsARectangle ? (Rectangle) z : gdata.dungeon.boundingBoxes.get(z);
			assert bbox != null;
			final Coord corner = Rectangle.Utils.getCorner(bbox, dir);
			assert bbox.contains(corner);
			DP_CELL.clear();
			final List<Coord> all = z.getAll(false);
			final int sz = all.size();
			for (int i = 0; i < sz; i++) {
				final Coord c = all.get(i);
				DP_CELL.union(c, c.distance(corner));
			}
			final Coord coord = DP_CELL.get();
			if (coord != null)
				buf.add(coord);
		}
		return !buf.isEmpty();
	}

	private static Direction toGoFromZoneToZone(Coord c1, Coord c2, boolean cardinalsOnly) {
		if (cardinalsOnly) {
			// To return cardinals only: (look bad. It's hard to explain but try it,
			// there will be too many straight corridors).
			final int x = c1.x - c2.x;
			final int y = c1.y - c2.y;
			return Direction.getCardinalDirection(x, y);
		} else
			return Direction.toGoTo(c1, c2);
	}

}
