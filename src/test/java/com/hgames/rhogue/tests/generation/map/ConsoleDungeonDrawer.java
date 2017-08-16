package com.hgames.rhogue.tests.generation.map;

import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.generation.map.IDungeonDrawer;
import com.hgames.rhogue.generation.map.IDungeonSymbolDrawer;

/**
 * An implementation of {@link IDungeonDrawer} that draws to {@code System.out}.
 * 
 * @author smelC
 */
public class ConsoleDungeonDrawer implements IDungeonDrawer {

	protected int nbDrawn = 0;
	protected final IDungeonSymbolDrawer symbolDrawer;

	ConsoleDungeonDrawer(IDungeonSymbolDrawer symbolDrawer) {
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
				System.out.print(symbolDrawer.draw(sym));
			}
			System.out.println("");
		}
		nbDrawn++;
	}

}
