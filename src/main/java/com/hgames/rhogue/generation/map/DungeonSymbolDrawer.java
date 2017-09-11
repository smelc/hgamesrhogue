package com.hgames.rhogue.generation.map;

import com.hgames.lib.Exceptions;

/**
 * A standard implementation of {@link IDungeonSymbolDrawer}.
 * 
 * @author smelC
 */
public class DungeonSymbolDrawer implements IDungeonSymbolDrawer {

	@Override
	public char draw(DungeonSymbol sym) {
		if (sym == null)
			return draw(DungeonSymbol.WALL);
		else {
			switch (sym) {
			case CHASM:
				return ':';
			case DEEP_WATER:
				return '~';
			case DOOR:
				return '+';
			case FLOOR:
				return '.';
			case GRASS:
				return '"';
			case HIGH_GRASS:
				return 'ψ';
			case SHALLOW_WATER:
				return ',';
			case STAIR_DOWN:
				return '>';
			case STAIR_UP:
				return '<';
			case WALL:
				return '#';
			}
			throw Exceptions.newUnmatchedISE(sym);
		}
	}

}
