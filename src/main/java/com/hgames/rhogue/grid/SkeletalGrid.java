package com.hgames.rhogue.grid;

/**
 * Skeletal implementation of {@link Grid}.
 * 
 * @author smelC
 */
public abstract class SkeletalGrid implements Grid {

	private static final long serialVersionUID = 8330840262053425809L;

	@Override
	public final boolean isInGrid(int x, int y) {
		return 0 <= x && x < getWidth() && 0 <= y && y < getHeight();
	}

}
