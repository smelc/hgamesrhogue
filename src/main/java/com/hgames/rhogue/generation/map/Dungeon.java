package com.hgames.rhogue.generation.map;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hgames.lib.Exceptions;
import com.hgames.rhogue.generation.map.draw.DungeonSymbolArrayDrawer;
import com.hgames.rhogue.zone.ListZone;
import com.hgames.rhogue.zone.Rectangle;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;

/**
 * The type returned by {@link DungeonGenerator}. The API of this class is kept
 * intentionally small. Mutation to dungeons are done via {@link #getBuilder()
 * DungeonBuilder}, and complex queries are done via {@link Dungeons}.
 * 
 * <p>
 * Instances of this class are supposed to be built by {@link DungeonGenerator}.
 * If you need to interface with dungeons build in another manner, you can still
 * use this API by using {@link DungeonAdapter} which will build an almost
 * structure-less instance of {@link Dungeon}.
 * </p>
 * 
 * @author smelC
 * 
 * @see DungeonGenerator
 * @see Dungeons
 * @see DungeonBuilder
 */
public class Dungeon implements Serializable {

	final DungeonSymbol[][] map;

	protected final int width;
	protected final int height;

	/** The mutation API */
	protected final DungeonBuilder builder;

	/** A cache from a coordinate, to its enclosing Zome; or null */
	/* FIXME FILL ME */
	protected transient Zone[][] coordToZone;

	final List<Zone> rooms;
	/**
	 * Map whose keys are {@link #rooms} and whose values wrap the keys. It can be
	 * used for example to quickly rule out zones when searching which zone contains
	 * a cell (see {@link Dungeons#findZoneContaining(Dungeon, int, int)}). It can
	 * be incomplete both for {@link #rooms} and for {@link #corridors}, as rooms
	 * whose bounding box is the room itself aren't recorded in there (search tag
	 * (NO_BBOX) to see which implementations of {@link Zone} satisfy that).
	 * Furthermore it can be straight incomplete for {@link #corridors}, because
	 * corridors do not have a {@link Rectangle} bounding box (see tag
	 * (NO_CORRIDOR_BBOX) for that).
	 */
	final Map<Zone, Rectangle> boundingBoxes;
	/** Doors between adjacent rooms are in there (and are zones of size 1). */
	final List<Zone> corridors;

	/** Members of {@link #rooms} that aren't connected to the stairs. */
	/* @Nullable */ List<Zone> disconnectedRooms;

	/** Members of {@link #rooms} that are surrounded by deep water. */
	/* @Nullable */ List<Zone> waterIslands;

	/**
	 * The zones to which a zone is directly connected. Keys and values belong both
	 * to {@link #rooms} and {@link #corridors}.
	 */
	final Map<Zone, List<Zone>> connections;

	Coord upwardStair;
	Coord downwardStair;

	/** Chasms */
	/* @Nullable */ List<Zone> chasms;

	/** Grass */
	/* @Nullable */ List<Zone> grassPools;
	/** High-grass, grass through which you cannot see */
	/* @Nullable */ List<Zone> highGrassPools;

	/** Deep water */
	/* @Nullable */ List<ListZone> waterPools;

	private static final long serialVersionUID = 993644642092518044L;

	/**
	 * A fresh instance backed up by {@code map}.
	 * 
	 * @param map
	 */
	Dungeon(DungeonSymbol[][] map) {
		this.map = map;
		this.builder = new DungeonBuilder(this);
		final int estimatedNumberOfRooms = size() / 256;
		this.rooms = new ArrayList<Zone>(estimatedNumberOfRooms);
		this.boundingBoxes = new HashMap<Zone, Rectangle>(estimatedNumberOfRooms);
		this.corridors = new ArrayList<Zone>(estimatedNumberOfRooms * 2);
		this.width = map.length;
		this.height = width == 0 ? 0 : map[0].length;
		this.connections = new HashMap<Zone, List<Zone>>();
	}

