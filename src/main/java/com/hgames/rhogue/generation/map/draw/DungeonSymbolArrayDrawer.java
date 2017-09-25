package com.hgames.rhogue.generation.map.draw;

import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.generation.map.DungeonSymbolDrawer;

/**
 * The instantiation of {@link Generic2DArrayDrawer} on {@link DungeonSymbol}.
 * 
 * @author smelC
 */
public class DungeonSymbolArrayDrawer extends Generic2DArrayDrawer<DungeonSymbol> {

	protected final IDungeonSymbolDrawer symd;

	/**
	 * A fresh instance that uses {@link DungeonSymbolArrayDrawer}. Handy to debug.
	 */
	public DungeonSymbolArrayDrawer() {
		this(new DungeonSymbolDrawer());
	}

	/**
	 * @param symd
	 *            How to draw an individual symbol.
	 */
	public DungeonSymbolArrayDrawer(IDungeonSymbolDrawer symd) {
		this.symd = symd;
	}

	@Override
	protected char draw(DungeonSymbol sym) {
		return symd.draw(sym);
	}

}
