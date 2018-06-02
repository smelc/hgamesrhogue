package com.hgames.rhogue.rng;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import com.hgames.lib.GwtIncompatible;

import squidpony.squidmath.IRNG;

/**
 * A partial implementation of {@link IRNG} that fills the higher-level methods
 * (not the {@code next*} ones). Methods are final, because really you shouldn't
 * change them.
 * 
 * <p>
 * It is {@link Serializable}, but subclasses may not. Use
 * {@link #toSerializable()} to get the serialized version.
 * </p>
 * 
 * @author smelC
 */
public abstract class AbstractRNG implements IRNG, Serializable {

	private static final long serialVersionUID = -2754544638448229504L;

	@Override
	public final int between(int min, int max) {
		if (min == max) {
			return min;
		} else {
			final int result = nextInt(max - min) + min;
			assert min <= result && result < max;
			return result;
		}
	}

	@Override
	public final <T> T getRandomElement(T[] array) {
		return array.length == 0 ? null : array[nextInt(array.length)];
	}

	@Override
	public final <T> T getRandomElement(List<T> list) {
		final int sz = list.size();
		return sz == 0 ? null : list.get(nextInt(sz));
	}

	@Override
	public final <T> T getRandomElement(Collection<T> collection) {
		int sz = collection.size();
		if (sz == 0)
			return null;
		int idx = nextInt(sz);
		final Iterator<T> it = collection.iterator();
		while (0 < idx) {
			assert it.hasNext();
			it.next();
			idx--;
		}
		assert it.hasNext();
		return it.next();
	}

	@Override
	public final <T> Iterable<T> getRandomStartIterable(final List<T> list) {
		// I initially contributed this code to SquidLib, so I just copy/pasted it
		final int sz = list.size();
		if (sz == 0)
			return Collections.<T>emptyList();

		/*
		 * Here's a tricky bit: Defining 'start' here means that every Iterator returned
		 * by the returned Iterable will have the same iteration order. In other words,
		 * if you use more than once the returned Iterable, you'll will see elements in
		 * the same order every time, which is desirable.
		 */
		final int start = nextInt(sz);

		return new Iterable<T>() {
			@Override
			public final Iterator<T> iterator() {
				return new Iterator<T>() {

					int next = -1;

					@Override
					public final boolean hasNext() {
						return next != start;
					}

					@Override
					public final T next() {
						if (next == start)
							throw new NoSuchElementException("Iteration terminated; check hasNext() before next()");
						if (next == -1)
							/* First call */
							next = start;
						final T result = list.get(next);
						if (next == sz - 1)
							/*
							 * Reached the list's end, let's continue from the list's left.
							 */
							next = 0;
						else
							next++;
						return result;
					}

					@Override
					public final void remove() {
						throw new UnsupportedOperationException("Remove is not supported from a randomStartIterable");
					}

					@Override
					public final String toString() {
						return "RandomStartIterator at index " + next;
					}
				};
			}
		};
	}

	@Override
	public short nextShort(short bound) {
		return (short) nextInt(bound);
	}

	@Override
	@GwtIncompatible
	public final <T> T[] shuffle(T[] elements) {
		final int sz = elements.length;
		final T[] result = Arrays.copyOf(elements, sz);
		for (int i = 0; i < sz; i++) {
			final int j = i + nextInt(sz - i);
			final T save = result[j];
			result[j] = result[i];
			result[i] = save;
		}
		return result;
	}

	@Override
	public final <T> T[] shuffle(T[] elements, T[] dest) {
		assert elements != dest;
		if (dest.length != elements.length) {
			assert false;
			return dest;
		}
		for (int i = 0; i < elements.length; i++) {
			final int r = nextInt(i + 1);
			if (r != i)
				dest[i] = dest[r];
			dest[r] = elements[i];
		}
		return dest;

	}

	@Override
	public final <T> ArrayList<T> shuffle(Collection<T> elements, ArrayList<T> buf) {
		final ArrayList<T> result;
		if (buf == null || !buf.isEmpty())
			result = new ArrayList<>(elements);
		else {
			result = buf;
			result.addAll(elements);
		}
		final int sz = result.size();
		for (int i = 0; i < sz; i++)
			Collections.swap(result, i + nextInt(sz - i), i);
		return result;
	}

	@Override
	public final <T> void shuffleInPlace(T[] elements) {
		for (int i = elements.length - 1; i > 0; i--) {
			final int r = nextInt(i + 1);
			final T save = elements[r];
			elements[r] = elements[i];
			elements[i] = save;
		}
	}

	@Override
	public final <T> void shuffleInPlace(List<T> elements) {
		assert !(elements instanceof LinkedList);
		final int sz = elements.size();
		for (int i = sz - 1; i > 0; i--) {
			final int j = nextInt(i + 1);
			/* Get */
			final T atJ = elements.get(j);
			final T atI = elements.get(i);
			/* Swap */
			elements.set(j, atI);
			elements.set(i, atJ);
		}
	}

}
