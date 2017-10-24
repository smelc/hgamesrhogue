package com.hgames.rhogue.fov;

import com.hgames.rhogue.lighting.ILightSource;

/**
 * A concrete implementation of {@link IFOVCell} that keeps track of the
 * strongest source.
 * 
 * @author smelC
 * @param <T>
 */
public class FOVCell<T extends ILightSource> implements IFOVCell<T> {

	protected double lighting;
	protected /* @Nullable */ T source;

	/**
	 * A pristine cell.
	 */
	public FOVCell() {
		this.lighting = 0;
		this.source = null;
	}

	@Override
	public double getLighting() {
		return lighting;
	}

	@Override
	public void clear() {
		lighting = 0;
		source = null;
	}

	@Override
	public boolean unionLight(T src, double v) {
		assert 0 <= v && v <= 1.0;
		if (lighting < v) {
			/* Take strongest source */
			this.lighting = v;
			this.source = src;
			return true;
		} else
			return false;
	}

	/**
	 * @return The source of light in this cell, or null if none.
	 */
	public /* @Nullable */ T getSource() {
		return source;
	}

}
