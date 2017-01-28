package com.hgames.rhogue.grid;

import java.io.Serializable;

/**
 * A 2D grid.
 * 
 * @author smelC
 */
public interface Grid extends Serializable {

	public int getWidth();

	public int getHeight();

	public boolean isInGrid(int x, int y);

}
