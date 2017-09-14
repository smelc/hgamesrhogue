package com.hgames.rhogue.generation.map;

import com.hgames.lib.collection.multiset.EnumMultiset;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * Utility methods concerning {@link Dungeon}, to avoid avoid a huge query API
 * in {@link Dungeon}.
 * 
 * @author smelC
 */
public class Dungeons {

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @return The room or corridor of {@code dungeon} that
	 *         {@link Zone#contains(int, int)} {@code (x, y)}, or {@code null}
	 *         if none.
	 */
	public static Zone findZoneContaining(Dungeon dungeon, int x, int y) {
		return DungeonBuilder.findZoneContaining(dungeon, x, y);
	}

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @param considerDiagonals
	 *            Whether to consider cardinal neighbors.
	 * @return The neighbors of {@code (x, y)}. The size can be smaller than
	 *         expected since null symbols and out of bound neighbors are
	 *         skipped.
	 */
	public static EnumMultiset<DungeonSymbol> getNeighbors(Dungeon dungeon, int x, int y,
			boolean considerDiagonals) {
		final EnumMultiset<DungeonSymbol> result = EnumMultiset.noneOf(DungeonSymbol.class);
		final Direction[] dirs = considerDiagonals ? Direction.OUTWARDS : Direction.CARDINALS;
		for (Direction dir : dirs) {
			final int nx = x + dir.deltaX;
			final int ny = y + dir.deltaY;
			final DungeonSymbol sym = dungeon.getSymbol(nx, ny);
			if (sym != null)
				result.add(sym);
			/* else out of bound */
		}
		return result;
	}

	/**
	 * @param dungeon
	 * @param z
	 * @return Whether {@code z} is a room or corridor in {@code z}.
	 */
	public static boolean hasRoomOrCorridor(Dungeon dungeon, Zone z) {
		return DungeonBuilder.hasRoomOrCorridor(dungeon, z);
	}

	/**
	 * @param dungeon
	 * @param c
	 * @return Whether {@code c} is on {@code dungeon}'s edge.
	 */
	public static boolean isOnEdge(Dungeon dungeon, Coord c) {
		return c.x == 0 || c.x == dungeon.getWidth() - 1 || c.y == 0 || c.y == dungeon.getHeight() - 1;
	}

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @return Whether {@code (x, y)} is on the dungeon's edge.
	 */
	public static boolean isOnEdge(Dungeon dungeon, int x, int y) {
		return x == 0 || x == dungeon.getWidth() - 1 || y == 0 || y == dungeon.getHeight() - 1;
	}
}
