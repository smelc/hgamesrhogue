package com.hgames.rhogue.generation.map;

import com.hgames.rhogue.generation.map.lifetime.Eternity;
import com.hgames.rhogue.generation.map.lifetime.OneShot;
import com.hgames.rhogue.generation.map.rgenerator.CircularRoomGenerator;

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

	/** @return A generator that builds dungeons with only rectangle rooms. */
	// FIXME CH Rename me
	public DungeonGenerator rogueLikeGenerator() {
		final DungeonGenerator result = new DungeonGenerator(rng, width, height);
		result.installRoomGenerator(new RectangleRoomGenerator(rng), 1, Eternity.INSTANCE);
		return result;
	}

	/**
	 * @param startWithWater
	 *            Whether to start with water. It makes water more central.
	 * @return A generator that builds a dungeons that always features a single
	 *         circular room, plus classical rectangular rooms.
	 */
	public DungeonGenerator guaranteesOneCircularRoom(boolean startWithWater) {
		final DungeonGenerator result = new DungeonGenerator(rng, width, height);
		if (startWithWater)
			result.setWaterObjective(startWithWater, 20, 1, 0);
		result.installRoomGenerator(new CircularRoomGenerator(rng), 100, new OneShot());
		result.installRoomGenerator(new RectangleRoomGenerator(rng), 1, Eternity.INSTANCE);
		return result;
	}

}
