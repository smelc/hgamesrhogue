package com.hgames.rhogue.generation.map;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hgames.lib.collection.Multimaps;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * Methods to mutate a {@link Dungeon}. Do not change visibilities, they are
 * intended.
 * 
 * @author smelC
 */
class DungeonBuilder {

	static void addConnection(Dungeon dungeon, Zone z1, Zone z2) {
		assert z1 != z2;
		if (z1 == z2)
			throw new IllegalStateException("A zone should not be connected to itself");
		assert hasZone(dungeon, z1);
		assert hasZone(dungeon, z2);
		Multimaps.addToListMultimapIfAbsent(dungeon.connections, z1, z2);
		Multimaps.addToListMultimapIfAbsent(dungeon.connections, z2, z1);
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
	 * @param dungeon
	 * @param x
	 * @param y
	 * @return The room or corridor of {@code dungeon} that
	 *         {@link Zone#contains(int, int)} {@code (x, y)}, or {@code null}
	 *         if none.
	 */
	static /* @Nullable */ Zone findZoneContaining(Dungeon dungeon, int x, int y) {
		Zone result = findZoneContaining(dungeon.rooms, dungeon.boundingBoxes, x, y);
		if (result != null)
			return result;
		return findZoneContaining(dungeon.corridors, dungeon.boundingBoxes, x, y);
	}

	static boolean hasZone(Dungeon dungeon, Zone z) {
		return dungeon.rooms.contains(z) || dungeon.corridors.contains(z);
	}

	private static /* @Nullable */ Zone findZoneContaining(/* @Nullable */ List<? extends Zone> zones,
			/* @Nullable */ Map<Zone, ? extends Zone> boundingBoxes, int x, int y) {
		if (zones == null)
			return null;
		final int nb = zones.size();
		for (int i = 0; i < nb; i++) {
			final Zone z = zones.get(i);
			if (boundingBoxes != null) {
				final Zone boundingBox = boundingBoxes.get(z);
				assert boundingBox.contains(z) : boundingBox + " isn't a bounding box of " + z;
				if (boundingBox != null && !boundingBox.contains(x, y))
					continue;
			}
			if (z.contains(x, y))
				return z;
		}
		return null;
	}

}
