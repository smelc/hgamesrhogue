package com.hgames.rhogue.level;

import java.io.Serializable;

/**
 * @author smelC
 */
public interface ILevelIdentifier extends Serializable {

	/**
	 * This method's prototype is temporary, while I generalize Dungeon Mercenary.
	 * Later it'll return a list.
	 * 
	 * @return The previous level's identifier, if any.
	 */
	public /* @Nullable */ ILevelIdentifier getPrevious();

	/**
	 * This method's prototype is temporary, while I generalize Dungeon Mercenary.
	 * Later it'll return a list.
	 * 
	 * @return The next level's identifier, if any.
	 */
	public /* @Nullable */ ILevelIdentifier getNext();

	/**
	 * @return Whether this is the first level
	 */
	public boolean isFirst();

	@Override
	public boolean equals(Object other);

	@Override
	public int hashCode();

}
