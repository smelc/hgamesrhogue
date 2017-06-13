package com.hgames.rhogue.ai;

import com.hgames.rhogue.animate.IAnimate;

/**
 * A skeletal implementation of {@link IAI}.
 * 
 * @author smelC
 * @param <T>
 */
public abstract class SkeletalAI<T extends IAnimate> implements IAI<T> {

	/** The animate whose AI it is */
	protected final T animate;

	/**
	 * A fresh AI instance for {@code animate}.
	 * 
	 * @param animate
	 */
	public SkeletalAI(T animate) {
		this.animate = animate;
	}

	@Override
	public T getAnimate() {
		return animate;
	}

	@Override
	public boolean isInvalidHuntingTarget(/* @Nullable */ T other) {
		return other == null || other.isDead() || !other.getTeam().adversaries(animate.getTeam());
	}

}
