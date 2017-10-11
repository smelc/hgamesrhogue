package com.hgames.rhogue.grid;

import squidpony.squidmath.Coord;

/**
 * Doer that iterates on the rectangle around a cell. It is reusable, to spare
 * allocations.
 * 
 * @author smelC
 */
public abstract class DoerInACenteredRectangle extends SkeletalCellsDoer {

	protected Coord center;
	protected int width;
	protected int height;

	/**
	 * A fresh instance. You should call {@link #init(Coord, int, int)} before using
	 * it.
	 */
	public DoerInACenteredRectangle() {
		this.center = null;
	}

	/**
	 * To call before doing a new iteration.
	 * 
	 * @param c
	 * @param w
	 * @param h
	 */
	public void init(Coord c, int w, int h) {
		this.center = c;
		this.width = w;
		this.height = h;
	}

	/**
	 * Calls {@link #doOnACell(int, int)} on cells in the rectangle set with
	 * {@link #init(Coord, int, int)}.
	 */
	@Override
	public void doOnCells() {
		if (center == null) {
			assert false : "Instance of " + getClass().getSimpleName() + " should be initialized before iterating";
			return;
		}
		final int cx = center.x;
		final int cy = center.y;
		for (int x = -width; x <= width; x++) {
			for (int y = -height; y <= height; y++) {
				final boolean stop = doOnACell(cx + x, cy + y);
				if (stop)
					return;
			}
		}
	}

}
