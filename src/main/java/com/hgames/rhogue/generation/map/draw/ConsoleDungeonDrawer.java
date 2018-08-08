package com.hgames.rhogue.generation.map.draw;

import com.hgames.rhogue.generation.map.dungeon.DungeonSymbol;

/**
 * An implementation of {@link IDungeonDrawer} that draws to {@code System.out}.
 * 
 * @author smelC
 */
public class ConsoleDungeonDrawer implements IDungeonDrawer {

	protected int nbDrawn = 0;
	protected final IDungeonSymbolDrawer symbolDrawer;

	/**
	 * @param symbolDrawer
	 */
	public ConsoleDungeonDrawer(IDungeonSymbolDrawer symbolDrawer) {
		this.symbolDrawer = symbolDrawer;
	}

	@Override
	public void draw(DungeonSymbol[][] dungeon) {
		final int width = dungeon.length;
		if (0 < nbDrawn) {
			for (int i = 0; i < width; i++)
				System.out.print("*");
			System.out.println("");
		}
		final int height = width == 0 ? 0 : dungeon[0].length;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				final DungeonSymbol sym = dungeon[x][y];
				System.out.print(print(sym, x, y));
			}
			System.out.println("");
		}
		nbDrawn++;
	}

	/* Clients may override */
	@SuppressWarnings("unused")
	protected char print(DungeonSymbol sym, int x, int y) {
		return symbolDrawer.draw(sym);
	}
}
