package com.hgames.rhogue.generation.map.connection;

import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.zone.Zone;

/**
 * Whether two zones are connected in a dungeon. Not in {@link Dungeon} itself
 * since this is for dungeon generation only. When a dungeon has been built all
 * zones are connected (except the ones listed in
 * {@code Dungeon.disconnectedRooms}).
 * 
 * @author smelC
 */
public interface IConnectionFinder {

	/**
	 * Don't go crazy on this method for big values of {@code intermediates}.
	 * 
	 * @param z0
	 * @param z1
	 * @param intermediates
	 *            A bound on the allowed intermediates. 1 is the minimum (a corridor
	 *            connecting the two rooms).
	 * @return Whether {@code z0} and {@code z1} are connected by at most
	 *         {@code intermediates} zones.
	 */
	public boolean areConnected(Zone z0, Zone z1, int intermediates);

}
