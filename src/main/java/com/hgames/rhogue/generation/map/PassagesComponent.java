package com.hgames.rhogue.generation.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hgames.lib.choice.DoublePriorityCell;
import com.hgames.lib.collection.Multimaps;
import com.hgames.lib.collection.pair.Pair;
import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.DungeonGenerator.ZoneType;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.zone.SingleCellZone;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * Generation of doors between almost adjacent rooms: generate doors/floors on
 * cells that are {@link DungeonSymbol#WALL} and which are between rooms.
 * 
 * @author smelC
 */
public class PassagesComponent extends SkeletalComponent {

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
		final IDungeonGeneratorListener listener = gen.listener;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final Coord doorCandidate = Coord.get(x, y);
				if (!isDoorCandidate(gdata, dungeon, doorCandidate, ZONE_PAIR_BUF))
					continue;
				final Zone z0 = ZONE_PAIR_BUF[0];
				assert z0 != null;
				final Zone z1 = ZONE_PAIR_BUF[1];
				assert z1 != null;
				if (z0 == z1)
					/* Can happen with weird zones (U shape) */
					continue;
				if (!acceptsOneMoreConnection(gen, dungeon, z0) || !acceptsOneMoreConnection(gen, dungeon, z1))
					/* Connection is disallowed */
					continue;
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
			if (!acceptsOneMoreConnection(gen, dungeon, z0) || !acceptsOneMoreConnection(gen, dungeon, z1))
				continue;
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
		if (forceDoors(gen, dungeon, z0) || forceDoors(gen, dungeon, z1))
			return true;
		else {
			final RNG rng = gen.rng;
			return rng.next(101) <= gen.doorProbability;
		}
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
