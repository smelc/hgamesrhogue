package com.hgames.rhogue.generation.monsters.generator;

import java.util.Collection;

import com.hgames.rhogue.animate.IAnimate;
import com.hgames.rhogue.generation.monsters.group.IMonstersFactory;
import com.hgames.rhogue.generation.monsters.group.IMonstersGroupGenerator;

import squidpony.squidmath.RNG;

/**
 * How to generate monsters in a level, when targeting a given number of
 * monsters.
 * 
 * @author smelC
 * @param <U>
 *            Identifiers of monsters
 * @param <T>
 *            The concrete type of {@link IAnimate}.
 * @see IMonstersGroupGenerator Generation of monsters without aiming for some
 *      size.
 */
public interface IMonstersGenerator<U, T extends IAnimate> {

	/**
	 * @param factory
	 *            How to create a single monster.
	 * @param rng
	 * @param acc
	 *            Where to records created monsters.
	 * @param size
	 *            The number of monsters to create. May not be honored exactly: it's
	 *            a hint.
	 */
	public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc, int size);

}
