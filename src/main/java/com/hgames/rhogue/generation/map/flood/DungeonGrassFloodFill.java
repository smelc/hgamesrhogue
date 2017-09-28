package com.hgames.rhogue.generation.map.flood;

import com.hgames.lib.Exceptions;
import com.hgames.rhogue.generation.map.DungeonSymbol;

import squidpony.squidmath.Coord;

/**
 * An instance of {@link FloodFill} to generate grass.
 * 
 * @author smelC
 */
public class DungeonGrassFloodFill extends FloodFill {

	protected final DungeonSymbol[][] map;

	/**
	 * @param map
	 */
	public DungeonGrassFloodFill(DungeonSymbol[][] map) {
		super(map.length, map.length == 0 ? 0 : map[0].length);
		this.map = map;
	}

	@Override
	protected boolean canBeFloodOn(Coord c) {
		final DungeonSymbol sym = map[c.x][c.y];
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case GRASS:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
		case WALL:
			return false;
		case FLOOR:
			return true;
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

	@Override
	protected int getMinFloodSize() {
		return 6;
	}

}
