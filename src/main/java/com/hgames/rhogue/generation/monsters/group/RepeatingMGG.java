package com.hgames.rhogue.generation.monsters.group;

import java.util.Collection;

import com.hgames.rhogue.animate.IAnimate;

import squidpony.squidmath.RNG;

/**
 * A forwarder that calls the specified amount of times its delegate.
 * 
 * @author smelC
 * @param <U>
 *            Identifiers of monsters
 * @param <T>
 *            The concrete type of {@link IAnimate}s used.
 */
public class RepeatingMGG<U, T extends IAnimate> implements IMonstersGroupGenerator<U, T> {

	protected final IMonstersGroupGenerator<U, T> delegate;
	protected final int lower;
	protected final int higher;

	/**
	 * @param delegate
	 *            The generator delegated to
	 * @param lower
	 *            The minimum number of times to call the delegate
	 * @param higher
	 *            The minimum number of times to call the delegate
	 */
	public RepeatingMGG(IMonstersGroupGenerator<U, T> delegate, int lower, int higher) {
		this.delegate = delegate;
		this.lower = lower;
		this.higher = higher;
	}

	@Override
	public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc, int size) {
		generate(factory, rng, acc);
	}

	@Override
	public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc) {
		final int between = rng.between(lower, higher + 1);
		for (int i = 0; i < between; i++)
			delegate.generate(factory, rng, acc);
	}

	@Override
	public void addMay(Collection<U> acc) {
		delegate.addMay(acc);
	}

	@Override
	public boolean may(U u) {
		return delegate.may(u);
	}

}
