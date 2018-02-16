package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * Implementation of floodfills. I originally wrote if for {@code SquidLib} and
 * later copied it to this library, removing contributions from SquidLib's
 * commiters (TEttinger); and specializing it for symbols described by enums.
 * 
 * @author smelC
 * @param <T>
 *            The type of symbols.
 */
public class Splash<T extends Enum<T>> {

	protected final EnumSet<T> impassable;

	/**
	 * @param impassable
	 *            The symbols not be spill on.
	 */
	public Splash(EnumSet<T> impassable) {
		this.impassable = impassable;
	}

	/**
	 * @param rng
	 *            used to randomize the floodfill
	 * @param level
	 *            char 2D array with x, y indices for the dungeon/map level
	 * @param start
	 *            Where the spill should start. It should be passable, otherwise
	 *            an empty list gets returned. Consider using
	 *            {@link DungeonUtility#getRandomCell(IRNG, char[][], Set, int)}
	 *            to find it.
	 * @param volume
	 *            The number of cells to spill on.
	 * @param drunks
	 *            The ratio of drunks to use to make the splash more realistic.
	 *            Like for dungeon generation, if greater than 0, drunk walkers
	 *            will remove the splash's margins, to make it more realistic.
	 *            You don't need that if you're doing a splash that is bounded
	 *            by walls, because the fill will be realistic. If you're doing
	 *            a splash that isn't bounded, use that for its borders not to
	 *            be too square.
	 * 
	 *            <p>
	 *            Useful values are 0, 1, and 2. Giving more will likely yield
	 *            an empty result, on any decent map sizes.
	 *            </p>
	 * @return The spill. It is a list of coordinates (containing {@code start})
	 *         valid in {@code level} that are all adjacent and whose symbol is
	 *         passable. If non-empty, this is guaranteed to be an
	 *         {@link ArrayList}.
	 */
	public List<Coord> spill(IRNG rng, T[][] level, Coord start, int volume, int drunks) {
		final int width = level.length;
		final int height = width == 0 ? 0 : level[0].length;
		if (!insideLevel(width, height, start.x, start.y) || !passable(level[start.x][start.y]))
			return Collections.emptyList();

		final List<Coord> result = new ArrayList<Coord>(volume);

		Direction[] dirs = new Direction[Direction.OUTWARDS.length];

		final LinkedList<Coord> toTry = new LinkedList<Coord>();
		toTry.add(start);
		final Set<Coord> trieds = new HashSet<Coord>();

		while (!toTry.isEmpty()) {
			assert result.size() < volume;
			final Coord current = toTry.removeFirst();
			assert DungeonUtility.inLevel(level, current);
			assert passable(level[current.x][current.y]);
			if (trieds.contains(current))
				continue;
			trieds.add(current);
			/*
			 * Here it holds that either 'current == start' or there's a Coord
			 * in 'result' that is adjacent to 'current'.
			 */
			result.add(current);
			if (result.size() == volume)
				/* We're done */
				break;
			/* Now prepare data for next iterations */
			/* Randomize directions */
			dirs = rng.shuffle(Direction.OUTWARDS, dirs);
			for (Direction d : dirs) {
				final Coord next = current.translate(d);
				if (insideLevel(width, height, next.x, next.y) && !trieds.contains(next)
						&& passable(level[next.x][next.y]))
					/* A valid cell for trying to be spilled on */
					toTry.add(next);
			}
		}

		if (0 < drunks)
			drunkinize(rng, level, result, DungeonUtility.border(result, null), drunks);

		return result;
	}

	/**
	 * @param rng
	 * @param map
	 *            The map on which {@code zone} is a pool
	 * @param zone
	 *            The zone to shrink
	 * @param border
	 *            {@code zone}'s border
	 * @param drunks
	 *            The number of drunken walkers to consider
	 */
	protected void drunkinize(IRNG rng, T[][] map, List<Coord> zone, List<Coord> border, int drunks) {
		if (drunks == 0)
			return;

		final int sz = zone.size();
		final int nb = (sz / 10) * drunks;
		if (nb == 0)
			return;

		assert !border.isEmpty();
		for (int j = 0; j < nb && !zone.isEmpty(); j++) {
			drunkinize0(rng, zone, border, drunks);
			if (border.isEmpty() || zone.isEmpty())
				return;
		}
	}

	protected boolean passable(T sym) {
		return !impassable.contains(sym);
	}

	/**
	 * Removes a circle from {@code zone}, by taking the circle's center in
	 * {@code zone} 's border: {@code border}.
	 * 
	 * @param border
	 *            {@code result}'s border.
	 */
	private void drunkinize0(IRNG rng, List<Coord> zone, List<Coord> border, int nb) {
		assert !border.isEmpty();
		assert !zone.isEmpty();

		final int width = rng.nextInt(nb) + 1;
		final int height = rng.nextInt(nb) + 1;
		final int radius = Math.max(1, Math.round(nb * Math.min(width, height)));
		final Coord center = rng.getRandomElement(border);
		zone.remove(center);
		for (int dx = -radius; dx <= radius; ++dx) {
			final int high = (int) Math.floor(Math.sqrt(radius * radius - dx * dx));
			for (int dy = -high; dy <= high; ++dy) {
				final Coord c = center.translate(dx, dy);
				zone.remove(c);
				if (zone.isEmpty())
					return;
			}
		}
	}

	private static boolean insideLevel(int width, int height, int x, int y) {
		if (x <= 0)
			return false;
		if (width - 1 <= x)
			return false;
		if (y <= 0)
			return false;
		if (height - 1 <= y)
			return false;
		return true;
	}

}
