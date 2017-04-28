package com.hgames.rhogue.generation.corridor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidmath.Coord;

/**
 * A class that computes the straight corridors of a map.
 * 
 * @author smelC
 * @see Corridors
 */
public class CorridorComputer {

	protected final char[][] map;
	protected final int width;
	protected final int height;

	protected final Set<Character> walls;

	/**
	 * A fresh computer, that treats '#' as walls and everything else as
	 * passable.
	 * 
	 * @param map
	 *            The map to use.
	 */
	public CorridorComputer(char[][] map) {
		this.map = map;
		this.width = map.length;
		this.height = width == 0 ? 0 : map[0].length;
		this.walls = new HashSet<Character>();
		walls.add('#');
	}

	/**
	 * @param c
	 *            A character to treat as a wall.
	 */
	public void addWall(char c) {
		walls.add(c);
	}

	/**
	 * @param c
	 *            A character not to treat as a wall.
	 */
	public void removeWall(char c) {
		walls.remove(c);
	}

	/**
	 * @return The corridors in {@code c}
	 */
	public Corridors compute() {
		/*
		 * Implementation could be more efficient by iterating only once on the
		 * map. But it would be tartelette aux concombres.
		 */

		final List<Rectangle> horizontalCorridors = computeHorizontalCorridors(map);
		final List<Rectangle> verticalCorridors = computeVerticalCorridors(map);

		return new Corridors(horizontalCorridors, verticalCorridors);
	}

	protected boolean isCorridorCell(int x, int y, boolean horizontalOrVertical) {
		if (x < 0 || y < 0) {
			/* out of bounds */
			assert false;
			return false;
		}
		if (width <= x || height <= y) {
			/* out of bounds */
			assert false;
			return false;
		}
		if (!isCorridor(map[x][y]))
			return false;
		return hasValidNeighbors(x, y, horizontalOrVertical);
	}

	/* Subclassers may override */
	protected boolean isCorridor(char c) {
		return !walls.contains(c);
	}

	/* Subclassers may override */
	protected boolean isCorridorNeighbor(char c) {
		return walls.contains(c);
	}

	private List<Rectangle> computeHorizontalCorridors(char[][] map) {
		/* Lazily allocated */
		final List<Rectangle> result = new ArrayList<Rectangle>();

		for (int y = 0; y < height; y++) {
			/*
			 * The starting x of the current corridor, or -1 if no current
			 * corridor.
			 */
			int start = -1;
			for (int x = 0; x < width; x++) {
				assert start == -1 || (0 <= start && start < width);
				final boolean corridor = isCorridorCell(x, y, true);
				if (corridor) {
					if (start < 0)
						/* Start a corridor */
						start = x;
					/* else continue a corridor */
				} else {
					if (0 <= start) {
						/* End the current corridor */
						result.add(new Rectangle.Impl(Coord.get(start, y), x - start, 1));
						start = -1;
					}
				}
			}
			/* Corridor until the map's edge */
			if (0 <= start)
				result.add(new Rectangle.Impl(Coord.get(start, y), width - start, 1));
		}

		return result.isEmpty() ? Collections.<Rectangle> emptyList() : result;
	}

	private List<Rectangle> computeVerticalCorridors(char[][] map) {
		/* Lazily allocated */
		final List<Rectangle> result = new ArrayList<Rectangle>();

		for (int x = 0; x < width; x++) {
			/*
			 * The starting y of the current corridor, or -1 if no current
			 * corridor.
			 */
			int start = -1;
			for (int y = 0; y < height; y++) {
				assert start == -1 || (0 <= start && start < height);
				final boolean corridor = isCorridorCell(x, y, false);
				if (corridor) {
					if (start < 0)
						/* Start a corridor */
						start = y;
					/* else continue a corridor */
				} else {
					if (0 <= start) {
						/* End the corridor */
						result.add(new Rectangle.Impl(Coord.get(x, y - 1), 1, y - start));
						start = -1;
					}
				}
			}
			/* Corridor until the map's edge */
			if (0 <= start)
				result.add(new Rectangle.Impl(Coord.get(x, height - 1), 1, height - start));
		}

		return result.isEmpty() ? Collections.<Rectangle> emptyList() : result;
	}

	private boolean hasValidNeighbors(int x, int y, boolean horizontalOrVertical) {
		/* If a horizontal corridor: UP and DOWN cells must be walls */
		/* If a vertical corridor: LEFT and RIGHT cells must be walls */
		if (!isCorridorNeighborCell(x, y, horizontalOrVertical ? Direction.UP : Direction.LEFT))
			return false;
		if (!isCorridorNeighborCell(x, y, horizontalOrVertical ? Direction.DOWN : Direction.RIGHT))
			return false;
		return true;
	}

	private boolean isCorridorNeighborCell(int x_, int y_, Direction dir) {
		final int x = x_ + dir.deltaX;
		if (x < 0 || width <= x)
			/* Off the map, should not be a blocking cell */
			return true;
		final int y = y_ + dir.deltaY;
		if (y < 0 || height <= y)
			return true;
		return isCorridorNeighbor(map[x][y]);
	}
}
