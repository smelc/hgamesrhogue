package com.hgames.rhogue.generation.map.rgenerator;

import com.hgames.rhogue.generation.map.RoomComponent;
import com.hgames.rhogue.zone.CircularZone;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A room generator that generate rooms that are circular.
 * 
 * @author smelC
 */
public class CircularRoomGenerator extends SkeletalRoomGenerator {

	/**
	 * A fresh instance.
	 */
	public CircularRoomGenerator() {
	}

	@Override
	public Zone generate(RNG rng, RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		final int radius = radius(rng, maxWidth, maxHeight);
		if (radius <= 1)
			return null;
		final int cx = maxWidth / 2;
		final int cy = -(maxHeight / 2);
		return new CircularZone(Coord.get(cx, cy), radius);
	}

	protected int radius(RNG rng, int maxWidth, int maxHeight) {
		final int constraint = Math.min(maxWidth, maxHeight);
		final int max = constraint / 2; // Exclusive
		final int min = getMinSize();
		if (min <= 0)
			/* Cannot do */
			return -1;
		return max <= 2 ? -1 : rng.between(min, max);
	}

	private int getMinSize() {
		int minw = getMinSideSize(true);
		if (minw == 0)
			/* Cannot do */
			return -1;
		else if (minw < 0)
			/* No constraint specified on width */
			minw = 1;
		int minh = getMinSideSize(false);
		if (minh == 0)
			/* Cannot do */
			return -1;
		else if (minw < 0)
			/* No constraint specified on height */
			minh = 1;
		return Math.min(minw, minh);
	}
}
