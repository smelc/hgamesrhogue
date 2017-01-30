package com.hgames.rhogue.ai;

import com.hgames.rhogue.animate.IAnimate;

/**
 * The """artificial intelligence""" of a monster/ally of players.
 * 
 * @author smelC
 */
public abstract class AbstractAI<T extends IAnimate> {

	/** The animate whose AI it is */
	protected final T animate;

	public AbstractAI(T animate) {
		this.animate = animate;
	}

	/**
	 * @return The animate to which this AI applies.
	 */
	public T getAnimate() {
		return animate;
	}

	/**
	 * @param one
	 * @param two
	 * @return Whether {@code one} has a Line of Sight on {@code two}.
	 */
	protected abstract boolean hasLOS(T one, T two);

	protected boolean isInvalidHuntingTarget(T other) {
		return other == null || other.isDead() || !other.getTeam().adversaries(animate.getTeam());
	}

}
