package com.hgames.rhogue.generation.map;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A room generator that generates simple rectangle rooms.
 * 
 * @author smelC
 */
public class RectangleRoomGenerator extends SkeletalRoomGenerator {

	protected final RNG rng;

	/**
	 * @param rng
	 *            The rng to use.
	 */
	public RectangleRoomGenerator(RNG rng) {
		this.rng = rng;
	}

	@Override
	public Rectangle generate(Dungeon dungeon, Coord translation, int maxWidth, int maxHeight) {
		assert 0 < maxWidth;
		assert 0 < maxHeight;
		/* Avoid generating corridors (width or height == 1) if possible */
		final int w = rng.between(maxWidth == 1 ? 1 : 2, maxWidth + 1);
		final int h = rng.between(maxHeight == 1 ? 1 : 2, maxHeight + 1);
		return new Rectangle.Impl(Coord.get(0, 0), w, h);
	}
}
