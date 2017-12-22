package com.hgames.rhogue.grid;

import java.io.Serializable;

import squidpony.squidmath.Coord;

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
	 * @param c
	 * @return Whether {@code c} is a valid location.
	 */
	public boolean isInGrid(Coord c);

	/**
	 * @param x
	 * @param y
	 * @return Whether {@code (x, y)} is a valid location.
	 */
	public boolean isInGrid(int x, int y);

	/**
	 * @param c
	 * @return Whether {@code c} is on one of this grid's edge.
	 */
	public boolean isOnEdge(Coord c);

	/**
	 * @param x
	 * @param y
	 * @return Whether {@code (x, y)} is on one of this grid's edge.
	 */
	public boolean isOnEdge(int x, int y);

}
