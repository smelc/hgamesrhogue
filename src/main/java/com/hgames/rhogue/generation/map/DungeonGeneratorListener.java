package com.hgames.rhogue.generation.map;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;

/**
 * An example implementation of {@link IDungeonGeneratorListener} that uses
 * {@link IRoomGenerator#getMaxConnections()} to determine locked doors.
 * 
 * @author smelC
 */
public class DungeonGeneratorListener implements IDungeonGeneratorListener {

	protected Set<Coord> lockedDoors;

	@Override
	public void placedRoom(Dungeon dungeon, IRoomGenerator rg, Zone room) {
		// An example of debugging:
		// if (rg.getMaxConnections() == 1) {
		// dungeon.getBuilder().setSymbol(room.getCenter(), DungeonSymbol.HIGH_GRASS);
		// }
	}

	@Override
	public void removedRoom(Dungeon dungeon, IRoomGenerator rg, Zone room) {
		/* Nothing done */
	}

	@Override
	public void punchedDoor(Dungeon dungeon, IRoomGenerator g1, Zone z1, Coord door, IRoomGenerator g2, Zone z2) {
		assert Dungeons.hasRoomOrCorridor(dungeon, z1) : z1 + " is not a room or corridor";
		assert Dungeons.hasRoomOrCorridor(dungeon, z2) : z2 + " is not a room or corridor";
		assert (g1 == null) == dungeon.getCorridors().contains(z1);
		assert (g2 == null) == dungeon.getCorridors().contains(z2);
		if (g1 == null && g2 == null) {
			assert false;
			/* Cannot do */
			return;
		}
		if ((g1 != null && 1 == g1.getMaxConnections()) || (g2 != null && 1 == g2.getMaxConnections())) {
			// System.out.println("Adding a locked door at " + door + " (between " + z1 + "
			// [generator: " + g1 + "] and "
			// + z2 + " [generator: " + g2 + "])");
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
