package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.List;

import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A zone that is the union of two zones.
 * 
 * @author smelC
 */
public class ZoneUnion extends Zone.Skeleton {

	protected final Zone z1;
	protected final Zone z2;

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
		final List<Coord> all = new ArrayList<Coord>(size());
		all.addAll(z1.getAll());
		all.addAll(z2.getAll());
		return all;
	}

	@Override
	public Coord getRandom(RNG rng) {
		final Zone target = rng.nextBoolean() ? z1 : z2;
		Coord result = target.getRandom(rng);
		if (result == null)
			result = (target == z1 ? z2 : z1).getRandom(rng);
		return result;
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
		final Coord z1c = z1.getCenter();
		final Coord z2c = z2.getCenter();
		return Coord.get(Math.round(((z1c.x + z2c.x) * z1.size()) / 2f),
				Math.round(((z1c.y + z2c.y) * z2.size()) / 2f));
	}

	@Override
	public Zone union(Zone other) {
		if (z1.equals(other) || z2.equals(other))
			return this;
		else {
			final List<Zone> union = new ArrayList<Zone>(3);
			union.add(z1);
			union.add(z2);
			union.add(other);
			return new ZoneNAryUnion(union);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + z1.toString() + "|" + z2.toString() + "]";
	}
}
