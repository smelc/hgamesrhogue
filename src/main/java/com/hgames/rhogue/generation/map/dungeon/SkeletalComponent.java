package com.hgames.rhogue.generation.map.dungeon;

import com.hgames.lib.Exceptions;
import com.hgames.rhogue.generation.map.dungeon.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public abstract class SkeletalComponent implements GeneratorComponent {

	/**
	 * @param gen
	 * @param dungeon
	 * @param room
	 * @return Whether {@code room} can receive an additional connection.
	 */
	protected boolean acceptsOneMoreConnection(DungeonGenerator gen, Dungeon dungeon, Zone room) {
		assert dungeon.getRooms().contains(room);
		final IRoomGenerator rg = gen.getRoomGenerator(dungeon, room);
		final int max = rg.getMaxConnections();
		if (max == Integer.MAX_VALUE)
			return true;
		else
			return Dungeons.getNumberOfConnections(dungeon, room) < max;
	}

	/**
	 * @param gen
	 * @param dungeon
	 * @param room
	 * @return Whether doors to {@code room} should be forced.
	 */
	protected boolean forceDoors(DungeonGenerator gen, Dungeon dungeon, Zone room) {
		assert dungeon.getRooms().contains(room);
		final IRoomGenerator rg = gen.getRoomGenerator(dungeon, room);
		return rg.getForceDoors();
	}

	protected boolean isDoorCandidate(GenerationData gdata, Dungeon dungeon, Coord c, Zone[] buf) {
		return isDoorCandidate(gdata, dungeon, c, buf, true) || isDoorCandidate(gdata, dungeon, c, buf, false);
	}

	/**
	 * @param gdata
	 * @param dungeon
	 * @param c
	 * @param southNorthOrEastWest
	 *            The direction in which the door would allow a move.
	 * @return Whether {@code (x,y)} is a valid door candidate.
	 * 
	 *         <p>
	 *         If it returns {@code true}, {@code buf} is non-null; it is filled
	 *         with the zones that could be connected by the door.
	 *         </p>
	 */
	protected boolean isDoorCandidate(GenerationData gdata, Dungeon dungeon, Coord c, Zone[] buf,
			boolean southNorthOrEastWest) {
		if (!isDoorCandidate(dungeon.getSymbol(c)))
			return false;
		if (Dungeons.hasNeighbor(dungeon, c, DungeonSymbol.DOOR, false))
			return false;
		final Direction walkableDir1 = southNorthOrEastWest ? Direction.DOWN : Direction.LEFT;
		final Coord walkableNeighbor1 = c.translate(walkableDir1);
		if (!isDoorNeighborWalkableCandidate(dungeon.getSymbol(walkableNeighbor1)))
			return false;
		final Direction walkableDir2 = southNorthOrEastWest ? Direction.UP : Direction.RIGHT;
		final Coord walkableNeighbor2 = c.translate(walkableDir2);
		if (!isDoorNeighborWalkableCandidate(dungeon.getSymbol(walkableNeighbor2)))
			return false;
		{
			final Direction impassableDir1 = southNorthOrEastWest ? Direction.LEFT : Direction.DOWN;
			final Direction impassableDir2 = southNorthOrEastWest ? Direction.RIGHT : Direction.UP;
			if (!isDoorNeighborImpassableCandidate(dungeon.getSymbol(c.translate(impassableDir1))))
				return false;
			if (!isDoorNeighborImpassableCandidate(dungeon.getSymbol(c.translate(impassableDir2))))
				return false;
		}
		if (buf != null) {
			assert 2 <= buf.length;
			// Using gdata's cellToEncloser cache isn't necessary for performances
			final int x1 = walkableNeighbor1.x;
			final int y1 = walkableNeighbor1.y;
			final Zone z1 = gdata.findZoneContaining(x1, y1);
			if (z1 == null) {
				assert false;
				throw new IllegalStateException("Cannot find zone containing " + x1 + "," + y1);
			}
			assert Dungeons.hasRoomOrCorridor(dungeon, z1);
			final int x2 = walkableNeighbor2.x;
			final int y2 = walkableNeighbor2.y;
			final Zone z2 = gdata.findZoneContaining(x2, y2);
			if (z2 == null) {
				assert false;
				throw new IllegalStateException("Cannot find zone containing " + x1 + "," + y1);
			}
			assert Dungeons.hasRoomOrCorridor(dungeon, z2);
			buf[0] = z1;
			buf[1] = z2;
		}
		return true;
	}

	private static boolean isDoorCandidate(/* @Nullable */ DungeonSymbol sym) {
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
			return true;
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

	private static boolean isDoorNeighborWalkableCandidate(/* @Nullable */ DungeonSymbol sym) {
		if (sym == null)
			return false;
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
		case WALL:
			return false;
		case FLOOR:
		case GRASS:
			return true;
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

	private static boolean isDoorNeighborImpassableCandidate(/* @Nullable */ DungeonSymbol sym) {
		if (sym == null)
			return false;
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
		case FLOOR:
		case GRASS:
			return false;
		case WALL:
			return true;
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

}
