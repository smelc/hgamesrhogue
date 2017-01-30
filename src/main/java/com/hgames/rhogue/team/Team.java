package com.hgames.rhogue.team;

import com.hgames.rhogue.animate.IAnimate;

/**
 * The team of an {@link IAnimate}. In
 * <a href="http://www.schplaf.org/hgames/">Dungeon Mercenary</a>, it is
 * implemented by an enumeration.
 * 
 * @author smelC
 */
public interface Team {

	/**
	 * @param t
	 * @return {@code true} if {@code this} is an enemy of {@code t}.
	 */
	public boolean adversaries(Team t);

	/**
	 * @param t
	 * @param considerItselfAsAlly
	 *            Whether {@code true} must be returned if {@code this} and
	 *            {@code other} designates the same team.
	 * @return {@code true} if {@code this} is an ally of {@code t}.
	 */
	public boolean allies(Team t, boolean considerItselfAsAlly);

}
