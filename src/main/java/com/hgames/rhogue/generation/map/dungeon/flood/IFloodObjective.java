package com.hgames.rhogue.generation.map.dungeon.flood;

import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public interface IFloodObjective {

	/**
	 * @param c
	 *            A cell that was flooded on.
	 */
	public void record(Coord c);

	/**
	 * @return Whether the objective is met and the flood should be stopped.
	 */
	public boolean isMet();

}
