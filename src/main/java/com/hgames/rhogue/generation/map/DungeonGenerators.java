package com.hgames.rhogue.generation.map;

import squidpony.squidmath.RNG;

/**
 * Factory for instances of {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public class DungeonGenerators {

	protected final RNG rng;
	protected final int width;
	protected final int height;

	/**
	 * A fresh instance.
	 * 
	 * @param rng
	 *            The random number generator to use.
	 * @param width
	 *            The width of generated dungeons.
	 * @param height
	 *            The height of generated dungeons.
	 */
	public DungeonGenerators(RNG rng, int width, int height) {
		this.rng = rng;
		this.width = width;
		this.height = height;
	}

	/**
	 * @return A generator that builds dungeons like the original rogue.
	 */
	public DungeonGenerator rogueLikeGenerator() {
		final DungeonGenerator result = new DungeonGenerator(rng, width, height);
		result.installRoomGenerator(new RectangleRoomGenerator(rng), 1);
		return result;
	}

}
