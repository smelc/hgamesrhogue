package com.hgames.rhogue.generation.map.dungeon;

import java.util.ArrayList;
import java.util.Collection;
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
					if (!interpreted[x][y])
						builder.addChasm(new ListZone(spill(x, y, EnumSet.of(sym))));
					continue;
				case DEEP_WATER:
					if (!interpreted[x][y])
						builder.addWaterPool(new ListZone(spill(x, y, EnumSet.of(sym))));
					continue;
				case SHALLOW_WATER:
				case DOOR:
					assert false : sym + " is disallowed in text maps";
					continue;
				case FLOOR:
					if (!interpreted[x][y])
						/* null: we cannot provide a meaningful bounding box */
						builder.addZone(new ListZone(spill(x, y, EnumSet.of(sym))), null, true);
					continue;
				case GRASS:
					if (!interpreted[x][y])
						builder.addGrassPool(new ListZone(spill(x, y, EnumSet.of(sym))), EnumSet.of(sym));
					continue;
				case HIGH_GRASS:
					if (!interpreted[x][y])
						builder.addHighGrassPool(new ListZone(spill(x, y, EnumSet.of(sym))));
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
	 *            The symbols to spill on.
	 */
	private List<Coord> spill(int x_, int y_, EnumSet<DungeonSymbol> syms) {
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
			for (Direction dir : Direction.CARDINALS) {
				final Coord d = c.translate(dir);
				if (!interpreted[d.x][d.y] && syms.contains(map[d.x][d.y]))
					todos.add(d);
			}
		}
		assert containsAll(syms, result);
		return result;
	}

	private boolean containsAll(EnumSet<DungeonSymbol> syms, Collection<Coord> coords) {
		for (Coord coord : coords) {
			if (!syms.contains(map[coord.x][coord.y])) {
				assert false : coord + "'s symbol (" + map[coord.x][coord.y] + ") doesn't belong to " + syms;
				return false;
			}
		}
		return true;
	}

}
