package com.hgames.rhogue.lighting;

import com.hgames.lib.color.IColor;

/**
 * A source of light.
 * 
 * @author smelC
 */
public interface ILightSource {

	/** @return The color that this source emits, or null if none. */
	public /* @Nullable */ IColor getLightColor();

	/** @return The intensity of {@link #getLightColor()}. */
	public int getLightIntensity();

}
