package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.zone.ListZone;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public class Zones {

	/**
	 * @param coords
	 * @return A {@link ListZone} built from {@code coords}.
	 */
	public static ListZone newListZone(Coord[] coords) {
		final List<Coord> list = new ArrayList<Coord>(coords.length);
		Collections.addAll(list, coords);
		return new ListZone(list);
	}

	/**
	 * @param zone
	 * @param buffer
	 *            Where to store the result, or {@code null} for this method to
	 *            allocate a fresh set.
	 * @return Members of {@code zone} that are adjacent to a cell not in
	 *         {@code zone}.
	 */
	public static List<Coord> border(List<Coord> zone, /* @Nullable */ List<Coord> buffer) {
		return DungeonUtility.border(zone, buffer);
	}

	/**
	 * @param zone
	 * @param buffer
	 *            Where to store the result, or {@code null} for this method to
	 *            allocate a fresh set.
	 * @return Neighbors to {@code zone} that are not in {@code zone}.
	 */
	public static Set<Coord> externalBorder(final Zone zone, /* @Nullable */ Set<Coord> buffer) {
		final int zsz = zone.size();
		final Set<Coord> border = buffer == null ? new HashSet<Coord>(zsz / 4) : buffer;
		for (Coord c : zone) {
			for (Direction out : Direction.OUTWARDS) {
				final Coord neighbor = c.translate(out);
				if (!zone.contains(neighbor))
					border.add(neighbor);
			}
		}
		return border;
	}

	/**
	 * @param zone
	 * @param dir
	 * @param buffer
	 *            Where to store the result. Give null to let this method
	 *            allocate a fresh set.
	 * @return The corner of a zone.
	 */
	public static Set<Coord> getCorner(final Zone zone, Direction dir, /* @Nullable */ Set<Coord> buffer) {
		if (dir == Direction.NONE)
			return Collections.emptySet();
		final Set<Coord> result = buffer == null ? new HashSet<Coord>() : buffer;
		for (Coord c : zone) {
			if (result.isEmpty())
				result.add(c);
			else {
				for (Coord tmp : result) {
					if (inCorner(c, tmp, dir) < 0) {
						result.clear();
						result.add(c);
						break;
					}
				}
			}
		}
		assert zone.isEmpty() || !result.isEmpty();
		return result;
	}

	/**
	 * @param zone
	 * @param c
	 * @return {@code true} if {@code zone} has a cell adjacent to {@code c}.
	 */
	public static boolean isAdjacentTo(Zone zone, Coord c) {
		for (Coord inz : zone) {
			if (inz.isAdjacent(c))
				return true;
		}
		return false;
	}

	/**
	 * @param zones
	 * @param c
	 * @return true if a member of {@code zones} {@link Zone#contains(Coord)}
	 *         {@code c}.
	 */
	public static boolean anyContains(List<? extends Zone> zones, Coord c) {
		final int bound = zones.size();
		for (int i = 0; i < bound; i++) {
			if (zones.get(i).contains(c))
				return true;
		}
		return false;
	}

	/**
	 * @param zones
	 * @param z
	 * @return true if a member of {@code zones} {@link Zone#contains(Zone)}
	 *         {@code z}.
	 */
	public static boolean anyContains(List<? extends Zone> zones, Zone z) {
		final int bound = zones.size();
		for (int i = 0; i < bound; i++) {
			if (zones.get(i).contains(z))
				return true;
		}
		return false;
	}

	/**
	 * @param zones
	 * @param z
	 * @return true if a member of {@code zones}
	 *         {@link Zone#intersectsWith(Zone)} {@code z}.
	 */
	public static boolean anyIntersectsWith(List<? extends Zone> zones, Zone z) {
		final int bound = zones.size();
		for (int i = 0; i < bound; i++) {
			if (zones.get(i).intersectsWith(z))
				return true;
		}
		return false;
	}

	/**
	 * @param c1
	 * @param c2
	 * @return < 0 if {@code c1} is more in the corner {@code dir} than
	 *         {@code c2}, 0 if {@code c1} and {@code c2} are equally in the
	 *         corner and something > 0 if {@code c2} is more in the corner than
	 *         {@code c1}.
	 */
	private static int inCorner(Coord c1, Coord c2, Direction dir) {
		switch (dir) {
		case DOWN:
			// In SquidLib, a small y denotes a cell high on the screen.
			return Integer.compare(c2.y, c1.y);
		case DOWN_LEFT:
			return inCorner(c1, c2, Direction.DOWN) + inCorner(c1, c2, Direction.LEFT);
		case DOWN_RIGHT:
			return inCorner(c1, c2, Direction.DOWN) + inCorner(c1, c2, Direction.RIGHT);
		case LEFT:
			return Integer.compare(c1.x, c2.x);
		case NONE:
			return 0;
		case RIGHT:
			return Integer.compare(c2.x, c1.x);
		case UP:
			// In SquidLib, a small y denotes a cell high on the screen.
			return Integer.compare(c1.y, c2.y);
		case UP_LEFT:
			return inCorner(c1, c2, Direction.UP) + inCorner(c1, c2, Direction.LEFT);
		case UP_RIGHT:
			return inCorner(c1, c2, Direction.UP) + inCorner(c1, c2, Direction.RIGHT);
		}
		throw new IllegalStateException("Umatched direction: " + dir);
	}
}
