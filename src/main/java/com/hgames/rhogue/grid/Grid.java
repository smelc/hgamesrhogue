package com.hgames.rhogue.grid;

import java.io.Serializable;

/**
 * A 2D grid.
 * 
 * @author smelC
 */
public interface Grid extends Serializable {

	/**
	 * @return The grid's width.
	 */
	public int getWidth();

	/**
	 * @return The grid's height.
	 */
	public int getHeight();

	/**
	 * @param x
	 * @param y
	 * @return Whether {@code (x, y)} is a valid location.
	 */
	public boolean isInGrid(int x, int y);

}
