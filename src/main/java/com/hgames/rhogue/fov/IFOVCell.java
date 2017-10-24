package com.hgames.rhogue.fov;

import com.hgames.rhogue.lighting.ILightSource;

/**
 * A cell lit by some light source.
 * 
 * @author smelC
 * @param <T>
 *            The type of light emitters.
 */
public interface IFOVCell<T extends ILightSource> {

	/** @return {@code 0.0} if cell not lit, else something {@code <= 1.0} */
	public double getLighting();

	/** Resets this cell's state */
	public void clear();

	/**
	 * Callback done when this cell is found to receive light from {@code source}.
	 * 
	 * @param source
	 *            The light source.
	 * @param v
	 *            The amount of light received from the source.
	 * @return Whether something changed.
	 */
	public boolean unionLight(T source, double v);

}
