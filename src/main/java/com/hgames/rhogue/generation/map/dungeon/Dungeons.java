package com.hgames.rhogue.generation.map.dungeon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.hgames.lib.collection.list.Lists;
import com.hgames.lib.collection.multiset.EnumMultiset;
import com.hgames.rhogue.zone.Rectangle;
import com.hgames.rhogue.zone.Zone;
import com.hgames.rhogue.zone.Zones;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * Query methods concerning {@link Dungeon}, to avoid avoid a huge API in
 * {@link Dungeon}. Methods in this file are sorted, keep it that way.
 * 
 * @author smelC
 */
public class Dungeons {

	/**
	 * A flag to help me detect bugs. When this flag is ON, every non-Rectangle room
	 * should have a bounding box. This is guaranteed by {@link DungeonGenerator},
	 * but clients crafting dungeons on their own can break it.
	 */
	private static final Boolean ENFORCE_BBOX_PRECISION = Boolean.FALSE;

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
	 *            A bound on the allowed intermediates. 1 is the minimum (a corridor
	 *            connecting the two rooms).
	 * @return Whether {@code z0} and {@code z1} are connected by at most
	 *         {@code intermediates} zones.
	 */
	public static boolean areConnected(Dungeon dungeon, Zone z0, Zone z1, int intermediates) {
		return areConnected(dungeon, z0, z1, intermediates, new HashSet<Zone>());
	}

