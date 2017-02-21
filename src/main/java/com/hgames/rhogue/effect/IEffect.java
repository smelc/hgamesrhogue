package com.hgames.rhogue.effect;

import java.io.Serializable;

import com.hgames.rhogue.animate.IAnimate;

/**
 * Some temporary or durable effects. They are usually attached to instances of
 * {@link IAnimate}.
 * 
 * @author smelC
 * @param <T>
 *            The enumeration of the kind of effects.
 */
public interface IEffect<T extends Enum<T>> extends Serializable {

	/**
	 * @return true if this effect has no effect anymore/should be removed from
	 *         the element to which it is associated.
	 */
	public boolean shouldBeRemoved();

	/**
	 * @return This effect's kind.
	 */
	public T getKind();

}
