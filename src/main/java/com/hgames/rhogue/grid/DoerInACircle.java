package com.hgames.rhogue.grid;

/**
 * A class to execute some actions within all cells of a circle.
 * 
 * @author smelC
 */
public abstract class DoerInACircle extends SkeletalCellsDoer {

	@Override
	public void doOnCells() {
		throw new IllegalStateException();
	}

	/**
	 * @param x
	 *            The x-coordinate of the circle's center
	 * @param y
	 *            The y-coordinate of the circle's center
	 * @param radius
	 *            The circle's radius (rayon).
	 */
	@Deprecated // Harmonize with DoerInACenteredRectangle
	public void doInACircle(int x, int y, int radius) {
		if (radius < 0)
			return;
		for (int dx = -radius; dx <= radius; ++dx) {
			final int high = (int) Math.floor(Math.sqrt((radius * radius) - (dx * dx)));
			for (int dy = -high; dy <= high; ++dy) {
				final boolean stop = doOnACell(x + dx, y + dy);
				if (stop)
					return;
			}
		}
	}

}
