package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hgames.lib.Exceptions;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * The type returned by {@link DungeonGenerator}. The API of this class is
 * intentionally only to retrieve the dungeon's structure. Mutation to the
 * dungeon are done via package-visible fields.
 * 
 * @author smelC
 */
public class Dungeon {

	final DungeonSymbol[][] map;

	protected final int width;
	protected final int height;

	final List<Zone> rooms;
	/**
	 * Map whose keys are {@link #rooms} and whose values wrap the keys. It can
	 * be used for example to quickly rule out zones when searching which zone
	 * contains a cell (see
	 * {@link DungeonBuilder#findZoneContaining(Dungeon, int, int)}). It can be
	 * incomplete both for {@link #rooms} and for {@link #corridors}, as rooms
	 * whose bounding box is the room itself aren't recorded in there (search
	 * tag (NO_BBOX) to see which implementations of {@link Zone} satisfy that).
	 * Furthermore it can be straight incomplete for {@link #corridors}, because
	 * corridors do not have a {@link Rectangle} bounding box (see tag
	 * (NO_CORRIDOR_BBOX) for that).
	 */
	final Map<Zone, Rectangle> boundingBoxes;
	/** Doors between adjacent rooms are in there (and are zones of size 1). */
	final List<Zone> corridors;

	/**
	 * The zones to which a zone is directly connected. Keys and values belong
	 * both to {@link #rooms} and {@link #corridors}.
	 */
	final Map<Zone, List<Zone>> connections;

	Dungeon(DungeonSymbol[][] map) {
		this.map = map;
		final int estimatedNumberOfRooms = size() / 256;
		this.rooms = new ArrayList<Zone>(estimatedNumberOfRooms);
		this.boundingBoxes = new HashMap<Zone, Rectangle>(estimatedNumberOfRooms);
		this.corridors = new ArrayList<Zone>(estimatedNumberOfRooms * 2);
		this.width = map.length;
		this.height = width == 0 ? 0 : map[0].length;
		this.connections = new HashMap<Zone, List<Zone>>();
	}

	/**
	 * @return The underlying map.
	 */
	public DungeonSymbol[][] getMap() {
		return map;
	}

	/**
	 * @param c
	 * @return The symbol at {@code c} or null if out of bounds
	 */
	public DungeonSymbol getSymbol(Coord c) {
		return isValid(c) ? map[c.x][c.y] : null;
	}

	/**
	 * @param x
	 * @param y
	 * @return The symbol at {@code (x,y)} or null if out of bounds
	 */
	public DungeonSymbol getSymbol(int x, int y) {
		return isValid(x, y) ? map[x][y] : null;
	}

	/**
	 * @param c
	 * @return Whether {@code c} is a valid coordinate in {@code this}.
	 */
	public boolean isValid(Coord c) {
		return isValid(c.x, c.y);
	}

	/**
	 * @param x
	 * @param y
	 * @return Whether {@code (x, y)} is a valid coordinate in {@code this}.
	 */
	public boolean isValid(int x, int y) {
		if (x < 0 || y < 0)
			return false;
		return x < width && y < height;
	}

	/**
	 * @param x
	 * @param y
	 * @return The zone containing {@code (x,y)} or {@code null} if none.
	 */
	public /* @Nullable */ Zone findZoneContaining(int x, int y) {
		return DungeonBuilder.findZoneContaining(this, x, y);
	}

	/** @return The number of cells in this dungeon */
	public int size() {
		return width * height;
	}

	/** @return This dungeon's width */
	public int getWidth() {
		return width;
	}

	/** @return This dungeon's height */
	public int getHeight() {
		return height;
	}

	/**
	 * @return true if {@code this}' invariants holds.
	 */
	public boolean invariant() {
		/* Every zone that is a key in the 'boundingboxes' map is registered */
		for (Zone key : boundingBoxes.keySet()) {
			if (!DungeonBuilder.hasZone(this, key)) {
				assert false;
				return false;
			}
		}
		/* Bounding boxes are correct */
		for (Map.Entry<Zone, ? extends Zone> key : boundingBoxes.entrySet()) {
			final Zone boundingBox = key.getValue();
			final Zone zone = key.getKey();
			if (!boundingBox.contains(zone)) {
				System.err.println(boundingBox + " isn't a bounding box of " + zone);
				assert false;
				return false;
			}
		}
		/* Connections are correct */
		for (Map.Entry<Zone, ? extends Collection<? extends Zone>> entry : connections.entrySet()) {
			final Zone z = entry.getKey();
			if (!DungeonBuilder.hasZone(this, z)) {
				assert false;
				return false;
			}
			final Collection<? extends Zone> connectedTo = entry.getValue();
			if (connectedTo.contains(z)) {
				assert false;
				return false;
			}
			for (Zone dest : connectedTo) {
				if (!DungeonBuilder.hasZone(this, dest)) {
					assert false;
					return false;
				}
			}
		}
		/* Every non-wall cell is within a zone */
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final DungeonSymbol sym = getSymbol(x, y);
				switch (sym) {
				case CHASM:
				case DEEP_WATER:
				case DOOR:
				case FLOOR:
				case GRASS:
				case HIGH_GRASS:
				case SHALLOW_WATER: {
					final Zone zone = findZoneContaining(x, y);
					if (zone == null) {
						System.err.println(sym + " cell (" + x + "," + y + ") doesn't belong to a zone.");
						return false;
					}
					continue;
				}
				case STAIR_DOWN:
				case STAIR_UP:
				case WALL:
					final Zone zone = findZoneContaining(x, y);
					if (zone != null) {
						System.err.println(sym + " cell (" + x + "," + y
								+ ") shouldn't belong to a zone (found " + zone + ").");
						return false;
					}
					continue;
				}
				throw Exceptions.newUnmatchedISE(sym);
			}
		}
		/* FIXME Check that stairs are correct */
		return true;
	}
}
