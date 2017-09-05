package com.hgames.rhogue.grid;

import java.util.Collection;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public class Grids {

	/**
	 * @param coords
	 * @param c
	 * @return Whether {@code c} is on {@code coords}'s border.
	 */
	public static boolean isOnBorder(Collection<? extends Coord> coords, Coord c) {
		for (Direction dir : Direction.OUTWARDS) {
			final Coord neighbor = c.translate(dir);
			if (!coords.contains(neighbor))
				/*
				 * There's a neighbor of 'c' that isn't in 'coords'. Hence 'c'
				 * is on the border.
				 */
				return true;
		}
		return false;
	}

	/**
	 * @param coords
	 * @param c
	 * @return The number of neighbors of {@code c} that aren't in
	 *         {@code coords}. The more the more on the border {@code c} is.
	 */
	public static int borderness(Collection<? extends Coord> coords, Coord c) {
		int result = 0;
		for (Direction dir : Direction.OUTWARDS) {
			final Coord neighbor = c.translate(dir);
			if (!coords.contains(neighbor))
				result++;
		}
		return result;
	}

}
