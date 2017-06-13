package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.List;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * A zone containing 9 or 10 cells, centered around a {@link Coord}.
 * 
 * @author smelC
 * 
 * @see DirectionsZone A variant where directions can be specified.
 */
public class NineOrTenCellsZone extends Zone.Skeleton {

	protected final Coord center;
	protected final boolean includesCenter;

	private static final long serialVersionUID = 4952793900307422804L;

	/**
	 * @param center
	 *            The zone's center
	 * @param centerIncluded
	 *            Whether {@code center} belongs to the zone.
	 */
	public NineOrTenCellsZone(Coord center, boolean centerIncluded) {
		this.center = center;
		this.includesCenter = centerIncluded;
	}

	@Override
	public final boolean isEmpty() {
		return false;
	}

	@Override
	public int size() {
		return includesCenter ? 10 : 9;
	}

	@Override
	public boolean contains(int x, int y) {
		return contains(x, y, includesCenter);
	}

	/**
	 * @param x
	 * @param y
	 * @param considerCenter
	 *            As to whether the center can be used for matching
	 * @return Whether {@code this} contains {@code (x,y)} overidding the center
	 *         with {@code considerCenter}.
	 */
	public boolean contains(int x, int y, boolean considerCenter) {
		if (considerCenter && center.x == x && center.y == y)
			return true;
		for (Direction out : Direction.OUTWARDS) {
			if (x == (center.x + out.deltaX) && y == (center.y + out.deltaY))
				return true;
		}
		return false;
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
	public NineOrTenCellsZone removeCenter() {
		return includesCenter ? new NineOrTenCellsZone(center, false) : this;
	}

	private transient List<Coord> all;

	@Override
	public List<Coord> getAll() {
		if (all == null) {
			all = new ArrayList<Coord>(size());
			if (includesCenter)
				all.add(center);
			for (Direction out : Direction.OUTWARDS)
				all.add(center.translate(out));
		}
		return all;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + center;
	}
}
