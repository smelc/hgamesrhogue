package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

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

	@Override
	public List<Coord> getAll() {
		final List<Coord> all = new ArrayList<Coord>(size());
		if (includesCenter)
			all.add(center);
		for (Direction out : Direction.OUTWARDS)
			all.add(center.translate(out));
		return all;
	}

	@Override
	public Coord getRandom(IRNG rng) {
		return center.translate(rng.getRandomElement(includesCenter ? Direction.values() : Direction.OUTWARDS));
	}

	@Override
	public List<Coord> getInternalBorder() {
		return includesCenter ? new NineOrTenCellsZone(center, false).getAll() : getAll();
	}

	@Override
	public Zone translate(int x, int y) {
		return new NineOrTenCellsZone(center.translate(x, y), includesCenter);
	}

	@Override
	public Zone shrink() {
		return includesCenter ? new SingleCellZone(center) : EmptyZone.INSTANCE;
	}

	@Override
	public Zone remove(Coord c) {
		if (includesCenter && c.equals(center))
			return new NineOrTenCellsZone(center, false);
		if (c.isAdjacent(center)) {
			assert contains(c);
			final EnumSet<Direction> copy = EnumSet.allOf(Direction.class);
			copy.remove(Direction.NONE);
			final boolean rmed = copy.remove(center.toGoTo(c));
			assert rmed;
			return copy.isEmpty() ? EmptyZone.INSTANCE : new DirectionsZone(center, copy, includesCenter);
		} else
			return super.remove(c);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + center;
	}
}
