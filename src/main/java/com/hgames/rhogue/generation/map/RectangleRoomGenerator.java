package com.hgames.rhogue.generation.map;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A room generator that generates simple rectangle rooms.
 * 
 * @author smelC
 */
public class RectangleRoomGenerator implements IRoomGenerator {

	protected final RNG rng;

	/**
	 * @param rng
	 *            The rng to use.
	 */
	public RectangleRoomGenerator(RNG rng) {
		this.rng = rng;
	}

	@Override
	public Zone generate(int maxWidth, int maxHeight) {
		final int w = rng.between(1, maxWidth + 1);
		final int h = rng.between(1, maxHeight + 1);
		return new Rectangle.Impl(Coord.get(0, 0), w, h);
	}

}
