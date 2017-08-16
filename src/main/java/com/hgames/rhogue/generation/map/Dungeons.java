package com.hgames.rhogue.generation.map;

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

}
