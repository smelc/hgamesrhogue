package com.hgames.rhogue.generation.map;

/**
 * @author smelC
 */
public interface IDungeonDrawer {

	/**
	 * @param dungeon
	 *            The dungeon to draw. Symbols can be null, in which case they
	 *            should be considered as walls.
	 */
	public void draw(DungeonSymbol[][] dungeon);

}
