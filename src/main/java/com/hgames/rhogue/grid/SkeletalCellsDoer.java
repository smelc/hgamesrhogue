package com.hgames.rhogue.grid;

/**
 * A skeletal implementation of {@link CellsDoer}.
 * 
 * @author smelC
 */
public abstract class SkeletalCellsDoer implements CellsDoer {

	/**
	 * Do something on an individual cell.
	 * 
	 * @param x
	 * @param y
	 * @return true to stop
	 */
	protected abstract boolean doOnACell(int x, int y);

}
