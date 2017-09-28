package com.hgames.rhogue.generation.map.flood;

import com.hgames.lib.Arrays;
import com.hgames.lib.Exceptions;
import com.hgames.rhogue.generation.map.DungeonSymbol;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * An instance of {@link FloodFill} tuned to generate water first in
 * {@link DungeonSymbol}.
 * 
 * @author smelC
 */
public class DungeonWaterStartFloodFill extends FloodFill {

	protected final DungeonSymbol[][] map;

	/**
	 * A fresh instance.
	 * 
	 * @param map
	 * @param width
	 * @param height
	 */
	public DungeonWaterStartFloodFill(DungeonSymbol[][] map, int width, int height) {
		super(width, height);
		this.map = map;
	}

	@Override
	protected boolean canBeFloodOn(Coord c) {
		final DungeonSymbol sym = map[c.x][c.y];
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case FLOOR:
		case GRASS:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
			return false;
		case WALL:
			return isValidFloodNeighbor(c);
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

	private boolean isValidFloodNeighbor(Coord c) {
		for (Direction dir : Direction.OUTWARDS) {
			final Coord neighbor = c.translate(dir);
			final DungeonSymbol sym = Arrays.getIfValid(map, neighbor.x, neighbor.y);
			if (sym == null)
				/* oob */
				continue;
			switch (sym) {
			case DOOR:
				/* Water close to door is forbidden */
				return false;
			case CHASM:
			case DEEP_WATER:
			case FLOOR:
			case GRASS:
			case HIGH_GRASS:
			case SHALLOW_WATER:
			case STAIR_DOWN:
			case STAIR_UP:
			case WALL:
				return true;
			}
			throw Exceptions.newUnmatchedISE(sym);
		}
		return true;
	}

}
