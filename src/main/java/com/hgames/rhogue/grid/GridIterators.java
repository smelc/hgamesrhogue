package com.hgames.rhogue.grid;

import java.util.Iterator;
import java.util.NoSuchElementException;

import com.hgames.lib.Exceptions;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * Iterators over maps.
 * 
 * @author smelC
 */
public class GridIterators {

	/**
	 * An iterator that iterates within a rectangle, starting at at any
	 * direction and iterating in any direction. This iterator stops when it
	 * iterated over all cells.
	 * 
	 * @author smelC
	 */
	public static class RectangleRandomStartAndDirection implements Iterator<Coord> {

		protected final int mapWidth;
		protected final int mapHeight;
		protected final int xstart;
		protected final int ystart;
		protected final Direction dir;

		protected /* @Nullable */ Coord prev;

		/**
		 * @param mapWidth
		 *            The rectangle's width.
		 * @param mapHeight
		 *            The rectangle's height.
		 * @param xstart
		 *            The rectangle x-starting point. Must be >= 0 and <
		 *            mapWidth.
		 * @param ystart
		 *            The rectangle y-starting point. Must be >= 0 and <
		 *            mapHeight.
		 * @param dir
		 *            The direction in which to iterate. It MUST be cardinal.
		 */
		public RectangleRandomStartAndDirection(int mapWidth, int mapHeight, int xstart, int ystart,
				Direction dir) {
			if (mapWidth < 0)
				throw new IllegalStateException(
						"The map's width must be greater than zero. Received " + mapWidth);
			this.mapWidth = mapWidth;
			if (mapHeight < 0)
				throw new IllegalStateException(
						"The map's height must be greater than zero. Received " + mapHeight);
			this.mapHeight = mapHeight;
			if (xstart < 0 || mapWidth <= xstart)
				throw new IllegalStateException(
						"Starting x position must be in [0, " + mapWidth + "). Received: " + xstart);
			this.xstart = xstart;
			if (ystart < 0 || mapHeight <= ystart)
				throw new IllegalStateException(
						"Starting y position must be in [0, " + mapHeight + "). Received: " + ystart);
			this.ystart = ystart;
			if (!dir.isCardinal())
				throw new IllegalStateException("Expected a cardinal direction. Received: " + dir);
			this.dir = dir;
		}

		@Override
		public boolean hasNext() {
			final Coord next = next0();
			if (prev == null)
				return next != null;
			else
				return next.x != xstart || next.y != ystart;
		}

		@Override
		public Coord next() {
			if (hasNext()) {
				final Coord result = next0();
				assert 0 <= result.x && result.x <= mapWidth;
				assert 0 <= result.y && result.y <= mapHeight;
				prev = result;
				return result;
			} else
				throw new NoSuchElementException();
		}

		private Coord next0() {
			if (prev == null && (0 == mapWidth || 0 == mapHeight))
				return null;
			if (prev == null)
				return Coord.get(xstart, ystart);
			/* Recall that in SquidLib convention, (0,0) is top left */
			switch (dir) {
			case UP:
				/* Iterate from bottom to top and left to right */
				if (prev.y == 0) {
					/* Need to go right */
					if (prev.x == mapWidth - 1)
						/* Cycle */
						return Coord.get(0, mapHeight - 1);
					return Coord.get(prev.x + 1, mapHeight - 1);
				} else
					return prev.translate(dir);
			case DOWN:
				/* Iterate from left to right and top to bottom */
				if (prev.y == mapHeight - 1) {
					/* Need to go right */
					if (prev.x == mapWidth - 1)
						/* Cycle */
						return Coord.get(0, 0);
					return Coord.get(prev.x + 1, 0);
				} else
					return prev.translate(dir);
			case LEFT:
				/* Iterate from left to right and bottom to top */
				if (prev.x == 0) {
					/* Need to go one line up */
					if (prev.y == 0)
						/* Cycle */
						return Coord.get(mapWidth - 1, mapHeight - 1);
					return Coord.get(mapWidth - 1, prev.y - 1);
				} else
					return prev.translate(dir);
			case RIGHT:
				/* Iterate from right to left and bottom to top */
				if (prev.x == mapWidth - 1) {
					/* Need to go one line up */
					if (prev.y == 0)
						/* Cycle */
						return Coord.get(0, mapHeight - 1);
					return Coord.get(0, prev.y - 1);
				} else
					return prev.translate(dir);
			case NONE:
			case DOWN_LEFT:
			case DOWN_RIGHT:
			case UP_LEFT:
			case UP_RIGHT:
				throw new IllegalStateException("Expected a cardinal direction. Received: " + dir);
			}
			throw Exceptions.newUnmatchedISE(dir);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

}
