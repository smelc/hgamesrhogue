package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * A {@link Zone} containing a single cell.
 * 
 * @author smelC
 */
public class SingleCellZone extends Zone.Skeleton implements Zone {

	protected final int x;
	protected final int y;

	private static final long serialVersionUID = 6267793374508699574L;

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
	public final boolean contains(int x, int y) {
		return this.x == x && this.y == y;
	}

	@Override
	public final boolean contains(Coord c) {
		return x == c.x && y == c.y;
	}

	private transient List<Coord> all = null;

	@Override
	public List<Coord> getAll() {
		if (all == null) {
			all = new ArrayList<Coord>(1);
			all.add(Coord.get(x, y));
			all = Collections.unmodifiableList(all);
		}
		return all;
	}

}
