package com.hgames.rhogue.generation.map.stair;

import java.util.Queue;

import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public interface IStairGenerator {

	/**
	 * @return The possible locations of a stair, ordered by quality. Or null.
	 */
	public /* @Nullable */ Queue<Coord> candidates();

}
