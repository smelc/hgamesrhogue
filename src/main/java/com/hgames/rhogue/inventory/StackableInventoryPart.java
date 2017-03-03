package com.hgames.rhogue.inventory;

import java.util.Collection;

import com.hgames.lib.collection.multiset.EnumMultiset;

/**
 * An inventory part for items that stack. You can retrieve the count of an item
 * with {@link #count(Enum)}. It can be bounded or unbounded.
 * 
 * @author smelC
 * @param <T>
 */
public class StackableInventoryPart<T extends Enum<T>> extends EnumMultiset<T> implements InventoryPart<T> {

	protected final int maxSize;

	private static final long serialVersionUID = 8118160557151036984L;

	/**
	 * @param clazz
	 * @param maxSize
	 *            The maximum size of this inventory part, or anything negative
	 *            if unbounded.
	 */
	public StackableInventoryPart(Class<T> clazz, int maxSize) {
		super(clazz);
		this.maxSize = maxSize;
	}

	@Override
	public boolean add(T e) {
		return atMax() ? false : super.add(e);
	}

	@Override
	public boolean atMax() {
		return isBounded() && maxSize == size();
	}

	@Override
	public boolean isBounded() {
		return 0 <= maxSize;
	}

	@Override
	public int getBound() {
		if (maxSize < 0)
			throw new UnsupportedOperationException("This inventory part is unbounded");
		return maxSize;
	}

	@Override
	public boolean delete(T t) {
		return remove(t);
	}

	@Override
	public boolean replace(T inThere, T novel) {
		final int c = count(inThere);
		if (c == 0)
			return false;
		else {
			final boolean deleted = delete(inThere);
			assert deleted;
			final boolean added = add(novel);
			assert added;
			return true;
		}
	}

	@Override
	public boolean has(T t) {
		return contains(t);
	}

	@Override
	public void pourInto(Collection<? super T> accumulator) {
		for (T key : keySet()) {
			int count = count(key);
			while (0 < count) {
				accumulator.add(key);
				count--;
			}
		}
	}
}
