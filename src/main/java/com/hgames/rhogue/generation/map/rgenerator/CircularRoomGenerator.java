package com.hgames.rhogue.generation.map.rgenerator;

import com.hgames.rhogue.generation.map.SkeletalRoomGenerator;
import com.hgames.rhogue.zone.CircularZone;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A room generator that generate rooms that are circular.
 * 
 * @author smelC
 */
public class CircularRoomGenerator extends SkeletalRoomGenerator {

	protected final RNG rng;

	/**
	 * @param rng
	 *            The RNG to use.
	 */
	public CircularRoomGenerator(RNG rng) {
		this.rng = rng;
	}

	@Override
	public Zone generate(int maxWidth, int maxHeight) {
		final int radius = radius(maxWidth, maxHeight);
		if (radius <= 1)
			return null;
		final int cx = maxWidth / 2;
		final int cy = -(maxHeight / 2);
		return new CircularZone(Coord.get(cx, cy), radius);
	}

	protected int radius(int maxWidth, int maxHeight) {
		final int constraint = Math.min(maxWidth, maxHeight);
		final int max = constraint / 2; // Exclusive
		return rng.between(1, max);
	}
}
