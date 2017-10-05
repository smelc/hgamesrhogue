package com.hgames.rhogue.generation.map.connection;

import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.generation.map.DungeonGenerator;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;

import squidpony.squidgrid.zone.Zone;

/**
 * How to control connections of zones.
 * 
 * @author smelC
 */
public interface IConnectionControl {

	/**
	 * @param gen
	 *            The dungeon generator in use.
	 * @param dungeon
	 *            The dungeon being built.
	 * @param rg
	 *            The generator of {@code z}.
	 * @param z
	 * @return true to ignore the value set with
	 *         {@link DungeonGenerator#setDoorProbability(int)} and always punch a
	 *         door to {@code z1} and {@code z2}.
	 */
	public boolean forceDoor(DungeonGenerator gen, Dungeon dungeon, IRoomGenerator rg, Zone z);

	/**
	 * @param gen
	 *            The dungeon generator in use.
	 * @param dungeon
	 *            The dungeon being built.
	 * @param gen1
	 *            The generator of {@code z1}.
	 * @param z1
	 * @param gen2
	 *            The generator of {@code z2}.
	 * @param z2
	 * @return Whether a connection from {@code z1} to {@code z2} can be done.
	 */
	public boolean acceptsConnection(DungeonGenerator gen, Dungeon dungeon, IRoomGenerator gen1, Zone z1,
			IRoomGenerator gen2, Zone z2);

}
