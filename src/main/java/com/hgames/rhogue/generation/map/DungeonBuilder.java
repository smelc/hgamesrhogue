package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hgames.lib.collection.Multimaps;
import com.hgames.rhogue.zone.Zones;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.ListZone;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * Methods to mutate a {@link Dungeon}. Do not change visibilities, they are
 * intended.
 * 
 * @author smelC
 */
class DungeonBuilder {

	/**
	 * Adds a connection between {@code z1} and {@code z2} in {@code dungeon},
	 * taking care of reflexivity.
	 * 
	 * @param dungeon
	 * @param z1
	 * @param z2
	 */
	static void addConnection(Dungeon dungeon, Zone z1, Zone z2) {
		assert z1 != z2;
		if (z1 == z2)
			throw new IllegalStateException("A zone should not be connected to itself");
		assert hasRoomOrCorridor(dungeon, z1);
		assert hasRoomOrCorridor(dungeon, z2);
		Multimaps.addToListMultimapIfAbsent(dungeon.connections, z1, z2);
		Multimaps.addToListMultimapIfAbsent(dungeon.connections, z2, z1);
	}

	static void addDisconnectedRoom(Dungeon dungeon, Zone z) {
		assert dungeon.getRooms().contains(z);
		if (dungeon.disconnectedRooms == null)
			dungeon.disconnectedRooms = new ArrayList<Zone>();
		dungeon.disconnectedRooms.add(z);
	}

	static void addWaterIsland(Dungeon dungeon, Zone z) {
		assert dungeon.getRooms().contains(z);
		if (dungeon.waterIslands == null)
			dungeon.waterIslands = new ArrayList<Zone>();
		dungeon.waterIslands.add(z);
	}

	/** Prefer this method over direct mutations, it eases debugging. */
	static void addWaterPool(Dungeon dungeon, ListZone pool) {
		assert !hasZone(dungeon, pool);
		if (dungeon.waterPools == null)
			dungeon.waterPools = new ArrayList<ListZone>();
		else
			assert !dungeon.waterPools.contains(pool);
		dungeon.waterPools.add(pool);
	}

	/** Prefer this method over direct mutations, it eases debugging. */
	static void addZone(Dungeon dungeon, Zone z, /* @Nullable */ Rectangle boundingBox,
			boolean roomOrCorridor) {
		/* Zone should not intersect with existing zones */
		assert findIntersectingZones(dungeon, z, true, true, true) == null : ("Cannot add zone " + z
				+ ". It overlaps with an existing zone: "
				+ findIntersectingZones(dungeon, z, true, true, true));
		/* Check that bounding box (if any) is correct */
		assert boundingBox == null || boundingBox.contains(z);
		// System.out.println("Adding zone: " + z);
		if (roomOrCorridor)
			dungeon.rooms.add(z);
		else
			dungeon.corridors.add(z);
		if (boundingBox != null) {
			final Rectangle prev = dungeon.boundingBoxes.put(z, boundingBox);
			if (prev != null)
				throw new IllegalStateException(z + " was recorded already");
		}
	}

	static void removeZone(Dungeon dungeon, Zone z) {
		boolean done = dungeon.rooms.remove(z);
		if (!done)
			done = dungeon.corridors.remove(z);
		if (!done)
			throw new IllegalStateException("Zone not found: " + z);
		dungeon.boundingBoxes.remove(z);
		dungeon.connections.remove(z);
		for (List<Zone> destinations : dungeon.connections.values())
			destinations.remove(z);
	}

	/** Prefer this method over direct mutations, it eases debugging. */
	static void setStair(Dungeon dungeon, int x, int y, boolean upOrDown) {
		final DungeonSymbol sym = upOrDown ? DungeonSymbol.STAIR_UP : DungeonSymbol.STAIR_DOWN;
		setSymbol(dungeon, x, y, sym);
		final Coord c = Coord.get(x, y);
		if (upOrDown)
			dungeon.upwardStair = c;
		else
			dungeon.downwardStair = c;
	}

	/** Prefer this method over direct mutations, it eases debugging. */
	static void setSymbol(Dungeon dungeon, Coord c, DungeonSymbol sym) {
		setSymbol(dungeon, c.x, c.y, sym);
	}

	/** Prefer this method over direct mutations, it eases debugging. */
	static void setSymbol(Dungeon dungeon, int x, int y, DungeonSymbol sym) {
		dungeon.map[x][y] = sym;
	}

	/**
	 * Sets {@code sym} everywhere in {@code it}.
	 * 
	 * @param sym
	 */
	static void setSymbols(Dungeon dungeon, Iterator<Coord> it, DungeonSymbol sym) {
		while (it.hasNext()) {
			final Coord c = it.next();
			dungeon.map[c.x][c.y] = sym;
		}
	}

