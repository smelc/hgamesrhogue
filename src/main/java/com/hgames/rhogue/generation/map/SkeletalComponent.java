package com.hgames.rhogue.generation.map;

import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;

import squidpony.squidgrid.zone.Zone;

/**
 * @author smelC
 */
public abstract class SkeletalComponent implements GeneratorComponent {

	/**
	 * @param gen
	 * @param dungeon
	 * @param room
	 * @return Whether {@code room} can receive an additional connection.
	 */
	protected boolean acceptsOneMoreConnection(DungeonGenerator gen, Dungeon dungeon, Zone room) {
		assert dungeon.getRooms().contains(room);
		final IRoomGenerator rg = gen.getRoomGenerator(dungeon, room);
		final int max = rg.getMaxConnections();
		if (max == Integer.MAX_VALUE)
			return true;
		else
			return Dungeons.getNumberOfConnections(dungeon, room) < max;
	}

	/**
	 * @param gen
	 * @param dungeon
	 * @param room
	 * @return Whether doors to {@code room} should be forced.
	 */
	protected boolean forceDoors(DungeonGenerator gen, Dungeon dungeon, Zone room) {
		assert dungeon.getRooms().contains(room);
		final IRoomGenerator rg = gen.getRoomGenerator(dungeon, room);
		return rg.getForceDoors();
	}

}
