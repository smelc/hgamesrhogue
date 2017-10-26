package com.hgames.rhogue.generation.monsters.generator;

import java.util.Collection;

import com.hgames.rhogue.animate.IAnimate;
import com.hgames.rhogue.generation.monsters.group.IMonstersFactory;
import com.hgames.rhogue.generation.monsters.group.IMonstersGroupGenerator;

import squidpony.squidmath.RNG;

/**
 * An implementation of {@link IMonstersGenerator}.
 * 
 * @author smelC
 * @param <U>
 *            Identifiers of monsters
 * @param <T>
 *            The concrete type of {@link IAnimate}s used.
 */
public class MonstersGenerator<U, T extends IAnimate> implements IMonstersGenerator<U, T> {

	protected final /* @Nullable */ IMonstersGroupGenerator<U, T> must;
	protected final IMonstersGroupGenerator<U, T> default_;

	/**
	 * @param must
	 *            A generator that is used exactly once at every call to
	 *            {@link #generate} (disregarding the size objective), or null. If
	 *            you want to ensure that some monsters are always there, you should
	 *            put them in this generator.
	 * @param default_
	 *            The generator used to honor the size, which can be called
	 *            repeatedly. Cannot be null. It's the "base" generator in the sense
	 *            that it's used to populate the levels.
	 */
	public MonstersGenerator(/* @Nullable */ IMonstersGroupGenerator<U, T> must, IMonstersGroupGenerator<U, T> default_) {
		this.must = must;
		this.default_ = default_;
	}

	@Override
	public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc, int size) {
		int addeds = 0;
		if (must != null) {
			final int before = acc.size();
			must.generate(factory, rng, acc);
			addeds += acc.size() - before;
		}
		while (addeds < size) {
			final int before = acc.size();
			default_.generate(factory, rng, acc);
			addeds += acc.size() - before;
		}
	}

}
