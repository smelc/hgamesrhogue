package com.hgames.rhogue.generation.map.draw;

import com.hgames.rhogue.generation.map.dungeon.DungeonSymbol;
import com.hgames.rhogue.generation.map.dungeon.DungeonSymbolDrawer;

/**
 * The instantiation of {@link Generic2DArrayDrawer} on {@link DungeonSymbol}.
 * 
 * @author smelC
 */
public class DungeonSymbolArrayDrawer extends Generic2DArrayDrawer<DungeonSymbol> {

	protected final IDungeonSymbolDrawer symd;

	/**
	 * @param lineSeparator
	 *            The separator to use. {@code System#getProperty("line.separator")}
	 *            isn't used, because it isn't GWT-compatible. A fresh instance that
	 *            uses {@link DungeonSymbolArrayDrawer}. Handy to debug.
	 */
	public DungeonSymbolArrayDrawer(String lineSeparator) {
		this(lineSeparator, new DungeonSymbolDrawer());
	}

	/**
	 * @param lineSeparator
	 *            The separator to use. {@code System#getProperty("line.separator")}
	 *            isn't used, because it isn't GWT-compatible.
	 * @param symd
	 *            How to draw an individual symbol.
	 */
	public DungeonSymbolArrayDrawer(String lineSeparator, IDungeonSymbolDrawer symd) {
		super(lineSeparator);
		this.symd = symd;
	}

	@Override
	protected char draw(DungeonSymbol sym) {
		return symd.draw(sym);
	}

}
