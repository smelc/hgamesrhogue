package com.hgames.rhogue.generation.monsters.group;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hgames.rhogue.animate.IAnimate;
import com.hgames.rhogue.generation.monsters.generator.IMonstersGenerator;
import com.hgames.rhogue.rng.ProbabilityTable;

import squidpony.squidmath.IRNG;

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
	 *            The {@link IRNG} to use.
	 * @param acc
	 *            Where to record created monsters.
	 */
	public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc);

	@Override
	public boolean may(U u);

	@Override
	public void addMay(Collection<U> acc);

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
		public final void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc, int size) {
			// Delegate to MonstersGroupGenerator API
			generate(factory, rng, acc);
		}

	}

	/**
	 * A generator that picks a random delegate every time it is called. It offers
	 * the option to remove a delegate after its first usage.
	 * 
	 * @author smelC
	 * @param <U>
	 * @param <T>
	 */
	public static class Or<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		protected final ProbabilityTable<IMonstersGroupGenerator<U, T>> table;

		/**
		 * Members of {@link #table} that should be removed after having been used once.
		 */
		protected /* @Nullable */ Set<IMonstersGroupGenerator<U, T>> oneShots;

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

		/**
		 * @param extension
		 * @param proba
		 * @return A variant of {@code this} where {@code t} with probability
		 *         {@code proba} has been added. The instances of {@code U} get shared
		 *         between {@code this} and the result.
		 */
		public IMonstersGroupGenerator<U, T> disjunct(IMonstersGroupGenerator<U, T> extension, int proba) {
			final IMonstersGroupGenerator.Or<U, T> result = new Or<U, T>(
					new ProbabilityTable<IMonstersGroupGenerator<U, T>>(table));
			result.table.add(extension, proba);
			if (oneShots != null)
				result.oneShots = new HashSet<IMonstersGroupGenerator<U, T>>(oneShots);
			return result;
		}

		/**
		 * @param delegate
		 *            A member of the underlying table.
		 */
		public void markOneShot(IMonstersGroupGenerator<U, T> delegate) {
			if (!table.getDomain().contains(delegate)) {
				assert false;
				System.err.println("Generator (" + delegate + ") should be a delegate to be marked one shot!");
				return;
			}
			if (oneShots == null)
				oneShots = new HashSet<IMonstersGroupGenerator<U, T>>();
			oneShots.add(delegate);
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc) {
			final IMonstersGroupGenerator<U, T> delegate = table.get(rng);
			if (delegate == null)
				/* Table is empty */
				return;
			delegate.generate(factory, rng, acc);
			if (oneShots != null) {
				if (oneShots.contains(delegate)) {
					/* A one shotter, delete it */
					table.remove(delegate);
					final boolean rmed = oneShots.remove(delegate);
					assert rmed;
					if (oneShots.isEmpty())
						/* Free memory */
						oneShots = null;
				}
			}
		}

		@Override
		public boolean may(U u) {
			for (IMonstersGroupGenerator<U, T> sub : table.getDomain()) {
				if (sub.may(u))
					return true;
			}
			return false;
		}

		@Override
		public void addMay(Collection<U> acc) {
			for (IMonstersGroupGenerator<U, T> sub : table.getDomain())
				sub.addMay(acc);
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
		public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc) {
			final int size = delegates.size();
			for (int i = 0; i < size; i++)
				delegates.get(i).generate(factory, rng, acc);
		}

		@Override
		public void addMay(Collection<U> acc) {
			final int size = delegates.size();
			for (int i = 0; i < size; i++) {
				delegates.get(i).addMay(acc);
			}
		}

		@Override
		public boolean may(U u) {
			final int size = delegates.size();
			for (int i = 0; i < size; i++) {
				if (delegates.get(i).may(u))
					return true;
			}
			return false;
		}

	}

	/**
	 * A generator that delegates to a generator, and maybe to another one.
	 * 
	 * @author smelC
	 * @param <U>
	 * @param <T>
	 */
	public static class AndMaybe<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		protected final IMonstersGroupGenerator<U, T> base;
		protected final IMonstersGroupGenerator<U, T> maybe;
		protected final int maybeProbability;

		/**
		 * @param base
		 *            The generator to which this generator always delegates.
		 * @param maybe
		 *            The generator to which this generator may delegate.
		 * @param maybeProbability
		 *            The probability with which to delegate to {@code maybe}. 0 means
		 *            never, 1 means 50%, etc.
		 */
		public AndMaybe(IMonstersGroupGenerator<U, T> base, IMonstersGroupGenerator<U, T> maybe, int maybeProbability) {
			this.base = base;
			this.maybe = maybe;
			this.maybeProbability = maybeProbability;
		}

		/**
		 * @param base
		 *            The generator to which this generator always delegates.
		 * @param maybe
		 *            The generator to which this generator may delegate.
		 * @param maybeProbability
		 *            The probability with which to delegate to {@code maybe}. 0 means
		 *            never, 1 means 50%, etc.
		 * @return A generator combining {@code base} and {@code maybe}.
		 */
		public static <U, T extends IAnimate> AndMaybe<U, T> create(IMonstersGroupGenerator<U, T> base,
				IMonstersGroupGenerator<U, T> maybe, int maybeProbability) {
			return new AndMaybe<U, T>(base, maybe, maybeProbability);
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc) {
			base.generate(factory, rng, acc);
			if (0 < maybeProbability && rng.nextInt(maybeProbability) == 0)
				maybe.generate(factory, rng, acc);
		}

		@Override
		public void addMay(Collection<U> acc) {
			base.addMay(acc);
			maybe.addMay(acc);
		}

		@Override
		public boolean may(U u) {
			return base.may(u) || maybe.may(u);
		}

	}

	/**
	 * A generator that may delegate to a generator or do nothing.
	 * 
	 * @author smelC
	 * @param <U>
	 * @param <T>
	 */
	public static class Maybe<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		protected final IMonstersGroupGenerator<U, T> maybe;
		protected final int probability;

		/**
		 * @param maybe
		 *            The generator to which this generator may delegate.
		 * @param proba
		 *            The probability with which to delegate to {@code maybe}. 0 means
		 *            never, 1 means 50%, etc.
		 */
		public Maybe(IMonstersGroupGenerator<U, T> maybe, int proba) {
			this.maybe = maybe;
			this.probability = proba;
		}

		/**
		 * @param maybe
		 *            The generator to which this generator may delegate.
		 * @param probability
		 *            The probability with which to delegate to {@code maybe}. 0 means
		 *            never, 1 means 50%, etc.
		 * @return A generator that may do something, using {@code maybe}.
		 */
		public static <U, T extends IAnimate> Maybe<U, T> create(IMonstersGroupGenerator<U, T> maybe, int probability) {
			return new Maybe<U, T>(maybe, probability);
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc) {
			if (0 < probability && rng.nextInt(probability) == 0)
				maybe.generate(factory, rng, acc);
		}

		@Override
		public void addMay(Collection<U> acc) {
			if (0 < probability)
				maybe.addMay(acc);
		}

		@Override
		public boolean may(U u) {
			return 0 < probability && maybe.may(u);
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
		public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc) {
			acc.add(factory.create(identifier));
		}

		@Override
		public void addMay(Collection<U> acc) {
			acc.add(identifier);
		}

		@Override
		public boolean may(U u) {
			return identifier.equals(u);
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
		public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc) {
			final int sz = identifiers.size();
			for (int i = 0; i < sz; i++)
				acc.add(factory.create(identifiers.get(i)));
		}

		@Override
		public void addMay(Collection<U> acc) {
			final int sz = identifiers.size();
			for (int i = 0; i < sz; i++)
				acc.add(identifiers.get(i));
		}

		@Override
		public boolean may(U u) {
			return identifiers.contains(u);
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
		public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc) {
			final int sz = rng.between(min, max + 1); // +1 because 'between' is exclusive for the max
			for (int i = 0; i < sz; i++)
				acc.add(factory.create(identifier));
		}

		@Override
		public void addMay(Collection<U> acc) {
			acc.add(identifier);
		}

		@Override
		public boolean may(U u) {
			return identifier.equals(u);
		}
	}

	/**
	 * A generator that most of the time delegates to another generator (the base
	 * one) and sometimes to another generator (the spicing one).
	 * 
	 * @author smelC
	 * @param <U>
	 *            Identifiers of monsters
	 * @param <T>
	 *            The concrete type of {@link IAnimate}s used.
	 */
	public class Roulette<U, T extends IAnimate> extends SkeletalMGG<U, T> {

		/** Base generator, called repeatedly */
		protected final IMonstersGroupGenerator<U, T> base;
		/**
		 * Spicy generator, called once in a while when {@link #base} has been used
		 * enough
		 */
		protected final IMonstersGroupGenerator<U, T> roulette;

		private int baseRolls = 0;
		private final int firstRouletteRoll;

		/**
		 * @param base
		 * @param roulette
		 */
		public Roulette(IMonstersGroupGenerator<U, T> base, IMonstersGroupGenerator<U, T> roulette) {
			this(base, roulette, 2);
		}

		/**
		 * @param base
		 * @param roulette
		 * @param firstRouletteRoll
		 */
		public Roulette(IMonstersGroupGenerator<U, T> base, IMonstersGroupGenerator<U, T> roulette,
				int firstRouletteRoll) {
			this.base = base;
			this.roulette = roulette;
			this.firstRouletteRoll = firstRouletteRoll;
		}

		@Override
		public void generate(IMonstersFactory<U, T> factory, IRNG rng, Collection<T> acc) {
			final IMonstersGroupGenerator<U, T> toUse;
			if ((baseRolls == firstRouletteRoll && rng.nextBoolean()) || (firstRouletteRoll + 1 <= baseRolls)) {
				assert baseRolls <= firstRouletteRoll + 1;
				toUse = roulette;
				baseRolls = 0;
			} else {
				toUse = base;
				baseRolls++;
			}
			toUse.generate(factory, rng, acc);
		}

		@Override
		public void addMay(Collection<U> acc) {
			base.addMay(acc);
			roulette.addMay(acc);
		}

		@Override
		public boolean may(U u) {
			return base.may(u) || roulette.may(u);
		}
	}

}
