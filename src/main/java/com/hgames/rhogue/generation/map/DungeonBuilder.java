package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hgames.lib.collection.Multimaps;

import squidpony.squidgrid.mapping.Rectangle;
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
		assert hasZone(dungeon, z1);
		assert hasZone(dungeon, z2);
		Multimaps.addToListMultimapIfAbsent(dungeon.connections, z1, z2);
		Multimaps.addToListMultimapIfAbsent(dungeon.connections, z2, z1);
	}

	/** Prefer this method over direct mutations, it eases debugging. */
	static void addZone(Dungeon dungeon, Zone z, /* @Nullable */ Rectangle boundingBox,
			boolean roomOrCorridor) {
		/* Zone should not intersect with existing zones */
		assert findIntersectingZones(dungeon, z, true, true) == null;
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
	 * Sets {@code sym} everywhere in {@code dungeon}.
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
		if (intermediates < 1)
			return false;
		final List<Zone> list = dungeon.connections.get(z0);
		if (list == null)
			return false;
		final int nb = list.size();
		for (int i = 0; i < nb; i++) {
			final Zone out = list.get(i);
			if (out.equals(z1) || areConnected(dungeon, out, z1, intermediates - 1))
				return true;
		}
		return false;
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

	static boolean hasZone(Dungeon dungeon, Zone z) {
		return dungeon.rooms.contains(z) || dungeon.corridors.contains(z);
	}

	/**
	 * @param dungeon
	 * @param z
	 * @param rooms
	 *            Whether to consider {@code dungeon}'s rooms.
	 * @param corridors
	 *            Whether to consider {@code dungeon}'s corridors.
	 * @return The zones of {@code dungeon} that intersect with {@code z}, or
	 *         null if none.
	 */
	private static /* @Nullable */ List<Zone> findIntersectingZones(Dungeon dungeon, Zone z, boolean rooms,
			boolean corridors) {
		List<Zone> result = null;
		if (rooms)
			result = findIntersectingZones(z, dungeon.rooms, result);
		if (corridors)
			result = findIntersectingZones(z, dungeon.corridors, result);
		return result;
	}

	private static /* @Nullable */ List<Zone> findIntersectingZones(Zone z, List<Zone> others,
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
