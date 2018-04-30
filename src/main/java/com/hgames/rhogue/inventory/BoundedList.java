package com.hgames.rhogue.inventory;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A list that accepts a limited number of elements. Useful to implement
 * slot-based inventories in roguelikes.
 * 
 * @author smelC
 * @param <T>
 *            The type of elements
 */
public class BoundedList<T> extends ArrayList<T> implements InventoryPart<T> {

	protected final int maxSize;

	private static final long serialVersionUID = 998652098836517339L;

	/**
	 * @param maxSize
	 *            The maximum number of elements that this list accepts.
	 * @throws IllegalArgumentException
	 *             If {@code maxSize < 0}
	 */
	public BoundedList(int maxSize) {
		this.maxSize = maxSize;
		checkMaxSize();
	}

	/**
	 * @param c
	 *            The initial elements.
	 * @param maxSize
	 *            The maximum number of elements that this list accepts.
	 * @throws IllegalArgumentException
	 *             If {@code maxSize < 0}
	 */
	public BoundedList(Collection<? extends T> c, int maxSize) {
		super(c);
		this.maxSize = maxSize;
		checkMaxSize();
		addAll(c);
	}

	/**
	 * @param initialCapacity
	 * @param maxSize
	 *            The maximum number of elements that this list accepts.
	 * @throws IllegalArgumentException
	 *             If {@code maxSize < 0}
	 */
	public BoundedList(int initialCapacity, int maxSize) {
		super(initialCapacity);
		this.maxSize = maxSize;
		checkMaxSize();
	}

	@Override
	public void add(int index, T element) {
		if (!atMax())
			super.add(index, element);
	}

	@Override
	public boolean add(T e) {
		return atMax() ? false : super.add(e);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean result = false;
		for (T t : c) {
			if (!add(t))
				return result;
			result |= true;
		}
		return result;
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		int i = index;
		boolean result = false;
		for (T t : c) {
			if (atMax())
				break;
			add(i++, t);
			result |= true;
		}
		return result;
	}

	/**
	 * @return true if this list doesn't accept additional elements.
	 */
	@Override
	public boolean atMax() {
		return size() == maxSize;
	}

	@Override
	public boolean isBounded() {
		return true;
	}

	@Override
	public int getBound() {
		return maxSize;
	}

	@Override
	public boolean delete(T t) {
		return remove(t);
	}

	@Override
	public boolean replace(T inThere, T novel) {
		final int idx = indexOf(inThere);
		if (idx < 0)
			return false;
		else {
			set(idx, novel);
			return true;
		}
	}

	@Override
	public boolean has(T t) {
		return contains(t);
	}

	@Override
	public void pourInto(Collection<? super T> accumulator) {
		accumulator.addAll(this);
	}

	private void checkMaxSize() {
		if (maxSize < 0)
			throw new IllegalArgumentException(
					"Maximum size of a bounded list should be greater or equal than 0, but received "
							+ maxSize);
	}
}
