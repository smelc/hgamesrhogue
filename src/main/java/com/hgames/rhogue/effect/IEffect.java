package com.hgames.rhogue.effect;

import java.io.Serializable;

import com.hgames.rhogue.animate.IAnimate;

/**
 * Some temporary or durable effects. They are usually attached to instances of
 * {@link IAnimate}.
 * 
 * @author smelC
 */
public interface IEffect extends Serializable {

	/**
	 * @return true if this effect has no effect anymore/should be removed from
	 *         the element to which it is associated.
	 */
	public boolean shouldBeRemoved();

}
