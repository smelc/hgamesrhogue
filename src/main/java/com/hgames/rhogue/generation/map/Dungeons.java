package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.hgames.lib.collection.Collections;
import com.hgames.lib.collection.multiset.EnumMultiset;
import com.hgames.rhogue.zone.Zones;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * Query methods concerning {@link Dungeon}, to avoid avoid a huge API in
 * {@link Dungeon}.
 * 
 * @author smelC
 */
public class Dungeons {

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @return The room or corridor of {@code dungeon} that
	 *         {@link Zone#contains(int, int)} {@code (x, y)}, or {@code null}
	 *         if none.
	 */
	public static Zone findZoneContaining(Dungeon dungeon, int x, int y) {
		return findZoneContaining(dungeon, x, y);
	}

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @param considerDiagonals
	 *            Whether to consider cardinal neighbors.
	 * @return The neighbors of {@code (x, y)}. The size can be smaller than
	 *         expected since null symbols and out of bound neighbors are
	 *         skipped.
	 */
	public static EnumMultiset<DungeonSymbol> getNeighbors(Dungeon dungeon, int x, int y, boolean considerDiagonals) {
		final EnumMultiset<DungeonSymbol> result = EnumMultiset.noneOf(DungeonSymbol.class);
		final Direction[] dirs = considerDiagonals ? Direction.OUTWARDS : Direction.CARDINALS;
		for (Direction dir : dirs) {
			final int nx = x + dir.deltaX;
			final int ny = y + dir.deltaY;
			final DungeonSymbol sym = dungeon.getSymbol(nx, ny);
			if (sym != null)
				result.add(sym);
			/* else out of bound */
		}
		return result;
	}

	/**
	 * @param dungeon
	 * @param z
	 * @return Whether {@code z} is a room or corridor in {@code z}.
	 */
	public static boolean hasRoomOrCorridor(Dungeon dungeon, Zone z) {
		return dungeon.rooms.contains(z) || dungeon.corridors.contains(z);
	}

	/**
	 * @param dungeon
	 * @return Whether {@code dungeon}'s stairs are set.
	 */
	public static boolean hasStairs(Dungeon dungeon) {
		return dungeon.upwardStair != null && dungeon.downwardStair != null;
	}

	/**
	 * @param dungeon
	 * @param c
	 * @return Whether {@code c} is on {@code dungeon}'s edge.
	 */
	public static boolean isOnEdge(Dungeon dungeon, Coord c) {
		return c.x == 0 || c.x == dungeon.getWidth() - 1 || c.y == 0 || c.y == dungeon.getHeight() - 1;
	}

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @return Whether {@code (x, y)} is on the dungeon's edge.
	 */
	public static boolean isOnEdge(Dungeon dungeon, int x, int y) {
		return x == 0 || x == dungeon.getWidth() - 1 || y == 0 || y == dungeon.getHeight() - 1;
	}

	/**
	 * @param dungeon
	 * @param it
	 * @return Whether a member of {@code it} is on the edge.
	 */
	public static boolean anyOnEdge(Dungeon dungeon, Iterator<Coord> it) {
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
	public static boolean areConnected(Dungeon dungeon, Zone z0, Zone z1) {
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
	public static boolean areConnected(Dungeon dungeon, Zone z0, Zone z1, int intermediates) {
		return areConnected(dungeon, z0, z1, intermediates, new HashSet<Zone>());
	}

	private boolean areConnected(Zone z0, Zone z1, int intermediates, Set<Zone> z0s) {
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
			if (added && areConnected(out, z1, intermediates - 1, z0s))
				return true;
		}
		return false;
	}

	static List<List<Zone>> connectedComponents(List<Zone> zones) {
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
					if (areConnected(zone, z)) {
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
	 * @param x
	 * @param y
	 * @return The room or corridor of {@code dungeon} that
	 *         {@link Zone#contains(int, int)} {@code (x, y)}, or {@code null}
	 *         if none.
	 */
	public /* @Nullable */ Zone findZoneContaining(int x, int y) {
		Zone result = findZoneContaining(dungeon.rooms, dungeon.boundingBoxes, x, y, true);
		if (result != null)
			return result;
		return findZoneContaining(dungeon.corridors, dungeon.boundingBoxes, x, y, false);
	}

	// FIXME CH Move me to Dungeons
	public static int getNumberOfZones(Dungeon dungeon) {
		return dungeon.rooms.size() + dungeon.corridors.size();
	}

	// FIXME CH Move me to Dungeons
	static int getSizeOfWater(Dungeon dungeon) {
		return dungeon.waterPools == null ? 0 : Zones.size(dungeon.waterPools);
	}

	/**
	 * @param dungeon
	 * @param z
	 * @return The symbols in {@code z}.
	 */
	// FIXME CH Move me to Dungeons
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

	/**
	 * @param dungeon
	 * @param z
	 * @return Whether {@code z} is a deep water pool of {@code dungeon}.
	 */
	public static boolean hasWaterPool(Dungeon dungeon, Zone z) {
		return dungeon.waterPools != null && dungeon.waterPools.contains(z);
	}

	public static boolean hasZone(Dungeon dungeon, Zone z) {
		return hasRoomOrCorridor(dungeon, z) || hasWaterPool(dungeon, z);
	}

	/**
	 * @boolean checkValidity Whether to return {@code false} on out of bounds
	 *          coordinates.
	 * @return Whether zone is valid in dungeon and only contains members of
	 *         {@code syms}
	 */
	static boolean isOnly(Dungeon dungeon, Iterator<Coord> zone, EnumSet<DungeonSymbol> syms, boolean checkValidity) {
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
	 * @param dungeon
	 * @param z
	 * @param objective
	 * @return Whether at least {@code objective} cells are reacahable from
	 *         {@code z}.
	 */
	static boolean reachesAtLeast(Dungeon dungeon, Zone z, int objective) {
		int result = z.size();
		if (objective <= result)
			return true;
		final Iterator<Zone> connectedsToZ = new DungeonZonesCrawler(dungeon, z).iterator();
		while (connectedsToZ.hasNext()) {
			result += connectedsToZ.next().size();
			if (objective <= result)
				return true;
		}
		return false;
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
