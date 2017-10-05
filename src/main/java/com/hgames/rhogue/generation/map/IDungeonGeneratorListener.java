package com.hgames.rhogue.generation.map;

import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * Interface to listen to dungeon building and record game-specific
 * informations. To be augmented with new needs.
 * 
 * @author smelC
 */
public interface IDungeonGeneratorListener {

	/**
	 * Callback done when a door is punched between {@code z1} and {@code z2}.
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
	 *            {@code z1}'s generator
	 * @param z1
	 * @param door
	 * @param g2
	 *            {@code z2}'s generator
	 * @param z2
	 */
	public void punchDoor(Dungeon dungeon, IRoomGenerator g1, Zone z1, Coord door, IRoomGenerator g2, Zone z2);

}
