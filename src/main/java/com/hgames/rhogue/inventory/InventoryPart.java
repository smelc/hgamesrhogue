package com.hgames.rhogue.inventory;

import java.util.Collection;

import com.hgames.rhogue.item.Item;

/**
 * The part of an inventory. A part has a type parameter, so it may either mix
 * all possible kind of items (give {@link Item} as {@code T}) or it may contain
 * all items <em>of some kind</em> (all weapons, all potions, etc. i.e. subtypes
 * of {@link Item}). A part can be bounded (i.e. it may receive only up to a
 * given number of objects or a number of objects that weight some maximum
 * value), or be unbounded.
 * 
 * @author smelC
 * @param <T>
 *            The type of elements
 */
public interface InventoryPart<T> {

	/**
	 * @return true if this part doesn't accept an additional element.
	 */
	public boolean atMax();

	/**
	 * @return true if {@link #atMax()} can return true.
	 */
	public boolean isBounded();

	/**
	 * @return The maximum number of elements that {@code this} can hold.
	 * @throws UnsupportedOperationException
	 *             If {@link #isBounded()} doesn't hold
	 */
	public int getBound();

	/**
	 * @param t
	 *            The element to add.
	 * @return Whether it got added.
	 */
	public boolean add(T t);

	/**
	 * @param t
	 *            The element to remove.
	 * @return Whether it got removed.
	 */
	// Not called "remove", because of possible name clashes with Java API in
	// subclassers that already feature remove(Object).
	public boolean delete(T t);

	/**
	 * @param inThere
	 * @param novel
	 * @return Whether {@code inThere} could be replaced by {@code novel}.
	 */
	public boolean replace(T inThere, T novel);

	/**
	 * @param t
	 * @return true if {@code this} contains {@code t}.
	 */
	// Not called "contains", because of possible name clashes with Java API in
	// subclassers that already feature remove(Object).
	public boolean has(T t);

	/**
	 * Adds all elements of {@code this} into {@code accumulator}.
	 * 
	 * @param accumulator
	 */
	public void pourInto(Collection<? super T> accumulator);

}
