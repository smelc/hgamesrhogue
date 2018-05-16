package com.hgames.rhogue.game;

import java.util.List;

import com.hgames.rhogue.level.ILevel;

/**
 * Services provided by {@link IGame} about {@link ILevel}s.
 * 
 * @author smelC
 * 
 * @param <L>
 *            The concrete type of levels.
 */
public interface ILevelService<L extends ILevel<?, ?>> {

	/**
	 * @return The current level, if any.
	 */
	public /* @Nullable */ L getCurrentLevel();

	/**
	 * @param source
	 * @param acc
	 *            Where to add the result, or null to allocate a fresh list.
	 * @param beingScheduled
	 *            Whether only scheduled levels should be returned
	 * @return The levels that are connected to {@code source}. {@code acc} is
	 *         returned if non-null (otherwise a fresh list is returned).
	 */
	public List<L> getDestinations(L source, /* @Nullable */ List<L> acc, boolean beingScheduled);

}
