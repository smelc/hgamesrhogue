package com.hgames.rhogue.generation.map.lifetime;

/**
 * A convenience subtype of {@link SomeShots} for room generators that should be
 * used at most once.
 * 
 * @author smelC
 */
public class OneShot extends SomeShots {

	/**
	 * A fresh instance.
	 */
	public OneShot() {
		super(1);
	}

}
