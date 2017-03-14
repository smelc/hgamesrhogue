package com.hgames.rhogue.generation;

import java.util.Iterator;
import java.util.NoSuchElementException;

import squidpony.squidmath.Coord;

/**
 * An iterator that iterates over the cells of a rectangular map, starting at a
 * given cell.
 * 
 * @author smelC
 */
public class RandomStartMapIterator implements Iterator<Coord> {

	protected final int xstart;
	protected final int ystart;

	protected final int width;
	protected final int height;

	/** The previous element returned */
	protected /* @Nullable */ Coord prev;

	/**
	 * @param xstart
	 *            The x-coordinate where to start iterating. Must be smaller
	 *            than width.
	 * @param ystart
	 *            The y-coordinate where to start iterating. Must be smaller
	 *            than height.
	 * @param width
	 *            The map's width
	 * @param height
	 *            The map's height
	 */
	public RandomStartMapIterator(int xstart, int ystart, int width, int height) {
		if (width <= xstart)
			throw new IllegalStateException("Invalid x-coordinate: " + xstart + " (width is " + width + ")");
		if (height <= ystart)
			throw new IllegalStateException(
					"Invalid y-coordinate: " + ystart + " (height is " + height + ")");
		this.xstart = xstart;
		this.ystart = ystart;
		this.width = width;
		this.height = height;
	}

	@Override
	public boolean hasNext() {
		return next0(true) != null;
	}

	@Override
	public Coord next() {
		if (!hasNext())
			throw new NoSuchElementException();
		return next0(false);
	}

	protected /* @Nullable */ Coord next0(boolean dry) {
		if (prev == null) {
			if (isEmpty())
				return null;
			final Coord result = Coord.get(xstart, ystart);
			if (!dry)
				prev = result;
			return result;
		} else {
			/* Continuing */
			final int nextx = prev.x == width - 1 ? 0 : prev.x + 1;
			final int nexty = prev.y == height - 1 ? 0 : prev.y + 1;
			if (nextx == xstart && nexty == ystart)
				/* Done */
				return null;
			else {
				final Coord result = Coord.get(nextx, nexty);
				if (!dry)
					prev = result;
				return result;
			}
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private boolean isEmpty() {
		return width == 0 || height == 0;
	}
}