	/**
	 * Sets {@code sym} for cells in {@code it} whose symbol is not in
	 * {@cod except}
	 * 
	 * @param sym
	 */
	static void setSymbolsExcept(Dungeon dungeon, Iterator<Coord> it, DungeonSymbol sym,
			EnumSet<DungeonSymbol> except) {
		while (it.hasNext()) {
			final Coord c = it.next();
			if (!except.contains(dungeon.getSymbol(c)))
				dungeon.map[c.x][c.y] = sym;
		}
	}

	/**
	 * Sets {@code sym} everywhere in {@code dungeon}.
	 * 
	 * @param sym
	 */
	static void setAllSymbols(Dungeon dungeon, DungeonSymbol sym) {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				dungeon.map[x][y] = sym;
			}
		}
	}

	static boolean anyOnEdge(Dungeon dungeon, Iterator<Coord> it) {
		while (it.hasNext()) {
			final Coord c = it.next();
			if (Dungeons.isOnEdge(dungeon, c))
				return true;
		}
		return false;
	}

	/**
	 * Don't go crazy on this method, it is slow on zones that are connected via
	 * many intermediates rooms/corridors.
	 * 
	 * @param dungeon
	 * @param z0
	 * @param z1
	 * @return Whether {@code z0} and {@code z1} are connected.
	 */
	static boolean areConnected(Dungeon dungeon, Zone z0, Zone z1) {
		return areConnected(dungeon, z0, z1, Integer.MAX_VALUE);
	}

	/**
	 * @param dungeon
	 * @param z0
	 * @param z1
	 * @param intermediates
	 *            A bound on the allowed intermediates. 1 is the minimum (a
	 *            corridor connecting the two rooms).
	 * @return Whether {@code z0} and {@code z1} are connected by at most
	 *         {@code intermediates} zones.
	 */
	static boolean areConnected(Dungeon dungeon, Zone z0, Zone z1, int intermediates) {
		return areConnected(dungeon, z0, z1, intermediates, new HashSet<Zone>());
	}

	private static boolean areConnected(Dungeon dungeon, Zone z0, Zone z1, int intermediates, Set<Zone> z0s) {
		if (intermediates < 1)
			return false;
		final List<Zone> list = dungeon.connections.get(z0);
		if (list == null)
			return false;
		final int nb = list.size();
		for (int i = 0; i < nb; i++) {
			final Zone out = list.get(i);
			if (out.equals(z1))
				return true;
			final boolean added = z0s.add(out);
			if (added && areConnected(dungeon, out, z1, intermediates - 1, z0s))
				return true;
		}
		return false;
	}

	static List<List<Zone>> connectedComponents(Dungeon dungeon, List<Zone> zones) {
		if (zones.isEmpty())
			return Collections.emptyList();
		final List<List<Zone>> result = new ArrayList<List<Zone>>();
		final int nbz = zones.size();
		nextZone: for (int i = 0; i < nbz; i++) {
			final Zone zone = zones.get(i);
			final int rsz = result.size();
			for (int j = 0; j < rsz; j++) {
				final List<Zone> component = result.get(j);
				final int csz = component.size();
				for (int k = 0; k < csz; k++) {
					final Zone z = component.get(k);
					if (areConnected(dungeon, zone, z)) {
						component.add(zone);
						continue nextZone;
					}
				}
			}
			/* 'zone' belongs to no component. Creating a new one. */
			final List<Zone> component = new ArrayList<Zone>(nbz / 4);
			component.add(zone);
			result.add(component);
		}
		return result;
	}

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @return The room or corridor of {@code dungeon} that
	 *         {@link Zone#contains(int, int)} {@code (x, y)}, or {@code null}
	 *         if none.
	 */
	static /* @Nullable */ Zone findZoneContaining(Dungeon dungeon, int x, int y) {
		Zone result = findZoneContaining(dungeon.rooms, dungeon.boundingBoxes, x, y, true);
		if (result != null)
			return result;
		return findZoneContaining(dungeon.corridors, dungeon.boundingBoxes, x, y, false);
	}

	static int getNumberOfZones(Dungeon dungeon) {
		return dungeon.rooms.size() + dungeon.corridors.size();
	}

	static int getSizeOfWater(Dungeon dungeon) {
		return dungeon.waterPools == null ? 0 : Zones.size(dungeon.waterPools);
	}

	/**
	 * @param dungeon
	 * @param z
	 * @return The symbols in {@code z}.
	 */
	static EnumSet<DungeonSymbol> getSymbols(Dungeon dungeon, Zone z) {
		final EnumSet<DungeonSymbol> result = EnumSet.noneOf(DungeonSymbol.class);
		for (Coord c : z) {
			final boolean added = result.add(dungeon.getSymbol(c));
			if (added && result.size() == DungeonSymbol.values().length)
				/* We're done */
				break;
		}
		return result;
	}

	static boolean hasRoomOrCorridor(Dungeon dungeon, Zone z) {
		return dungeon.rooms.contains(z) || dungeon.corridors.contains(z);
	}

	static boolean hasStairs(Dungeon dungeon) {
		return dungeon.upwardStair != null && dungeon.downwardStair != null;
	}

	static boolean hasWaterPool(Dungeon dungeon, Zone z) {
		return dungeon.waterPools != null && dungeon.waterPools.contains(z);
	}

	static boolean hasZone(Dungeon dungeon, Zone z) {
		return hasRoomOrCorridor(dungeon, z) || hasWaterPool(dungeon, z);
	}

	/**
	 * @boolean checkValidity Whether to return {@code false} on out of bounds
	 *          coordinates.
	 * @return Whether zone is valid in dungeon and only contains members of
	 *         {@code syms}
	 */
	static boolean isOnly(Dungeon dungeon, Iterator<Coord> zone, EnumSet<DungeonSymbol> syms,
			boolean checkValidity) {
		while (zone.hasNext()) {
			final Coord c = zone.next();
			final DungeonSymbol sym = dungeon.getSymbol(c.x, c.y);
			if (sym == null) {
				/* Out of bounds */
				if (checkValidity)
					return false;
				continue;
			}
			if (!syms.contains(sym))
				/* Not the expected symbol */
				return false;
		}
		return true;
	}

	static boolean isRoom(Dungeon dungeon, Zone z) {
		return dungeon.rooms.contains(z);
	}

	static boolean isSurroundedBy(Dungeon dungeon, Zone z, EnumSet<DungeonSymbol> allowed) {
		for (Coord c : z.getExternalBorder()) {
			assert !z.contains(c);
			final DungeonSymbol sym = dungeon.getSymbol(c);
			if (!allowed.contains(sym))
				return false;
		}
		return true;
	}

	/**
	 * Remove all members of {@code z} from {@code dungeon}'s water pools.
	 * 
	 * @param dungeon
	 * @param z
	 * @parma buf Where to record removed cells, or null.
	 * @return Whether something was indeed removed.
	 */
	static boolean removeFromWaterPools(Dungeon dungeon, Zone z, /* @Nullable */ Collection<Coord> acc) {
		if (dungeon.waterPools == null)
			return false;
		boolean result = false;
		final int sz = dungeon.waterPools.size();
		nextCoord: for (Coord c : z) {
			for (int i = 0; i < sz; i++) {
				final boolean rmed = dungeon.waterPools.get(i).getState().remove(c);
				if (rmed) {
					result = true;
					if (acc != null)
						acc.add(c);
					continue nextCoord;
				}
			}
		}
		return result;
	}

	/**
	 * @param dungeon
	 * @param z
	 * @param rooms
	 *            Whether to consider {@code dungeon}'s rooms.
	 * @param corridors
	 *            Whether to consider {@code dungeon}'s corridors.
	 * @param deepWaters
	 *            Whether to consider {@code dungeon}'s water pools.
	 * @return The zones of {@code dungeon} that intersect with {@code z}, or
	 *         null if none.
	 */
	private static /* @Nullable */ List<Zone> findIntersectingZones(Dungeon dungeon, Zone z, boolean rooms,
			boolean corridors, boolean deepWaters) {
		List<Zone> result = null;
		if (rooms)
			result = findIntersectingZones(z, dungeon.rooms, result);
		if (result != null)
			return result;
		if (corridors)
			result = findIntersectingZones(z, dungeon.corridors, result);
		if (result != null)
			return result;
		if (deepWaters)
			result = findIntersectingZones(z, dungeon.waterPools, result);
		return result;
	}

	private static /* @Nullable */ List<Zone> findIntersectingZones(Zone z, List<? extends Zone> others,
			List<Zone> buf) {
		List<Zone> result = buf;
		if (others == null)
			return result;
		final int nbo = others.size();
		for (int i = 0; i < nbo; i++) {
			final Zone other = others.get(i);
			if (other.intersectsWith(z)) {
				if (result == null)
					result = new ArrayList<Zone>();
				result.add(other);
			}
		}
		return result;
	}

	private static /* @Nullable */ Zone findZoneContaining(/* @Nullable */ List<? extends Zone> zones,
			/* @Nullable */ Map<Zone, ? extends Zone> boundingBoxes, int x, int y, boolean roomOrCorridors) {
		if (zones == null)
			return null;
		final int nb = zones.size();
		for (int i = 0; i < nb; i++) {
			final Zone z = zones.get(i);
			if (boundingBoxes != null) {
				final Zone boundingBox = boundingBoxes.get(z);
				if (boundingBox == null) {
					if (roomOrCorridors) {
						assert z instanceof Rectangle : "There should be a bounding box for room " + z;
						/*
						 * It's okay for 'z' not to have a bounding box, since
						 * it is a rectangle. Its bounding box would be 'z'
						 * itself.
						 */
					}
				} else {
					assert boundingBox.contains(z) : boundingBox + " isn't a bounding box of " + z;
					if (boundingBox != null && !boundingBox.contains(x, y))
						continue;
				}
			}
			if (z.contains(x, y))
				return z;
		}
		return null;
	}

}
