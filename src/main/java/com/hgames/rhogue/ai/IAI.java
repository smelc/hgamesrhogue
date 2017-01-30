package com.hgames.rhogue.ai;

import com.hgames.rhogue.animate.IAnimate;

/**
 * The """artificial intelligence""" of a monster/ally of players.
 * 
 * @author smelC
 */
public interface IAI<T extends IAnimate> {

	/**
	 * @return The animate to which this AI applies.
	 */
	public T getAnimate();

	/**
	 * @param one
	 * @param two
	 * @return Whether {@code one} has a Line of Sight on {@code two}.
	 */
	public boolean hasLOS(T one, T two);

	public boolean isInvalidHuntingTarget(/* @Nullable */ T other);

}
