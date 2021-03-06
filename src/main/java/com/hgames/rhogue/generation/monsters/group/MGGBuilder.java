package com.hgames.rhogue.generation.monsters.group;

import java.util.ArrayList;
import java.util.List;

import com.hgames.lib.collection.multiset.EnumMultiset;
import com.hgames.rhogue.animate.IAnimate;
import com.hgames.rhogue.rng.ProbabilityTable;

/**
 * Convenience methods for building instances of
 * {@link IMonstersGroupGenerator}.
 * 
 * @author smelC
 */
public class MGGBuilder {

	/**
	 * @param mg1
	 * @param mg2
	 * @return The uniform conjunction of {@code mg1} and {@code mg2}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> and(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2) {
		final List<IMonstersGroupGenerator<U, T>> list = new ArrayList<IMonstersGroupGenerator<U, T>>(2);
		list.add(mg1);
		list.add(mg2);
		return IMonstersGroupGenerator.And.create(list);
	}

	/**
	 * @param mg1
	 * @param mg2
	 * @param mg3
	 * @return The uniform conjunction of {@code mg1}, {@code mg2}, and {@code mg3}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> and(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2, IMonstersGroupGenerator<U, T> mg3) {
		final List<IMonstersGroupGenerator<U, T>> list = new ArrayList<IMonstersGroupGenerator<U, T>>(3);
		list.add(mg1);
		list.add(mg2);
		list.add(mg3);
		return IMonstersGroupGenerator.And.create(list);
	}

	/**
	 * @param mg1
	 * @param mg2
	 * @param mg3
	 * @param mg4
	 * @return The uniform disjunction of {@code mg1}, {@code mg2}, {@code mg3}, and
	 *         {@code mg4},
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> and(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2, IMonstersGroupGenerator<U, T> mg3, IMonstersGroupGenerator<U, T> mg4) {
		final List<IMonstersGroupGenerator<U, T>> list = new ArrayList<IMonstersGroupGenerator<U, T>>(4);
		list.add(mg1);
		list.add(mg2);
		list.add(mg3);
		list.add(mg4);
		return IMonstersGroupGenerator.And.create(list);
	}

	@SuppressWarnings("javadoc")
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> and(List<IMonstersGroupGenerator<U, T>> mgs) {
		return IMonstersGroupGenerator.And.create(mgs);
	}

	/**
	 * @param base
	 * @param maybe
	 * @param probability
	 * @return A generator that always delegates to {@code base} and with some
	 *         probability to {@code maybe}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> andMaybe(IMonstersGroupGenerator<U, T> base,
			IMonstersGroupGenerator<U, T> maybe, int probability) {
		return new IMonstersGroupGenerator.AndMaybe<U, T>(base, maybe, probability);
	}

	/**
	 * @param idents
	 * @return A generator that creates all monsters in {@code idents}.
	 */
	public static <U extends Enum<U>, T extends IAnimate> IMonstersGroupGenerator<U, T> list(EnumMultiset<U> idents) {
		final List<U> list = new ArrayList<U>(idents.size());
		for (U ident : idents.keySet()) {
			final int count = idents.count(ident);
			assert 0 < count;
			for (int i = 0; i < count; i++)
				list.add(ident);
		}
		return IMonstersGroupGenerator.ListMG.create(list);
	}

