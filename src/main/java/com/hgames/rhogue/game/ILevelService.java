package com.hgames.rhogue.game;

import com.hgames.rhogue.level.ILevel;

import squidpony.squidmath.Coord;

/**
 * Services provided by {@link IGame} about {@link ILevel}s.
 * 
 * @author smelC
 * 
 * @param <L>
 *            The concrete type of levels.
 */
public interface ILevelService<L extends ILevel<?>> {

	/**
	 * @param source
	 * @param exit
	 *            An exit of {@code source}, i.e. a member of
	 *            {@link ILevel#getExits()}.
	 * @param createIfMissing
	 *            Whether the result must be created if it hasn't been created
	 *            yet.
	 * @return The level to which to go when going through {@code exit} in
	 *         {@code source} or null if none.
	 */
	public /* @Nullable */ L getDestination(L source, Coord exit, boolean createIfMissing);

	/**
	 * @return The current level, if any.
	 */
	public /* @Nullable */ L getCurrentLevel();

}
