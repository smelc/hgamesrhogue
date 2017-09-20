package com.hgames.rhogue.generation.map.draw;

import com.hgames.rhogue.generation.map.DungeonSymbol;

/**
 * How to draw a {@link DungeonSymbol} using {@code char}s.
 * 
 * @author smelC
 */
public interface IDungeonSymbolDrawer {

	/**
	 * @param sym
	 *            The symbol. Can be null, which is equivalent to
	 *            {@link DungeonSymbol#WALL}.
	 * @return {@code sym} printed
	 */
	public char draw(/* @Nullable */ DungeonSymbol sym);

}
