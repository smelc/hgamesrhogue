package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.List;

import com.hgames.rhogue.grid.DoerInACircle;

import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A zone in the shape of a circle. Could be optimized by overriding some method
 * from {@link com.hgames.rhogue.zone.Zone.Skeleton}. Examples:
 * <a href="http://imgur.com/gallery/13sH3">imgur</a>
 * 
 * @author smelC
 */
public class CircularZone extends Zone.Skeleton implements Zone {

	protected final Coord center;
	protected final int radius;

	private static final long serialVersionUID = 3973121098983241491L;

	/**
	 * @param center
	 *            The circle's center.
	 * @param radius
	 *            The radius of the circle. Starts at {@code 0} (yielding an empty
	 *            zone), then 1 (yielding a size 5 zone), etc (see class
	 *            documentation for looks of this zone).
	 */
	public CircularZone(Coord center, int radius) {
		this.center = center;
		this.radius = radius;
	}

	@Override
	public boolean isEmpty() {
		return radius == 0;
	}

	@Override
	public final Coord getCenter() {
		return center;
	}

	@Override
	public List<Coord> getAll() {
		final List<Coord> result = new ArrayList<Coord>(radius * 4);
		DoerInACircle.computeAll(center.x, center.y, radius, result);
		return result;
	}

	@Override
	public Coord getRandom(RNG rng) {
		final Coord result = DoerInACircle.getRandom(rng, center.x, center.y, radius);
		assert contains(result);
		return result;
	}

	@Override
	public CircularZone translate(Coord c) {
		return new CircularZone(center.add(c), radius);
	}

	@Override
	public CircularZone translate(int x, int y) {
		return new CircularZone(Coord.get(center.x + x, center.y + y), radius);
	}

	@Override
	public CircularZone extend() {
		return new CircularZone(center, radius + 1);
	}

	@Override
	public String toString() {
		return "Circle at " + center + " with radius: " + radius;
	}
}
