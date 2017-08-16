package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	 * {@link DungeonBuilder#findZoneContaining(Dungeon, int, int)}). It is
	 * complete for {@link #rooms} but may be incomplete for {@link #corridors}.
	 */
	final Map<Zone, Rectangle> boundingBoxes;
	final List<Zone> corridors;

	/**
	 * The zones to which a zone is connected. Keys and values belong both to
	 * {@link #rooms} and {@link #corridors}.
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
	// FIXME CH Call me
	public boolean invariant() {
		for (Zone key : boundingBoxes.keySet()) {
			if (!DungeonBuilder.hasZone(this, key)) {
				assert false;
				return false;
			}
		}
		for (Map.Entry<Zone, ? extends Zone> key : boundingBoxes.entrySet()) {
			final Zone boundingBox = key.getValue();
			final Zone zone = key.getKey();
			if (!boundingBox.contains(zone)) {
				assert false;
				return false;
			}
		}
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
		return true;
	}
}
