package com.hgames.rhogue.grid;

import com.hgames.rhogue.level.ILevel;

/**
 * A cell in a map. Often heavily game dependent. There's not much in there, but
 * it doesn't matter, because {@link ILevel} gives the concrete type of cells.
 * 
 * @author smelC
 */
public interface IMapCell {

	/**
	 * @return {@code true} if this cell has been seen by a player.
	 */
	public boolean seenByPlayer();

}
