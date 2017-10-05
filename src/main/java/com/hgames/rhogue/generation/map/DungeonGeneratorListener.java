package com.hgames.rhogue.generation.map;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * An example implementation of {@link IDungeonGeneratorListener}.
 * 
 * @author smelC
 */
public class DungeonGeneratorListener implements IDungeonGeneratorListener {

	protected Set<Coord> lockedDoors;

	@Override
	public void punchDoor(Dungeon dungeon, IRoomGenerator g1, Zone z1, Coord door, IRoomGenerator g2, Zone z2) {
		if (g1 == null || g2 == null) {
			assert false;
			/* Cannot do */
			return;
		}
		if (1 == g1.getMaxConnections() || 1 == g2.getMaxConnections()) {
			System.out.println("Adding a locked door at " + door);
			addLockedDoor(door);
		}
	}

	/**
	 * @return The set of locked doors.
	 */
	public Set<Coord> getLockedDoors() {
		if (lockedDoors == null)
			return Collections.emptySet();
		else
			return lockedDoors;
	}

	protected void addLockedDoor(Coord door) {
		if (lockedDoors == null)
			lockedDoors = new LinkedHashSet<Coord>();
		lockedDoors.add(door);
	}

}