	/**
	 * @return The mutation API.
	 */
	public DungeonBuilder getBuilder() {
		return builder;
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
	 * @param upOrDown
	 * @return The stair up or the stair down.
	 */
	public /* @Nullable */ Coord getStair(boolean upOrDown) {
		return upOrDown ? upwardStair : downwardStair;
	}

	/**
	 * @return The rooms. This is a reference to an internal structure, you should
	 *         likely not modify it.
	 */
	public List<Zone> getRooms() {
		return rooms;
	}

	/**
	 * @return The corridors. This is a reference to an internal structure, you
	 *         should likely not modify it.
	 */
	public List<Zone> getCorridors() {
		return corridors;
	}

	/**
	 * @return The member of {@link #getRooms()} that aren't connected to the
	 *         stairs. Useful for hidden rooms that require carving to be reached
	 *         (brogue's underworms) This is a reference to an internal structure,
	 *         you should likely not modify it.
	 */
	public List<Zone> getDisconnectedRooms() {
		return disconnectedRooms == null ? Collections.<Zone>emptyList() : disconnectedRooms;
	}

	/** @return Pools of deep water. */
	public List<? extends Zone> getDeepWaterPools() {
		if (waterPools == null)
			return Collections.emptyList();
		else
			return waterPools;
	}

	/** @return Pools of grass. */
	public List<? extends Zone> getGrassPools() {
		if (grassPools == null)
			return Collections.emptyList();
		else
			return grassPools;
	}

	/** @return Pools of high grass. */
	public List<? extends Zone> getHighGrassPools() {
		if (highGrassPools == null)
			return Collections.emptyList();
		else
			return highGrassPools;
	}

	/**
	 * @return The member of {@link #getRooms()} that are water-islands (surround by
	 *         deep water) This is a reference to an internal structure, you should
	 *         likely not modify it.
	 */
	public List<Zone> getWaterIslands() {
		return waterIslands == null ? Collections.<Zone>emptyList() : waterIslands;
	}

	/**
	 * @param z
	 * @return The neighbors of {@code z}. It's either a room or a corridor.
	 */
	public List<Zone> getNeighbors(Zone z) {
		assert Dungeons.hasZone(this, z);
		final List<Zone> result = connections.get(z);
		return result == null ? Collections.<Zone>emptyList() : result;
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
	 * @param c
	 * @return The zone containing {@code z} or null if none.
	 */
	// FIXME CH put GenerationData's cache in there ?
	public /* @Nullable */ Zone findZoneContaining(Coord c) {
		return Dungeons.findZoneContaining(this, c.x, c.y);
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
			if (!Dungeons.hasZone(this, key)) {
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
			if (!Dungeons.hasRoomOrCorridor(this, z)) {
				assert false;
				return false;
			}
			final Collection<? extends Zone> connectedTo = entry.getValue();
			if (connectedTo.contains(z)) {
				assert false;
				return false;
			}
			for (Zone dest : connectedTo) {
				if (!Dungeons.hasRoomOrCorridor(this, dest)) {
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
				case GRASS:
				case HIGH_GRASS:
					/* To debug */
					continue;
				case DOOR:
				case FLOOR:
				case SHALLOW_WATER: {
					final Zone zone = Dungeons.findZoneContaining(this, x, y);
					if (zone == null) {
						System.err.println(sym + " cell (" + x + "," + y + ") doesn't belong to a zone.");
						return false;
					}
					continue;
				}
				case CHASM:
				case DEEP_WATER:
				case STAIR_DOWN:
				case STAIR_UP:
				case WALL:
					final Zone zone = Dungeons.findZoneContaining(this, x, y);
					if (zone != null) {
						System.err.println(
								sym + " cell (" + x + "," + y + ") shouldn't belong to a zone (found " + zone + ").");
						return false;
					}
					continue;
				}
				throw Exceptions.newUnmatchedISE(sym);
			}
		}
		/* All zones are non-empty */
		{
			for (Zone z : rooms) {
				if (z.isEmpty()) {
					assert false : "Room " + z + " is empty";
					return false;
				}
			}
			for (Zone z : corridors) {
				if (z.isEmpty()) {
					assert false : "Corridor " + z + " is empty";
					return false;
				}
			}
			if (waterPools != null) {
				for (Zone z : waterPools) {
					if (z.isEmpty()) {
						assert false : "Deep water pool " + z + " is empty";
						return false;
					}
				}
			}
		}

		/* FIXME Check that stairs are correct */
		/* FIXME Check that disconnectedRooms are correct */
		/* FIXME Check that waterIslands are correct */

		return true;
	}

	/**
	 * Debug method.
	 * 
	 * @return {@code this} pretty printed.
	 */
	public String dirtyPrint() {
		return new DungeonSymbolArrayDrawer().draw(map);
	}
}
