package com.hgames.rhogue.generation.map.dungeon;

import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;

/**
 * Interface to listen to dungeon building and record game-specific
 * informations. To be augmented with new needs.
 * 
 * @author smelC
 */
public interface IDungeonGeneratorListener {

	/**
	 * Callback done when {@code room} has been placed.
	 * 
	 * @param dungeon
	 * @param rg
	 *            {@code room}'s generator.
	 * @param room
	 */
	public void placedRoom(Dungeon dungeon, IRoomGenerator rg, Zone room);

	/**
	 * Callback done when {@code room} is removed, when it finally was deemed
	 * inadequate.
	 * 
	 * @param dungeon
	 * @param rg
	 *            {@code room}'s generator.
	 * @param room
	 */
	public void removedRoom(Dungeon dungeon, IRoomGenerator rg, Zone room);

	/**
	 * Callback done when a door is punched between {@code z1} and {@code z2} (one
	 * of them being a room, or both of them).
	 * 
	 * <p>
	 * This method is typically used to keep track of locked doors. You could define
	 * them as being the doors in between zones whose
	 * whose {@link IRoomGenerator#getMaxConnections() maximum number of
	 * connections} is 1.
	 * </p>
	 * 
	 * @param dungeon
	 *            The dungeon being built.
	 * @param g1
	 *            {@code z1}'s generator if {@code z1} is a room or null if it's a
	 *            corridor.
	 * @param z1
	 *            A room or corridor.
	 * @param door
	 * @param g2
	 *            {@code z2}'s generator if {@code z2} is a room or null if it's a
	 *            corridor.
	 * @param z2
	 *            A room or corridor.
	 */
	public void punchedDoor(Dungeon dungeon, /* @Nullable */ IRoomGenerator g1, Zone z1, Coord door,
			/* @Nullable */ IRoomGenerator g2, Zone z2);

}