	/**
	 * @param ident
	 * @param min
	 * @param max
	 * @return A generator that creates lists of {@code ident} of length in [min,
	 *         max].
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> list(U ident, int min, int max) {
		return IMonstersGroupGenerator.RandomLengthUniformList.create(ident, min, max);
	}

	/**
	 * @param maybe
	 * @param probability
	 * @return A generator that delegates to {@code maybe} with some probability, or
	 *         does nothing.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> maybe(IMonstersGroupGenerator<U, T> maybe,
			int probability) {
		return new IMonstersGroupGenerator.Maybe<U, T>(maybe, probability);
	}

	/**
	 * @param mg1
	 * @param mg2
	 * @return The uniform disjunction of {@code mg1} and {@code mg2}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator.Or<U, T> or(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2) {
		final List<IMonstersGroupGenerator<U, T>> list = new ArrayList<IMonstersGroupGenerator<U, T>>(2);
		list.add(mg1);
		list.add(mg2);
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.createUniform(list));
	}

	/**
	 * @param mg1
	 * @param mg2
	 * @param mg3
	 * @return The uniform disjunction of {@code mg1}, {@code mg2}, and {@code mg3}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator.Or<U, T> or(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2, IMonstersGroupGenerator<U, T> mg3) {
		final List<IMonstersGroupGenerator<U, T>> list = new ArrayList<IMonstersGroupGenerator<U, T>>(3);
		list.add(mg1);
		list.add(mg2);
		list.add(mg3);
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.createUniform(list));
	}

	/**
	 * @param mg1
	 * @param mg2
	 * @param mg3
	 * @param mg4
	 * @return The uniform disjunction of {@code mg1}, {@code mg2}, {@code mg3}, and
	 *         {@code mg4}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator.Or<U, T> or(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2, IMonstersGroupGenerator<U, T> mg3, IMonstersGroupGenerator<U, T> mg4) {
		final List<IMonstersGroupGenerator<U, T>> list = new ArrayList<IMonstersGroupGenerator<U, T>>(4);
		list.add(mg1);
		list.add(mg2);
		list.add(mg3);
		list.add(mg4);
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.createUniform(list));
	}

	/**
	 * @param mg1
	 * @param mg2
	 * @param mg3
	 * @param mg4
	 * @param mg5
	 * @return The uniform disjunction of {@code mg1}, {@code mg2}, {@code mg3},
	 *         {@code mg4}, and {@code mg5}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator.Or<U, T> or(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2, IMonstersGroupGenerator<U, T> mg3, IMonstersGroupGenerator<U, T> mg4,
			IMonstersGroupGenerator<U, T> mg5) {
		final List<IMonstersGroupGenerator<U, T>> list = new ArrayList<IMonstersGroupGenerator<U, T>>(4);
		list.add(mg1);
		list.add(mg2);
		list.add(mg3);
		list.add(mg4);
		list.add(mg5);
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.createUniform(list));
	}

	@SuppressWarnings("javadoc")
	public static <U, T extends IAnimate> IMonstersGroupGenerator.Or<U, T> or(
			ProbabilityTable<IMonstersGroupGenerator<U, T>> mgs) {
		return IMonstersGroupGenerator.Or.create(mgs);
	}

	/**
	 * @param mgs
	 * @return The uniform disjunction of {@code mgs}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator.Or<U, T> or(IMonstersGroupGenerator<U, T>[] mgs) {
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.createUniform(mgs));
	}

	/**
	 * @param mgs
	 * @return The uniform disjunction of {@code mgs}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator.Or<U, T> or(List<IMonstersGroupGenerator<U, T>> mgs) {
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.createUniform(mgs));
	}

	/**
	 * @param delegate
	 * @param lower
	 *            The minimum number of times to call {@code delegate}
	 * @param higher
	 *            The maximum number of times to call {@code delegate}
	 * @return A generator that calls {@code delegate} between lower and higher
	 *         times.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> repeater(IMonstersGroupGenerator<U, T> delegate,
			int lower, int higher) {
		return new RepeatingMGG<U, T>(delegate, lower, higher);
	}

	/**
	 * @param mg1
	 * @param mg2
	 * @return A roulette using {@code mg1} as the base and {@code mg2} for spicing.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> roulette(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2) {
		return new IMonstersGroupGenerator.Roulette<U, T>(mg1, mg2);
	}

	/**
	 * @param mg1
	 * @param mg2
	 * @return A roulette using {@code mg1} as the base and {@code mg2} for spicing.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> roulette3(IMonstersGroupGenerator<U, T> mg1,
			IMonstersGroupGenerator<U, T> mg2) {
		return new IMonstersGroupGenerator.Roulette<U, T>(mg1, mg2, 3);
	}

	/**
	 * @param identifier
	 * @return Generator that generates a single instance of {@code identifier}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> single(U identifier) {
		return IMonstersGroupGenerator.Single.create(identifier);
	}

	/**
	 * @param mg1
	 * @param w1
	 *            {@code mg1}'s weight
	 * @param mg2
	 * @param w2
	 *            {@code mg2}'s weight
	 * @return The weighted disjunction of {@code mg1} and {@code mg2}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> weightedOr(IMonstersGroupGenerator<U, T> mg1,
			int w1, IMonstersGroupGenerator<U, T> mg2, int w2) {
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.create(mg1, w1, mg2, w2));
	}

	/**
	 * @param mg1
	 * @param w1
	 *            {@code mg1}'s weight
	 * @param mg2
	 * @param w2
	 *            {@code mg2}'s weight
	 * @param mg3
	 * @param w3
	 *            {@code mg3}'s weight
	 * @return The weighted disjunction of {@code mg1}, {@code mg2}, and
	 *         {@code mg3}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> weightedOr(IMonstersGroupGenerator<U, T> mg1,
			int w1, IMonstersGroupGenerator<U, T> mg2, int w2, IMonstersGroupGenerator<U, T> mg3, int w3) {
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.create(mg1, w1, mg2, w2, mg3, w3));
	}

	/**
	 * @param mg1
	 * @param w1
	 *            {@code mg1}'s weight
	 * @param mg2
	 * @param w2
	 *            {@code mg2}'s weight
	 * @param mg3
	 * @param w3
	 *            {@code mg3}'s weight
	 * @param mg4
	 * @param w4
	 *            {@code mg4}'s weight
	 * @return The weighted disjunction of {@code mg1}, {@code mg2}, {@code mg3},
	 *         and {@code mg4}.
	 */
	public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> weightedOr(IMonstersGroupGenerator<U, T> mg1,
			int w1, IMonstersGroupGenerator<U, T> mg2, int w2, IMonstersGroupGenerator<U, T> mg3, int w3,
			IMonstersGroupGenerator<U, T> mg4, int w4) {
		return IMonstersGroupGenerator.Or.create(ProbabilityTable.create(mg1, w1, mg2, w2, mg3, w3, mg4, w4));
	}
}
