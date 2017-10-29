package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.List;

import squidpony.squidmath.Coord;

/**
 * A zone that is the union of two zones.
 * 
 * @author smelC
 */
public class ZoneUnion extends Zone.Skeleton {

	protected final Zone z1;
	protected final Zone z2;

	protected transient List<Coord> all;
	protected transient Coord center;

	private static final long serialVersionUID = 1414500085497183858L;

	/**
	 * The union of two zones.
	 * 
	 * @param z1
	 * @param z2
	 */
	public ZoneUnion(Zone z1, Zone z2) {
		this.z1 = z1;
		this.z2 = z2;
	}

	@Override
	public boolean isEmpty() {
		return z1.isEmpty() && z2.isEmpty();
	}

	@Override
	public int size() {
		return z1.size() + z2.size();
	}

	@Override
	public boolean contains(int x, int y) {
		return z1.contains(x, y) || z2.contains(x, y);
	}

	@Override
	public boolean contains(Coord c) {
		return z1.contains(c) || z2.contains(c);
	}

	@Override
	public List<Coord> getAll() {
		if (all == null) {
			all = new ArrayList<Coord>(size());
			all.addAll(z1.getAll());
			all.addAll(z2.getAll());
		}
		return all;
	}

	@Override
	public boolean intersectsWith(Zone other) {
		return z1.intersectsWith(other) || z2.intersectsWith(other);
	}

	@Override
	public Zone translate(int x, int y) {
		return new ZoneUnion(z1.translate(x, y), z2.translate(x, y));
	}

	@Override
	public Zone extend() {
		return new ZoneUnion(z1.extend(), z2.extend());
	}

	@Override
	public Coord getCenter() {
		if (center == null) {
			final Coord z1c = z1.getCenter();
			final Coord z2c = z2.getCenter();
			center = Coord.get(Math.round(((z1c.x + z2c.x) * z1.size()) / 2f),
					Math.round(((z1c.y + z2c.y) * z2.size()) / 2f));
		}
		return center;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + z1.toString() + "|" + z2.toString() + "]";
	}
}
