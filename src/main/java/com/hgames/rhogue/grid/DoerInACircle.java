package com.hgames.rhogue.grid;

import java.util.ArrayList;
import java.util.List;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * A class to execute some actions within all cells of a circle. It is reusable,
 * to spare allocations.
 * 
 * @author smelC
 */
public abstract class DoerInACircle extends SkeletalCellsDoer {

	/** The x-coordinate of the circle's center */
	protected int centerx;
	/** The y-coordinate of the circle's center */
	protected int centery;
	/** The circle's radius (rayon). */
	protected int radius;

	/**
	 * A fresh instance, you should call {@link #init(int, int, int)} afterwards.
	 */
	public DoerInACircle() {

	}

	/**
	 * Prepares {@code this} for a call to {@link #doOnCells}.
	 * 
	 * @param x
	 *            The x-coordindate of the circle's center
	 * @param y
	 *            The y-coordindate of the circle's center
	 * @param radius_
	 */
	public void init(int x, int y, int radius_) {
		this.centerx = x;
		this.centery = y;
		this.radius = radius_;
	}

	@Override
	public void doOnCells() {
		if (radius < 0)
			return;
		for (int dx = -radius; dx <= radius; ++dx) {
			final int high = (int) Math.floor(Math.sqrt((radius * radius) - (dx * dx)));
			for (int dy = -high; dy <= high; ++dy) {
				final boolean stop = doOnACell(centerx + dx, centery + dy);
				if (stop)
					return;
			}
		}
	}

	/**
	 * @param x
	 * @param y
	 * @param radius
	 * @param buf
	 *            The list to fill if non null, otherwise a new list is allocated
	 *            and returned.
	 * @return The list of cells of the circle centered at (x,y) and of radius
	 *         {@code radius}.
	 */
	public static List<Coord> computeAll(int x, int y, int radius, List<Coord> buf) {
		final List<Coord> result = buf == null ? new ArrayList<Coord>(radius * 4) : buf;
		for (int dx = -radius; dx <= radius; ++dx) {
			final int high = (int) Math.floor(Math.sqrt((radius * radius) - (dx * dx)));
			for (int dy = -high; dy <= high; ++dy) {
				result.add(Coord.get(x + dx, y + dy));
			}
		}
		return result;
	}

	/**
	 * @param rng
	 * @param x
	 * @param y
	 * @param radius
	 * @return A random cell within the circle centered at (x, y) and of radius
	 *         {@code radius}.
	 */
	public static Coord getRandom(IRNG rng, int x, int y, int radius) {
		final int dx = rng.between(-radius, radius);
		final int high = (int) Math.floor(Math.sqrt((radius * radius) - (dx * dx)));
		final int dy = rng.between(-high, high);
		return Coord.get(x + dx, y + dy);
	}

}
