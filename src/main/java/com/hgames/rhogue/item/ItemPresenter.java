package com.hgames.rhogue.item;

import com.hgames.rhogue.messages.IMessages;

import squidpony.panel.IColoredString;

/**
 * How to present instances of {@link Item}s. Instances of this class should
 * very likely internally rely on an instance of {@link IMessages}. There should
 * likely be a single instance of this class, which should be as alive as your
 * game instance.
 * 
 * @author smelC
 * 
 * @param <T>
 *            The type of items
 * @param <C>
 *            The type of colors.
 */
public interface ItemPresenter<T extends Item, C> {

	/**
	 * @param item
	 * @param count
	 *            The number of instances of {@code item} to consider (greater
	 *            than 1 only for stackable items).
	 * @return A description of {@code item}.
	 */
	public IColoredString<C> present(T item, int count);

}
