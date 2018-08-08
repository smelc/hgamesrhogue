package com.hgames.rhogue.generation.map.dungeon;

import java.util.Iterator;

import squidpony.squidmath.Coord;

/**
 * How to get coordinates out of thin air.
 * 
 * @author smelC
 */
interface CoordsProvider {

	public Iterator<Coord> getCoords();

}
