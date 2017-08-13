package com.hgames.rhogue.generation.map;

/**
 * How to draw a {@link DungeonSymbol} using {@code char}s.
 * 
 * @author smelC
 */
public interface IDungeonSymbolDrawer {

	/**
	 * @param sym
	 * @return {@code sym} printed
	 */
	public char draw(DungeonSymbol sym);

}
