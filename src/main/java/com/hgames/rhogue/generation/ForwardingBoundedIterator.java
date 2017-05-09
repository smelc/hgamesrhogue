package com.hgames.rhogue.generation;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator that bounds the number of elements returned by another iterator.
 * Useful for loop algorithms that try something and fail after a given number
 * of rolls (placement algorithms).
 * 
 * @author smelC
 * @param <T>
 * 
 * @see BoundedIterator A subclassing variant
 */
public class ForwardingBoundedIterator<T> implements Iterator<T> {

	protected int returned = 0;

	/**
	 * The maximum number of elements to return or anything negative for no
	 * bound.
	 */
	protected final int bound;

	protected final Iterator<T> delegate;

	/**
	 * @param delegate
	 * @param bound
	 *            The maximum number of elements to return or anything negative
	 *            for no bound.
	 */
	public ForwardingBoundedIterator(Iterator<T> delegate, int bound) {
		this.returned = 0;
		this.bound = bound;
		this.delegate = delegate;
	}

	@Override
	public final boolean hasNext() {
		return (bound < 0 || returned < bound) && delegate.hasNext();
	}

	@Override
	public T next() {
		if (!hasNext())
			throw new NoSuchElementException();
		final T result = delegate.next();
		returned++;
		return result;
	}

	@Override
	public void remove() {
		delegate.remove();
	}

}
