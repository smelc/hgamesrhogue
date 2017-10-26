package com.hgames.rhogue.rng;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import squidpony.squidmath.RNG;

/**
 * A table to choose objects according to some probability.
 *
 * @author smelC
 * @param <T>
 *            The type of objects
 */
public class ProbabilityTable<T> {

	/**
	 * It is important that this map has a constant iteration order. It maps an
	 * object to its probability.
	 */
	protected final Map<T, Integer> table;
	/** The sum of values of {@link #table}. */
	protected int total;

	/**
	 * Creates a new probability table.
	 */
	public ProbabilityTable() {
		this.table = new LinkedHashMap<T, Integer>();
		this.total = 0;
	}

	/**
	 * Creates a new probability table.
	 * 
	 * @param expectedSize
	 */
	public ProbabilityTable(int expectedSize) {
		this.table = new LinkedHashMap<T, Integer>(expectedSize);
		this.total = 0;
	}

	/** @return A fresh instance. */
	public static <T> ProbabilityTable<T> create() {
		return new ProbabilityTable<T>();
	}

	/**
	 * @param expectedSize
	 * @return A fresh instance.
	 */
	public static <T> ProbabilityTable<T> create(int expectedSize) {
		return new ProbabilityTable<T>(expectedSize);
	}

	/**
	 * @param domain
	 * @return A table that assigns the same probability (1) to every member of
	 *         {@code domain}.
	 */
	public static <T> ProbabilityTable<T> createUniform(List<T> domain) {
		final int sz = domain.size();
		final ProbabilityTable<T> result = new ProbabilityTable<T>(sz);
		for (int i = 0; i < sz; i++)
			result.add(domain.get(i), 1);
		return result;
	}

	/**
	 * @param t1
	 * @param w1
	 *            {@code t1}'s probability.
	 * @param t2
	 * @param w2
	 *            {@code t2}'s probability.
	 * @return A fresh instance initialized with the given elements and
	 *         probabilities.
	 */
	public static <T> ProbabilityTable<T> create(T t1, int w1, T t2, int w2) {
		final ProbabilityTable<T> result = new ProbabilityTable<T>(2);
		result.put(t1, w1);
		result.put(t2, w2);
		return result;
	}

	/**
	 * @param t1
	 * @param w1
	 *            {@code t1}'s probability.
	 * @param t2
	 * @param w2
	 *            {@code t2}'s probability.
	 * @param t3
	 * @param w3
	 *            {@code t3}'s probability.
	 * @return A fresh instance initialized with the given elements and
	 *         probabilities.
	 */
	public static <T> ProbabilityTable<T> create(T t1, int w1, T t2, int w2, T t3, int w3) {
		final ProbabilityTable<T> result = new ProbabilityTable<T>(3);
		result.put(t1, w1);
		result.put(t2, w2);
		result.put(t3, w3);
		return result;
	}

	/**
	 * @param t1
	 * @param w1
	 *            {@code t1}'s probability.
	 * @param t2
	 * @param w2
	 *            {@code t2}'s probability.
	 * @param t3
	 * @param w3
	 *            {@code t3}'s probability.
	 * @param t4
	 * @param w4
	 *            {@code t4}'s probability.
	 * @return A fresh instance initialized with the given elements and
	 *         probabilities.
	 */
	public static <T> ProbabilityTable<T> create(T t1, int w1, T t2, int w2, T t3, int w3, T t4, int w4) {
		final ProbabilityTable<T> result = new ProbabilityTable<T>(4);
		result.put(t1, w1);
		result.put(t2, w2);
		result.put(t3, w3);
		result.put(t4, w4);
		return result;
	}

	/**
	 * @param t1
	 * @param w1
	 *            {@code t1}'s probability.
	 * @param t2
	 * @param w2
	 *            {@code t2}'s probability.
	 * @param t3
	 * @param w3
	 *            {@code t3}'s probability.
	 * @param t4
	 * @param w4
	 *            {@code t4}'s probability.
	 * @param t5
	 * @param w5
	 *            {@code t4}'s probability.
	 * @return A fresh instance initialized with the given elements and
	 *         probabilities.
	 */
	public static <T> ProbabilityTable<T> create(T t1, int w1, T t2, int w2, T t3, int w3, T t4, int w4, T t5, int w5) {
		final ProbabilityTable<T> result = new ProbabilityTable<T>(5);
		result.put(t1, w1);
		result.put(t2, w2);
		result.put(t3, w3);
		result.put(t4, w4);
		result.put(t5, w5);
		return result;
	}

	/**
	 * Adds an object with the given probability. This object may later on be
	 * returned by {@link #get(RNG)} if its probability is > 0.
	 * 
	 * @param t
	 *            The object to be added.
	 * @param probability
	 *            The probability to use for {@code t}. Replaces any previous value
	 *            if {@code t} was added already.
	 */
	public void add(T t, int probability) {
		assert invariant();
		if (probability < 0)
			throw new IllegalStateException("Probability should be >= 0, but received: " + probability);
		final Integer inThere = table.put(t, probability);
		if (inThere != null)
			total -= inThere;
		total += probability;
		assert invariant();
	}

	/**
	 * Adds {@code t} to this table if missing, or changes its probability if
	 * already there.
	 * 
	 * @param t
	 * @param probability
	 */
	public void put(T t, int probability) {
		final int inThere = table.containsKey(t) ? table.get(t) : 0;
		final int diff = probability - inThere;
		total += diff;
		table.put(t, probability);
		assert invariant();
	}

	/**
	 * Remove an element from this table.
	 * 
	 * @param t
	 * @return Whether the element got removed.
	 */
	public boolean remove(T t) {
		final Integer rmed = table.remove(t);
		final boolean result;
		if (rmed == null)
			result = false;
		else {
			total -= rmed;
			result = true;
		}
		assert invariant();
		return result;
	}

	/**
	 * @param rng
	 *            The rng to use.
	 * @return The chosen object (can be null if null was put or if the table is
	 *         empty).
	 */
	public /* @Nullable */ T get(RNG rng) {
		if (table.isEmpty())
			return null;

		int index = rng.nextInt(total);
		for (T t : table.keySet()) {
			index -= table.get(t);
			if (index < 0)
				/* That's a hit */
				return t;
		}

		assert false;
		return null;
	}

	/**
	 * @return Elements that can be returned by {@link #get(RNG)}.
	 */
	public Collection<T> getDomain() {
		return table.keySet();
	}

	/**
	 * @return true if this table {@link #get(RNG)} method cannot return something.
	 */
	public boolean isEmpty() {
		return table.isEmpty() || total == 0;
	}

	protected boolean invariant() {
		int weight = 0;
		for (Integer value : table.values())
			weight += value;
		return weight == total;
	}

	@Override
	public String toString() {
		return table.toString();
	}
}
