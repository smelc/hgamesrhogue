package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.hgames.lib.Exceptions;
import com.hgames.rhogue.zone.ListZone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * How to obtain an instance of {@link Dungeon} out of a {@link DungeonSymbol}
 * map.
 * 
 * @author smelC
 * @see Dungeon
 */
public class DungeonAdapter {

	/** Whether a cell of {@link #map} has been interpreted already */
	protected final boolean[][] interpreted;
	protected final DungeonSymbol[][] map;

	/**
	 * A fresh instance on which {@link #build()} should be called exactly once.
	 * 
	 * @param map
	 *            A reference to the map to interpret. <b>Taken over</b> by
	 *            {@code this}.
	 */
	public DungeonAdapter(DungeonSymbol[][] map) {
		final int width = map.length;
		final int height = width == 0 ? 0 : map[0].length;

		this.interpreted = new boolean[width][height];
		this.map = map;
	}

	/**
	 * @return The dungeon.
	 */
	public Dungeon build() {
		final int width = map.length;
		final int height = width == 0 ? 0 : map[0].length;
		final Dungeon result = new Dungeon(map);
		final DungeonBuilder builder = result.getBuilder();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final DungeonSymbol sym = map[x][y];
				switch (sym) {
				case CHASM:
					break;
				case DEEP_WATER:
					if (!interpreted[x][y])
						builder.addWaterPool(new ListZone(spill(x, y, EnumSet.of(DungeonSymbol.DEEP_WATER), null)));
					continue;
				case DOOR:
				case FLOOR:
				case GRASS:
				case HIGH_GRASS:
				case SHALLOW_WATER:
					if (!interpreted[x][y]) {
						final EnumMap<DungeonSymbol, List<Coord>> terrainToZones = new EnumMap<DungeonSymbol, List<Coord>>(
								DungeonSymbol.class);
						terrainToZones.put(DungeonSymbol.GRASS, new ArrayList<Coord>());
						terrainToZones.put(DungeonSymbol.HIGH_GRASS, new ArrayList<Coord>());
						final EnumSet<DungeonSymbol> syms = EnumSet.of(DungeonSymbol.DOOR, DungeonSymbol.FLOOR,
								DungeonSymbol.GRASS, DungeonSymbol.HIGH_GRASS, DungeonSymbol.SHALLOW_WATER);
						builder.addWaterPool(new ListZone(spill(x, y, syms, terrainToZones)));
						final List<Coord> grass = terrainToZones.get(DungeonSymbol.GRASS);
						if (!grass.isEmpty())
							builder.addGrassPool(new ListZone(grass));
						final List<Coord> hgrass = terrainToZones.get(DungeonSymbol.HIGH_GRASS);
						if (!hgrass.isEmpty())
							builder.addHighGrassPool(new ListZone(hgrass));
					}
					continue;
				case STAIR_DOWN:
					assert !interpreted[x][y];
					builder.setStair(x, y, false);
					interpreted[x][y] = true;
					continue;
				case STAIR_UP:
					assert !interpreted[x][y];
					builder.setStair(x, y, true);
					interpreted[x][y] = true;
					continue;
				case WALL:
					/* Not tracked */
					continue;
				}
				throw Exceptions.newUnmatchedISE(sym);
			}
		}
		return result;
	}

	/**
	 * Spill from {@code (x_, y_)} on cells whose symbol is in {@code syms} and that
	 * haven't been interpreted yet.
	 * 
	 * @param x_
	 * @param y_
	 * @param syms
	 * @param sym2Zones
	 *            An optional map that is filled for members of {@code trackeds}.
	 */
	private List<Coord> spill(int x_, int y_, EnumSet<DungeonSymbol> syms,
			/* @Nullable */ EnumMap<DungeonSymbol, List<Coord>> sym2Zones) {
		final List<Coord> result = new ArrayList<Coord>();
		assert syms.contains(map[x_][y_]);
		final Queue<Coord> todos = new LinkedList<Coord>();
		todos.add(Coord.get(x_, y_));
		while (!todos.isEmpty()) {
			final Coord c = todos.remove();
			final int x = c.x;
			final int y = c.y;
			if (interpreted[x][y])
				continue;
			final DungeonSymbol sym = map[x][y];
			assert syms.contains(sym);
			assert !result.contains(c);
			result.add(c);
			interpreted[x][y] = true;
			if (sym2Zones != null && sym2Zones.containsKey(sym))
				add(sym2Zones, sym, c);
			for (Direction dir : Direction.CARDINALS) {
				final Coord d = c.translate(dir);
				if (!interpreted[d.x][d.y] && syms.contains(map[x][y]))
					todos.add(d);
			}
		}
		return result;
	}

	private static <T extends Enum<T>> void add(EnumMap<T, List<Coord>> map, T sym, Coord c) {
		final List<Coord> list = map.get(sym);
		assert list != null;
		assert !list.contains(c);
		list.add(c);
	}

}
