package com.hgames.rhogue.generation;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that bounds the number of elements it can return. Useful for loop
 * algorithms that try something and fail after a given number of rolls
 * (placement algorithms).
 * 
 * @author smelC
 * @param <T>
 * 
 * @see ForwardingBoundedIterator A composing variant.
 */
public abstract class BoundedIterator<T> implements Iterator<T> {

	protected int returned = 0;

	/**
	 * The maximum number of elements to return or anything negative for no
	 * bound.
	 */
	protected final int bound;

	/**
	 * @param bound
	 *            The maximum number of elements to return or anything negative
	 *            for no bound.
	 */
	protected BoundedIterator(int bound) {
		this.returned = 0;
		this.bound = bound;
	}

	@Override
	public final boolean hasNext() {
		return (bound < 0 || returned < bound) && hasNext0();
	}

	@Override
	public T next() {
		if (!hasNext())
			throw new NoSuchElementException();
		final T result = next0();
		returned++;
		return result;
	}

	protected abstract boolean hasNext0();

	protected abstract T next0();

}
