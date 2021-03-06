package com.hgames.rhogue.generation.monsters.generator;

import java.util.Collection;
import java.util.List;

import com.hgames.lib.collection.list.Lists;
import com.hgames.rhogue.animate.IAnimate;
import com.hgames.rhogue.generation.monsters.group.IMonstersFactory;

import squidpony.squidmath.IRNG;

/**
 * A forwarder to another {@link IMonstersGenerator}, typically to postprocess
 * its result. Think about redefining {@link #may(Object)} if you delete
 * monsters in {@link #intercept(List)}.
 * 
 * @author smelC
 * @param <U>
 *            Identifiers of monsters
 * @param <T>
 *            The concrete type of {@link IAnimate}s used.
 */
public abstract class DelegatingMG<U, T extends IAnimate> implements IMonstersGenerator<U, T> {

	protected final IMonstersGenerator<U, T> delegate;

	/**
	 * @param delegate
	 *            The generator to delegate to.
	 */
	public DelegatingMG(IMonstersGenerator<U, T> delegate) {
		this.delegate = delegate;
	}

	@Override
	public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc, int size) {
		final List<T> interceptor = Lists.newArrayList();
		delegate.generate(factory, rng, interceptor, size);
		intercept(interceptor);
		acc.addAll(interceptor);
	}

	@Override
	public void addMay(Collection<U> acc) {
		delegate.addMay(acc);
	}

	@Override
	public boolean may(U u) {
		return delegate.may(u);
	}

	/**
	 * @param generateds
	 *            Monsters generated by {@link #delegate}.
	 */
	protected abstract void intercept(List<T> generateds);
}