	/**
	 * @param dungeon
	 * @param zones
	 * @return The connected components in {@code zones}.
	 */
	public static List<List<Zone>> connectedComponents(Dungeon dungeon, List<Zone> zones) {
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
	 * @param zone
	 *            The starting points (which gets filtered by {@code walkables}
	 *            too).
	 * @param walkables
	 *            The cells through which crawling can go.
	 * @param upOrDown
	 *            Whether to consider the upwards stair (true), the downwards stair
	 *            (false), or both (null).
	 * @return Whether it is possible to go from {@code zone} to a stair.
	 */
	public static boolean connectedToStair(Dungeon dungeon, Zone zone, Set<DungeonSymbol> walkables,
			/* @Nullable */ Boolean upOrDown) {
		final Queue<Coord> todos = Lists.newLinkedList();
		final Set<Coord> dones = new HashSet<Coord>();
		final Coord stairUp = dungeon.getStair(true);
		final Coord stairDown = dungeon.getStair(false);
		for (Coord coord : zone) {
			final DungeonSymbol sym = dungeon.getSymbol(coord);
			if (sym != null && walkables.contains(sym))
				todos.add(coord);
			assert !coord.equals(stairUp) : "Zone shouldn't contain stair up";
			assert !coord.equals(stairDown) : "Zone shouldn't contain stair down";
		}
		final boolean checkUp = upOrDown == null || upOrDown.booleanValue();
		final boolean checkDown = upOrDown == null || (!upOrDown.booleanValue());
		while (!todos.isEmpty()) {
			final Coord coord = todos.poll();
			assert walkables.contains(dungeon.getSymbol(coord));
			final boolean added = dones.contains(coord);
			if (!added)
				/* Done already */
				continue;
			for (Direction dir : Direction.CARDINALS) {
				final Coord neighbor = coord.translate(dir);
				if (dones.contains(neighbor))
					/* Done already */
					continue;
				final DungeonSymbol sym = dungeon.getSymbol(neighbor);
				if (sym == null)
					/* Out of bounds */
					continue;
				if (!walkables.contains(sym))
					continue;
				if (checkUp && neighbor.isAdjacent(stairUp))
					return true;
				if (checkDown && neighbor.isAdjacent(stairDown))
					return true;
				todos.add(neighbor);
			}
		}
		return false;
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
	 * @return The zones of {@code dungeon} that intersect with {@code z}, or null
	 *         if none.
	 */
	public static /* @Nullable */ List<Zone> findIntersectingZones(Dungeon dungeon, Zone z, boolean rooms,
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

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @return The room or corridor of {@code dungeon} that
	 *         {@link Zone#contains(int, int)} {@code (x, y)}, or {@code null} if
	 *         none.
	 */
	public static /* @Nullable */ Zone findRoomOrCorridorContaining(Dungeon dungeon, int x, int y) {
		Zone result = findZoneContaining(dungeon.rooms, dungeon.boundingBoxes, x, y, true);
		if (result != null)
			return result;
		return findZoneContaining(dungeon.corridors, dungeon.boundingBoxes, x, y, false);
	}

	/**
	 * Puts in {@code acc} coordinates of {@code z}'s internal border that are
	 * adjacent to other zones, i.e {@code z}'s doorways.
	 * 
	 * @param dungeon
	 * @param z
	 * @param acc
	 * @param considerDiagonals
	 * @return acc if non-null otherwise a fresh list.
	 */
	public static List<Coord> getDoorways(Dungeon dungeon, Zone z, List<Coord> acc, boolean considerDiagonals) {
		final List<Coord> result = acc == null ? new ArrayList<Coord>() : acc;
		final List<Coord> iborder = z.getInternalBorder();
		final int isz = iborder.size();
		final Direction[] dirs = considerDiagonals ? Direction.OUTWARDS : Direction.CARDINALS;
		nextCoord: for (int i = 0; i < isz; i++) {
			final Coord c = iborder.get(i);
			for (Direction dir : dirs) {
				final Coord d = c.translate(dir);
				final Zone zo = dungeon.findRoomOrCorridorContaining(d);
				if (zo != null && zo != z) {
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
	 * @param x
	 * @param y
	 * @param considerDiagonals
	 *            Whether to consider cardinal neighbors.
	 * @return The neighbors of {@code (x, y)}. The size can be smaller than
	 *         expected since null symbols and out of bound neighbors are skipped.
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
	 * @param x
	 * @param y
	 * @param considerDiagonals
	 *            Whether to consider diagonal neighbors.
	 * @param acc
	 * @return {@code acc} if non-null, otherwise a fresh list; where neighboring
	 *         zones of {@code (x, y)} have been added.
	 */
	public static Collection<Zone> getNeighborz(Dungeon dungeon, int x, int y, boolean considerDiagonals,
			Collection<Zone> acc) {
		final Collection<Zone> result = acc == null ? Lists.<Zone> newArrayList() : acc;
		final Direction[] dirs = considerDiagonals ? Direction.OUTWARDS : Direction.CARDINALS;
		for (Direction dir : dirs) {
			final Coord neighbor = Coord.get(x + dir.deltaX, y + dir.deltaY);
			final Zone z = dungeon.findRoomOrCorridorContaining(neighbor);
			if (z != null && !result.contains(z))
				result.add(z);
		}
		return result;
	}

	/**
	 * @param dungeon
	 * @param z
	 *            A room or corridor
	 * @return The number of connections of {@code z}.
	 */
	public static int getNumberOfConnections(Dungeon dungeon, Zone z) {
		assert Dungeons.hasRoomOrCorridor(dungeon, z);
		final List<Zone> out = dungeon.connections.get(z);
		return out == null ? 0 : out.size();
	}

	/**
	 * @param dungeon
	 * @return The sum of the number of rooms and of corridors.
	 */
	public static int getNumberOfZones(Dungeon dungeon) {
		return dungeon.rooms.size() + dungeon.corridors.size();
	}

	/**
	 * @param dungeon
	 * @return The sum of the sizes of rooms and corridors.
	 */
	public static int getSizeOfRoomsAndCorridors(Dungeon dungeon) {
		return Zones.size(dungeon.corridors) + Zones.size(dungeon.rooms);
	}

	/**
	 * @param dungeon
	 * @return The sum of the sizes of deep water zones.
	 */
	public static int getSizeOfDeepWater(Dungeon dungeon) {
		return dungeon.waterPools == null ? 0 : Zones.size(dungeon.waterPools);
	}

	/**
	 * @param dungeon
	 * @param z
	 * @return The symbols in {@code z}.
	 */
	public static EnumSet<DungeonSymbol> getSymbols(Dungeon dungeon, Zone z) {
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
	 * @return Whether {@code z} is a chasm in {@code dungeon}.
	 */
	public static boolean hasChasm(Dungeon dungeon, Zone z) {
		return dungeon.chasms != null && dungeon.chasms.contains(z);
	}

	/**
	 * @param dungeon
	 * @param z
	 * @return Whether {@code z} is a disconnected room in {@code dungeon}.
	 */
	public static boolean hasDisconnectedRoom(Dungeon dungeon, Zone z) {
		return dungeon.disconnectedRooms != null && dungeon.disconnectedRooms.contains(z);
	}

	/**
	 * @param dungeon
	 * @param z
	 * @param lowOrHighGrass
	 * @return Whether {@code z} is a grass pool of {@code dungeon}.
	 */
	public static boolean hasGrassPool(Dungeon dungeon, Zone z, boolean lowOrHighGrass) {
		final List<Zone> target = lowOrHighGrass ? dungeon.grassPools : dungeon.highGrassPools;
		return target != null && target.contains(z);
	}

	/**
	 * @param dungeon
	 * @param c
	 * @param sym
	 * @param considerDiagonals
	 *            Whether to consider diagonal neighbors.
	 * @return true if {@code c} has {@code sym} as neighbor.
	 */
	public static boolean hasNeighbor(Dungeon dungeon, Coord c, DungeonSymbol sym, boolean considerDiagonals) {
		final Direction[] dirs = considerDiagonals ? Direction.OUTWARDS : Direction.CARDINALS;
		for (Direction dir : dirs) {
			if (sym == dungeon.getSymbol(c.translate(dir)))
				return true;
		}
		return false;
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
	 * @param z
	 * @param sym
	 * @return Whether {@code z} contains {@code sym}.
	 */
	public static boolean hasSymbol(Dungeon dungeon, Zone z, DungeonSymbol sym) {
		for (Coord c : z) {
			if (sym == dungeon.getSymbol(c))
				return true;
		}
		return false;
	}

	/**
	 * @param dungeon
	 * @param z
	 * @return Whether {@code z} is a deep water pool of {@code dungeon}.
	 */
	public static boolean hasWaterPool(Dungeon dungeon, Zone z) {
		return dungeon.waterPools != null && dungeon.waterPools.contains(z);
	}

	/**
	 * @param dungeon
	 * @param z
	 * @return If {@code z} is a room, corridor, or deep water in {@code dungeon}.
	 */
	public static boolean hasZone(Dungeon dungeon, Zone z) {
		return hasChasm(dungeon, z) || hasDisconnectedRoom(dungeon, z) || hasRoomOrCorridor(dungeon, z)
				|| hasGrassPool(dungeon, z, true) || hasGrassPool(dungeon, z, false) || hasWaterPool(dungeon, z);
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
	 * @param zone
	 * @param syms
	 * @param checkValidity
	 *            Whether to return {@code false} on out of bounds coordinates.
	 * @return Whether {@code zone} is valid in {@code dungeon} and only contains
	 *         members of {@code syms}
	 */
	public static boolean isOnly(Dungeon dungeon, Iterator<Coord> zone, EnumSet<DungeonSymbol> syms,
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

	/**
	 * @param dungeon
	 * @param z
	 * @return Whether {@code z} is a a room in {@code dungeon}.
	 */
	public static boolean isRoom(Dungeon dungeon, Zone z) {
		return dungeon.rooms.contains(z);
	}

	/**
	 * @param dungeon
	 * @param z
	 * @param allowed
	 * @return true if {@code z} {@link Zone#getExternalBorder() external border}'s
	 *         symols are all in {@code allowed}.
	 */
	public static boolean isSurroundedBy(Dungeon dungeon, Zone z, EnumSet<DungeonSymbol> allowed) {
		final List<Coord> extBorder = z.getExternalBorder();
		final int sz = extBorder.size();
		for (int i = 0; i < sz; i++) {
			final Coord c = extBorder.get(i);
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
	 * @return Whether at least {@code objective} cells are reachable from
	 *         {@code z}.
	 */
	public static boolean reachesAtLeast(Dungeon dungeon, Zone z, int objective) {
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
						assert !ENFORCE_BBOX_PRECISION.booleanValue()
								|| z instanceof Rectangle : "There should be a bounding box for room " + z;
						/*
						 * It's okay for 'z' not to have a bounding box, since it is a rectangle. Its
						 * bounding box would be 'z' itself.
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
