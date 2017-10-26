package com.hgames.rhogue.generation.monsters.group;

import com.hgames.rhogue.animate.IAnimate;

/**
 * How to create monsters.
 * 
 * @author smelC
 * 
 * @param <U>
 *            The type of identifiers.
 * @param <T>
 *            The concrete type of monsters.
 */
public interface IMonstersFactory<U, T extends IAnimate> {

	/**
	 * @param identifier
	 * @return A fresh monster corresponding to {@code identifier}.
	 */
	public abstract T create(U identifier);

}
