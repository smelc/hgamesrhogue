package com.hgames.rhogue.generation.monsters.group;

import java.util.Collection;
import java.util.List;

import com.hgames.rhogue.animate.IAnimate;
import com.hgames.rhogue.generation.monsters.generator.IMonstersGenerator;
import com.hgames.rhogue.rng.ProbabilityTable;

import squidpony.squidmath.RNG;

/**
 * How to create a group of monsters, without specifying a size. All
 * implementations you need are static classes in this file. See
 * {@link MGGBuilder} for convenience methods.
 * 
 * <p>
 * The implementation of {@link IMonstersGenerator} is a convenience, to save
 * allocations. You can ignore it if you don't really know what you're doing.
 * </p>
 * 
 * @author smelC
 * @param <U>
 *            Identifiers of monsters
 * @param <T>
 *            The concrete type of {@link IAnimate}s used.
 * @see IMonstersGenerator Generation of monsters, that should rely on this
 *      interface; when aiming for a given number of monsters.
 */
public interface IMonstersGroupGenerator<U, T extends IAnimate> extends IMonstersGenerator<U, T> {

	/**
	 * @param factory
	 * @param rng
	 *            The {@link RNG} to use.
	 * @param acc
	 *            Where to record created monsters.
	 */
	public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc);

	/**
	 * @param minOrMax
	 * @return The minimum or maximum number of monsters that {@code this} may add
	 *         to the accumulator during a call to {@link #generate}.
	 */
	public int size(boolean minOrMax);

	/**
	 * Fulfill the implementation of {@link IMonstersGenerator}, ignoring its
	 * {@code size} argument. For convenience, and to save allocations.
	 * 
	 * @author smelC
	 * @param <U>
	 *            Identifiers of monsters
	 * @param <T>
	 *            The concrete type of {@link IAnimate}s used.
	 */
	static abstract class SkeletalMGG<U, T extends IAnimate> implements IMonstersGroupGenerator<U, T> {

		@Override
		public final void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc, int size) {
			// Delegate to MonstersGroupGenerator API
			generate(factory, rng, acc);
		}

	}

	/**
	 * A generator that picks a random delegate every time it is called.
	 * 
	 * @author smelC
	 * @param <U>
	 * @param <T>
	 */
	public static class Or<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		protected final ProbabilityTable<IMonstersGroupGenerator<U, T>> table;

		/**
		 * @param table
		 *            The delegate to use, with their probability.
		 */
		public Or(ProbabilityTable<IMonstersGroupGenerator<U, T>> table) {
			this.table = table;
		}

		@SuppressWarnings("javadoc")
		public static <U, T extends IAnimate> Or<U, T> create(ProbabilityTable<IMonstersGroupGenerator<U, T>> table) {
			return new Or<U, T>(table);
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc) {
			final IMonstersGroupGenerator<U, T> delegate = table.get(rng);
			if (delegate == null)
				/* Table is empty */
				return;
			delegate.generate(null, null, acc);
		}

		@Override
		public int size(boolean minOrMax) {
			int result = minOrMax ? Integer.MAX_VALUE : 0;
			for (IMonstersGroupGenerator<U, T> t : table.getDomain()) {
				final int val = t.size(minOrMax);
				if (minOrMax && val < result)
					result = val;
				else if (!minOrMax && result < val)
					result = val;
			}
			return result;
		}

	}

	/**
	 * A generator that use a list of delegates every time it is called.
	 * 
	 * @author smelC
	 * @param <U>
	 * @param <T>
	 */
	public static class And<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		protected final List<IMonstersGroupGenerator<U, T>> delegates;

		/**
		 * @param delegates
		 */
		public And(List<IMonstersGroupGenerator<U, T>> delegates) {
			if (delegates == null)
				throw new NullPointerException();
			this.delegates = delegates;
		}

		@SuppressWarnings("javadoc")
		public static <U, T extends IAnimate> And<U, T> create(List<IMonstersGroupGenerator<U, T>> delegates) {
			return new And<U, T>(delegates);
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc) {
			final int size = delegates.size();
			for (int i = 0; i < size; i++)
				delegates.get(i).generate(null, null, acc);
		}

		@Override
		public int size(boolean minOrMax) {
			final int size = delegates.size();
			int result = 0;
			for (int i = 0; i < size; i++)
				result += delegates.get(i).size(minOrMax);
			return result;
		}

	}

	/**
	 * A group generator that generates a single monster.
	 * 
	 * @author smelC
	 * @param <U>
	 *            The type of identifiers of monsters.
	 * @param <T>
	 *            The concrete type of monsters.
	 */
	public class Single<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		protected final U identifier;

		/**
		 * A fresh instance.
		 * 
		 * @param identifier
		 *            The monster to build.
		 */
		public Single(U identifier) {
			this.identifier = identifier;
		}

		@SuppressWarnings("javadoc")
		public static <U, T extends IAnimate> IMonstersGroupGenerator<U, T> create(U identifier) {
			return new Single<U, T>(identifier);
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc) {
			acc.add(factory.create(identifier));
		}

		@Override
		public int size(boolean minOrMax) {
			return 1;
		}

	}

	/**
	 * A group generator that generates always the same (but fresh every time!)
	 * members.
	 * 
	 * @author smelC
	 * @param <U>
	 *            The type of identifiers of monsters.
	 * @param <T>
	 *            The concrete type of monsters.
	 */
	public class ListMG<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		private final List<U> identifiers;

		/**
		 * @param identifiers
		 */
		public ListMG(List<U> identifiers) {
			this.identifiers = identifiers;
		}

		@SuppressWarnings("javadoc")
		public static <U, T extends IAnimate> ListMG<U, T> create(List<U> identifiers) {
			return new ListMG<U, T>(identifiers);
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc) {
			final int sz = identifiers.size();
			for (int i = 0; i < sz; i++)
				acc.add(factory.create(identifiers.get(i)));
		}

		@Override
		public int size(boolean minOrMax) {
			return identifiers.size();
		}

	}

	/**
	 * A group generator that generates always a list concerning a group of monsters
	 * of the same kind, with a varying length.
	 * 
	 * @author smelC
	 * @param <U>
	 *            The type of identifiers of monsters.
	 * @param <T>
	 *            The concrete type of monsters.
	 */
	public class RandomLengthUniformList<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		private final U identifier;
		private final int min;
		private final int max;

		/**
		 * @param identifier
		 * @param min
		 *            The minimum size of generated lists (inclusive).
		 * @param max
		 *            The maximum size of generated lists (inclusive).
		 */
		public RandomLengthUniformList(U identifier, int min, int max) {
			this.identifier = identifier;
			this.min = min;
			this.max = max;
			if (max < min)
				throw new IllegalStateException(
						"Min should be less or equal than max but received " + min + " and " + max);
		}

		@SuppressWarnings("javadoc")
		public static <U, T extends IAnimate> RandomLengthUniformList<U, T> create(U identifier, int min, int max) {
			return new RandomLengthUniformList<U, T>(identifier, min, max);
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, RNG rng, Collection<T> acc) {
			final int sz = rng.between(min, max + 1); // +1 because 'between' is exclusive for the max
			for (int i = 0; i < sz; i++)
				acc.add(factory.create(identifier));
		}

		@Override
		public int size(boolean minOrMax) {
			return minOrMax ? min : max;
		}

	}

}
