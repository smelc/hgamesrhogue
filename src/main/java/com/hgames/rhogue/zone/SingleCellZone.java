package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * A {@link Zone} containing a single cell.
 * 
 * @author smelC
 */
public class SingleCellZone extends Zone.Skeleton implements Zone {

	protected final int x;
	protected final int y;

	private transient List<Coord> all = null;

	private static final long serialVersionUID = 6267793374508699574L;

	/**
	 * @param c
	 */
	public SingleCellZone(Coord c) {
		this(c.x, c.y);
	}

	/**
	 * @param x
	 * @param y
	 */
	public SingleCellZone(int x, int y) {
		this.x = x;
		this.y = y;
	}

	@Override
	public final boolean isEmpty() {
		return false;
	}

	@Override
	public final int size() {
		return 1;
	}

	@Override
	public final boolean contains(int ox, int oy) {
		return this.x == ox && this.y == oy;
	}

	@Override
	public final boolean contains(Coord c) {
		return x == c.x && y == c.y;
	}

	@Override
	public List<Coord> getAll() {
		if (all == null) {
			all = new ArrayList<Coord>(1);
			all.add(Coord.get(x, y));
			all = Collections.unmodifiableList(all);
		}
		return all;
	}

	@Override
	public Collection<Coord> getExternalBorder() {
		final List<Coord> result = new ArrayList<Coord>(9);
		final Coord center = getCenter();
		for (Direction out : Direction.OUTWARDS)
			result.add(center.translate(out));
		return result;
	}

	@Override
	public Zone translate(int xShift, int yShift) {
		return new SingleCellZone(this.x + xShift, this.y + yShift);
	}

	@Override
	public Zone extend() {
		return new NineOrTenCellsZone(Coord.get(x, y), true);
	}

	@Override
	public Zone shrink() {
		return EmptyZone.INSTANCE;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + getCenter() + "]";
	}
}
