package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * A zone containing a cell, and cells reachable from a {@link Direction} around
 * this cell.
 * 
 * @author smelC
 * 
 * @see NineOrTenCellsZone A variant where the set of {@link Direction} is
 *      {@link Direction#values()} (with or without {@link Direction#NONE}).
 */
public class DirectionsZone extends Zone.Skeleton implements Zone {

	protected final Coord center;
	/** The directions from {@link #center} */
	protected final EnumSet<Direction> dirs;
	/** Whether {@link #center} is included */
	protected final boolean includesCenter;

	private static final long serialVersionUID = -533130141065912443L;

	/**
	 * @param center
	 * @param dirs
	 *            The direction from {@link #center}.
	 * @param includeCenter
	 *            Whether {@link #center} is included.
	 */
	// FIXME CH Remove 'includeCenter' and use Directions.NONE for that
	public DirectionsZone(Coord center, EnumSet<Direction> dirs, boolean includeCenter) {
		this.dirs = dirs;
		this.center = center;
		this.includesCenter = includeCenter;
	}

	@Override
	public boolean isEmpty() {
		return !includesCenter && dirs.isEmpty();
	}

	@Override
	public int size() {
		return (includesCenter ? 1 : 0) + dirs.size();
	}

	@Override
	public boolean contains(int x, int y) {
		if (includesCenter && center.x == x && center.y == y)
			return true;
		for (Direction dir : dirs) {
			if ((center.x + dir.deltaX == x) && (center.y + dir.deltaY == y))
				return true;
		}
		return false;
	}

	private transient List<Coord> all;

	@Override
	public List<Coord> getAll() {
		if (all == null) {
			all = new ArrayList<Coord>(size());
			if (includesCenter)
				all.add(center);
			for (Direction dir : dirs)
				all.add(center.translate(dir));
		}
		return all;
	}

	@Override
	public Zone translate(int x, int y) {
		return new DirectionsZone(center.translate(x, y), dirs.clone(), includesCenter);
	}

	/**
	 * @return Whether this zone includes the center cell.
	 */
	public boolean includesCenter() {
		return includesCenter;
	}

	/**
	 * @return A variant of {@code this} whose center is excluded.
	 */
	public DirectionsZone removeCenter() {
		return includesCenter ? new DirectionsZone(center, dirs, false) : this;
	}

	/**
	 * @return The zone's center. May not be in the zone itself if
	 *         {@link #includesCenter()} doesn't hold.
	 */
	@Override
	public Coord getCenter() {
		return center;
	}

}
