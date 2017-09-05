package com.hgames.rhogue.generation.map;

import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
interface CellDoer {

	/**
	 * Do something related to {@code c}.
	 * 
	 * @param c
	 */
	public void doOnCell(Coord c);

}
