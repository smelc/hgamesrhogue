package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hgames.lib.collection.pair.Pair;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public class Zones {

	/**
	 * @param zones
	 * @return true if all members of zones are disjoint.
	 */
	public static boolean allDisjoint(List<? extends Zone> zones) {
		final int nbz = zones.size();
		if (nbz <= 1)
			return true;
		for (int i = 0; i < nbz; i++) {
			final Zone base = zones.get(i);
			for (Zone other : zones) {
				if (other == base)
					continue;
				if (base.intersectsWith(other))
					return false;
			}
		}
		return true;
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
	 * @return true if a member of {@code zones} {@link Zone#intersectsWith(Zone)}
	 *         {@code z}.
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
	 * A smart constructor.
	 * 
	 * @param coords
	 * @return A zone containing {@code coords}.
	 */
	public static Zone build(List<Coord> coords) {
		final int sz = coords.size();
		switch (sz) {
		case 0:
			return EmptyZone.INSTANCE;
		case 1:
			return new SingleCellZone(coords.get(0));
		default:
			return new ListZone(coords);
		}
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
	 * @param zones
	 * @return The approximate center of {@code zones}.
	 */
	public static Coord center(Iterable<Zone> zones) {
		int x = -1;
		int y = -1;
		float totalSz = 0;
		for (Zone zone : zones) {
			final Coord center = zone.getCenter();
			final int sz = zone.size();
			totalSz += sz;
			x += center.x * sz;
			y += center.y * sz;
		}
		return x == -1 ? null : Coord.get(Math.round(x / totalSz), Math.round(y / totalSz));
	}

	/**
	 * @param z1
	 * @param z2
	 * @return An approximation of the distance to connect {@code z1} to {@code z2}.
	 */
	public static double connectingDistance(Zone z1, Zone z2) {
		final Coord c1 = z1.getCenter();
		final Coord c2 = z2.getCenter();
		double result = c1.distance(c2);
		result -= (diagonal(z1) / 2);
		result -= (diagonal(z2) / 2);
		return Math.max(0, result);
	}

	/**
	 * @param zone
	 *            A possibly null zone
	 * @param c
	 * @return Whether zone is non-null and contains {@code c}.
	 */
	public static boolean contains(/* @Nullable */ Zone zone, Coord c) {
		return zone != null && zone.contains(c);
	}

	/**
	 * @param zone
	 * @param coords
	 * @return Whether {@code zone} contains a member of {@code coors}.
	 */
	public static boolean containsAny(Zone zone, Collection<Coord> coords) {
		for (Coord c : coords) {
			if (zone.contains(c))
				return true;
		}
		return false;
	}

	/**
	 * @param z
	 * @return An approximation of the length of the diagonal of a zone.
	 */
	@Deprecated // Use Zone#diagonal directly
	public static double diagonal(Zone z) {
		final int w = z.getWidth();
		final int h = z.getHeight();
		return Math.sqrt((w * w) + (h * h));
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
	 * @param zones
	 * @param center
	 * @return The first member of {@code zones} whose {@link Zone#getCenter()
	 *         center} is {@code center}, or null if none.
	 */
	public static /* @Nullable */ Zone findZoneWithCenter(/* @Nullable */ List<? extends Zone> zones, Coord center) {
		if (zones == null)
			return null;
		final int nbz = zones.size();
		for (int i = 0; i < nbz; i++) {
			final Zone zone = zones.get(i);
			if (zone != null && zone.getCenter().equals(center))
				return zone;
		}
		return null;
	}

	/**
	 * @param z1
	 * @param z2
	 * @return A pair of coords in z1 and z2, whose distance is minimum; or null if
	 *         none.
	 */
	public static /* @Nullable */ Pair<Coord, Coord> getAClosestPair(Zone z1, Zone z2) {
		int[] result = null;
		double minDistance = Double.MAX_VALUE;
		for (Coord c1 : z1) {
			for (Coord c2 : z2) {
				if (result == null) {
					result = new int[4];
					result[0] = c1.x;
					result[1] = c1.y;
					result[2] = c2.x;
					result[3] = c2.y;
				} else {
					final double local = c1.distance(c2);
					if (local < minDistance) {
						minDistance = local;
						result[0] = c1.x;
						result[1] = c1.y;
						result[2] = c2.x;
						result[3] = c2.y;
					}
				}
			}
		}
		return result == null ? null : Pair.of(Coord.get(result[0], result[1]), Coord.get(result[2], result[3]));
	}

	/**
	 * @param zone
	 * @param dir
	 * @param buffer
	 *            Where to store the result. Give null to let this method allocate a
	 *            fresh set.
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
		final Coord center = zone.getCenter();
		if ((zone.getDiagonal() * 1.5) <= center.distance(c))
			/* Quick dispatch to avoid iterating over the Zone */
			return false;

		for (Coord inz : zone) {
			if (inz.isAdjacent(c))
				return true;
		}
		return false;
	}

	/**
	 * @param z
	 * @param coords
	 * @return true if {@code z} and {@code coords} have a common cell.
	 */
	public static boolean intersects(Zone z, Collection<Coord> coords) {
		if (z.size() < coords.size()) {
			for (Coord c : z) {
				if (coords.contains(c))
					return true;
			}
			return false;
		} else {
			for (Coord c : coords) {
				if (z.contains(c))
					return true;
			}
			return false;
		}
	}

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
	 * @param zones
	 * @return The sum of {@code zones}'s size.
	 */
	public static int size(List<? extends Zone> zones) {
		int result = 0;
		final int sz = zones.size();
		for (int i = 0; i < sz; i++)
			result += zones.get(i).size();
		return result;
	}

	/**
	 * @param z1
	 * @param z2
	 * @return The smart union of {@code z1} and {@code z2}.
	 */
	public static Zone union(/* @Nullable */ Zone z1, /* @Nullable */ Zone z2) {
		if (z1 == null || z1.isEmpty())
			return z2;
		else if (z2 == null || z2.isEmpty())
			return z1;
		else
			return z1.union(z2);
	}

	/**
	 * @param c1
	 * @param c2
	 * @return < 0 if {@code c1} is more in the corner {@code dir} than {@code c2},
	 *         0 if {@code c1} and {@code c2} are equally in the corner and
	 *         something > 0 if {@code c2} is more in the corner than {@code c1}.
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
