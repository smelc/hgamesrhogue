package com.hgames.rhogue.zone;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public class Zones {

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

}
