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
		return max <= 2 ? -1 : rng.between(1, max);
	}
}
