package com.hgames.rhogue.ai;

import com.hgames.rhogue.animate.IAnimate;

/**
 * The """artificial intelligence""" of a monster/ally of players.
 * 
 * @author smelC
 * 
 * @param <T>
 *            The concrete instance of {@link IAnimate} being used.
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

	/**
	 * @param other
	 * @return If {@code other} should not be hunted by {@link #getAnimate()}.
	 */
	public boolean isInvalidHuntingTarget(/* @Nullable */ T other);

}
