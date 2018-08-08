package com.hgames.rhogue.generation.map.dungeon.stair;

import java.util.Iterator;

import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public interface IStairGenerator {

	/**
	 * @return The possible locations of a stair, ordered by quality. Or null.
	 */
	public Iterator<Coord> candidates();

}
