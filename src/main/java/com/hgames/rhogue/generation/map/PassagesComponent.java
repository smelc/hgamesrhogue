package com.hgames.rhogue.generation.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hgames.lib.Exceptions;
import com.hgames.lib.choice.DoublePriorityCell;
import com.hgames.lib.collection.Multimaps;
import com.hgames.lib.collection.pair.Pair;
import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.DungeonGenerator.ZoneType;
import com.hgames.rhogue.generation.map.connection.IConnectionControl;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.zone.SingleCellZone;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * Generation of doors between almost adjacent rooms: generate doors/floors on
 * cells that are {@link DungeonSymbol#WALL} and which are between rooms.
 * 
 * @author smelC
 */
public class PassagesComponent implements GeneratorComponent {

	private static final Zone[] ZONE_PAIR_BUF = new Zone[2];

	@Override
	public boolean generate(DungeonGenerator gen, GenerationData gdata) {
		final int width = gen.width;
		final int height = gen.height;
		final Dungeon dungeon = gdata.dungeon;
		/* Keys in this map are ordered according to GenerationData.zOrder */
		/* The Lists do not contain doublons */
		final Map<Pair<Zone, Zone>, List<Coord>> connectedsToCandidates = new HashMap<Pair<Zone, Zone>, List<Coord>>(
				16);
		final IConnectionControl control = gen.connectionControl;
		final IDungeonGeneratorListener listener = gen.listener;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (!isDoorCandidate(gdata, x, y, true) && !isDoorCandidate(gdata, x, y, false))
					continue;
				final Zone z0 = ZONE_PAIR_BUF[0];
				assert z0 != null;
				final Zone z1 = ZONE_PAIR_BUF[1];
				assert z1 != null;
				if (z0 == z1)
					/* Can happen with weird zones (U shape) */
					continue;
				if (control != null) {
					final IRoomGenerator g0 = gen.getRoomGenerator(dungeon, z0);
					final IRoomGenerator g1 = gen.getRoomGenerator(dungeon, z1);
					if (!control.acceptsConnection(gen, dungeon, g0, z0, g1, z1))
						/* Connection is disallowed */
						continue;
				}
				final Coord doorCandidate = Coord.get(x, y);
				Multimaps.addToListMultimap(connectedsToCandidates, orderedPair(gdata, z0, z1), doorCandidate);
				assert Dungeons.findZoneContaining(dungeon, x, y) == null : "Candidate for door: " + doorCandidate
						+ " should not be in a zone";
			}
		}
		/*
		 * Look for the door closest to the mean of the zones' centers. That's the ideal
		 * door.
		 */
		final DoublePriorityCell<Coord> cell = DoublePriorityCell.createEmpty();
		for (Map.Entry<Pair<Zone, Zone>, List<Coord>> entry : connectedsToCandidates.entrySet()) {
			cell.clear();
			final Pair<Zone, Zone> connecteds = entry.getKey();
			final Zone z0 = connecteds.getFst();
			final Zone z1 = connecteds.getSnd();
			assert !Dungeons.areConnected(dungeon, z0, z1, 1);
			final Coord ideal;
			{
				final Coord center0 = z0.getCenter();
				final Coord center1 = z1.getCenter();
				ideal = Coord.get((center0.x + center1.x) / 2, (center0.y + center1.y / 2));
			}
			final List<Coord> candidates = entry.getValue();
			final int nbc = candidates.size();
			assert 0 < nbc;
			for (int i = 0; i < nbc; i++) {
				final Coord candidate = candidates.get(i);
				cell.union(candidate, candidate.distance(ideal));
			}
			final Coord door = cell.get();
			assert door != null;
			final boolean doorOrFloor = doorOrFloor(gen, dungeon, z0, z1);
			final DungeonSymbol sym = doorOrFloor ? DungeonSymbol.DOOR : DungeonSymbol.FLOOR;
			final Zone zdoor = new SingleCellZone(door);
			gen.addZone(gdata, zdoor, null, null, ZoneType.CORRIDOR);
			final DungeonBuilder builder = dungeon.getBuilder();
			builder.addConnection(z0, zdoor);
			builder.addConnection(z1, zdoor);
			builder.setSymbol(door.x, door.y, sym);
			gen.draw(dungeon);
			if (listener != null && doorOrFloor) {
				final IRoomGenerator g0 = gen.getRoomGenerator(dungeon, z0);
				final IRoomGenerator g1 = gen.getRoomGenerator(dungeon, z1);
				listener.punchedDoor(dungeon, g0, z0, door, g1, z1);
			}
		}
		return true;
	}

	/** @return Whether a door should be created or a wall should be carved */
	private boolean doorOrFloor(DungeonGenerator gen, Dungeon dungeon, Zone z0, Zone z1) {
		final IConnectionControl control = gen.connectionControl;
		if (control != null && (control.forceDoor(gen, dungeon, gen.getRoomGenerator(dungeon, z0), z0)
				|| control.forceDoor(gen, dungeon, gen.getRoomGenerator(dungeon, z1), z1))) {
			System.out.println("Forcing door");
			return true;
		} else {
			final RNG rng = gen.rng;
			return rng.next(101) <= gen.doorProbability;
		}
	}

	/**
	 * @param gdata
	 * @param x
	 * @param y
	 * @param southNorthOrEastWest
	 * @return Whether {@code (x,y)} is a valid door candidate, i.e. it has a valid
	 *         walkable cell (according to
	 *         {@link #isDoorNeighborCandidate(DungeonSymbol)}) to its left and
	 *         right (if {@code southNorthOrEastWest} is set, otherwise north and
	 *         south are checked).
	 * 
	 *         <p>
	 *         If it returns {@code true}, {@link #ZONE_PAIR_BUF} is filled with the
	 *         zones that could be connected by the door.
	 *         </p>
	 */
	private boolean isDoorCandidate(GenerationData gdata, int x, int y, boolean southNorthOrEastWest) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonSymbol sym = dungeon.getSymbol(x, y);
		if (sym == null)
			return false;
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case FLOOR:
		case GRASS:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
			return false;
		case WALL:
			break;
		}
		/* As in CorridorsComponent */
		final int x1 = x + (southNorthOrEastWest ? Direction.DOWN.deltaX : Direction.LEFT.deltaX);
		final int y1 = y + (southNorthOrEastWest ? Direction.DOWN.deltaY : Direction.LEFT.deltaY);
		if (!isDoorNeighborCandidate(dungeon.getSymbol(x1, y1)))
			return false;
		final int x2 = x + (southNorthOrEastWest ? Direction.UP.deltaX : Direction.RIGHT.deltaX);
		final int y2 = y + (southNorthOrEastWest ? Direction.UP.deltaY : Direction.RIGHT.deltaY);
		if (!isDoorNeighborCandidate(dungeon.getSymbol(x2, y2)))
			return false;
		// Using gdata's cellToEncloser cache isn't necessary for performances
		final Zone z1 = gdata.findZoneContaining(x1, y1);
		if (z1 == null) {
			assert false;
			throw new IllegalStateException("Cannot find zone containing " + x1 + "," + y1);
		}
		assert Dungeons.hasRoomOrCorridor(dungeon, z1);
		final Zone z2 = gdata.findZoneContaining(x2, y2);
		if (z2 == null) {
			assert false;
			throw new IllegalStateException("Cannot find zone containing " + x1 + "," + y1);
		}
		assert Dungeons.hasRoomOrCorridor(dungeon, z2);
		ZONE_PAIR_BUF[0] = z1;
		ZONE_PAIR_BUF[1] = z2;
		return true;
	}

	static boolean isDoorNeighborCandidate(/* @Nullable */ DungeonSymbol sym) {
		if (sym == null)
			return false;
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case HIGH_GRASS:
		case GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
		case WALL:
			return false;
		case FLOOR:
			return true;
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

	private static Pair<Zone, Zone> orderedPair(GenerationData gdata, Zone z1, Zone z2) {
		final Integer i1 = gdata.zOrder.get(z1);
		if (i1 == null)
			throw new IllegalStateException("Unknown zone: " + z1);
		final Integer i2 = gdata.zOrder.get(z2);
		if (i2 == null)
			throw new IllegalStateException("Unknown zone: " + z2);
		return i1 < i2 ? Pair.of(z1, z2) : Pair.of(z2, z1);
	}

}
