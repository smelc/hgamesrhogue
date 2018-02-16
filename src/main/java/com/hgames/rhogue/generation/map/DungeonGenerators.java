package com.hgames.rhogue.generation.map;

import com.hgames.rhogue.generation.map.lifetime.Eternity;
import com.hgames.rhogue.generation.map.lifetime.OneShot;
import com.hgames.rhogue.generation.map.rgenerator.CaveRoomGenerator;
import com.hgames.rhogue.generation.map.rgenerator.CircularRoomGenerator;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.generation.map.rgenerator.RectangleRoomGenerator;
import com.hgames.rhogue.generation.map.rgenerator.ShallowRoomGenerator;

import squidpony.squidmath.IRNG;

/**
 * Factory for instances of {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public class DungeonGenerators {

	protected final IRNG rng;
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
	public DungeonGenerators(IRNG rng, int width, int height) {
		this.rng = rng;
		this.width = width;
		this.height = height;
	}

	/** @return A generator that builds dungeons with only rectangle rooms. */
	public DungeonGenerator basic() {
		final DungeonGenerator result = new DungeonGenerator(rng, width, height);
		result.installRoomGenerator(new RectangleRoomGenerator(), 1, Eternity.INSTANCE);
		return result;
	}

	/**
	 * @return A generator that builds dungeons with half rectangle rooms, half
	 *         caves
	 */
	public DungeonGenerator halfRectanglesHalfCaves() {
		final DungeonGenerator result = new DungeonGenerator(rng, width, height);
		result.installRoomGenerator(new RectangleRoomGenerator(), 3, Eternity.INSTANCE);
		result.installRoomGenerator(new CaveRoomGenerator(75), 1, Eternity.INSTANCE);
		result.installRoomGenerator(new CaveRoomGenerator(50), 1, Eternity.INSTANCE);
		result.installRoomGenerator(new CaveRoomGenerator(25), 1, Eternity.INSTANCE);
		return result;
	}

	/** @return A generator that builds dungeons with only caves */
	public DungeonGenerator cave() {
		final DungeonGenerator result = new DungeonGenerator(rng, width, height);
		result.installRoomGenerator(new CaveRoomGenerator(75), 1, Eternity.INSTANCE);
		result.installRoomGenerator(new CaveRoomGenerator(50), 1, Eternity.INSTANCE);
		result.installRoomGenerator(new CaveRoomGenerator(25), 1, Eternity.INSTANCE);
		return result;
	}

	/**
	 * @return A generator that builds a dungeons that always features a single
	 *         circular room, plus a mix of rectangular rooms and cave rooms; and an
	 *         island.
	 */
	public DungeonGenerator fancy() {
		final DungeonGenerator result = halfRectanglesHalfCaves();
		result.setWaterObjective(true, 20, 1, 10);
		{
			final IRoomGenerator treasureRoomGenerator = new RectangleRoomGenerator();
			/* To have a single door */
			treasureRoomGenerator.setMaxConnections(1);
			treasureRoomGenerator.setForceDoors(true);
			result.installRoomGenerator(treasureRoomGenerator, 200, new OneShot());
		}
		result.installRoomGenerator(new CircularRoomGenerator(), 100, new OneShot());
		final boolean circularCarved = rng.nextBoolean();
		/* Carve center only in circular rooms */
		final IRoomGenerator shallow = new ShallowRoomGenerator(
				circularCarved ? new CircularRoomGenerator() : new RectangleRoomGenerator(), circularCarved);
		result.installRoomGenerator(shallow, 100, new OneShot());
		return result;
	}

}
