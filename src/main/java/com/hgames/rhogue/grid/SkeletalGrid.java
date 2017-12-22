package com.hgames.rhogue.grid;

import squidpony.squidmath.Coord;

/**
 * Skeletal implementation of {@link Grid}.
 * 
 * @author smelC
 */
public abstract class SkeletalGrid implements Grid {

	private static final long serialVersionUID = 8330840262053425809L;

	@Override
	public final boolean isInGrid(Coord c) {
		return isInGrid(c.x, c.y);
	}

	@Override
	public final boolean isInGrid(int x, int y) {
		return 0 <= x && x < getWidth() && 0 <= y && y < getHeight();
	}

	@Override
	public final boolean isOnEdge(Coord c) {
		return isOnEdge(c.x, c.y);
	}

	@Override
	public final boolean isOnEdge(int x, int y) {
		return x == 0 || x == getWidth() - 1 || y == 0 || y == getHeight() - 1;
	}

}
