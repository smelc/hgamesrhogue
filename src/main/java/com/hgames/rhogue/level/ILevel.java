package com.hgames.rhogue.level;

import java.util.List;

import com.hgames.lib.collection.Union;
import com.hgames.rhogue.grid.Grid;
import com.hgames.rhogue.grid.IMapCell;
import com.hgames.rhogue.team.Team;

import squidpony.squidmath.Coord;

/**
 * A level, it contains the level's {@link Grid} as well as caches regarding
 * {@link Team teams}.
 * 
 * @author smelC
 * @param <MC>
 *            The concrete type of cells.
 */
public interface ILevel<MC extends IMapCell> {

	/**
	 * @return The level's width.
	 */
	public int getWidth();

	/**
	 * @return The level's height.
	 */
	public int getHeight();

	/**
	 * @param x
	 * @param y
	 * @return The map cell at {@code (x, y)} or null if out of bounds.
	 */
	public MC getMapCellAt(int x, int y);

	/**
	 * @return Stairs/doors to other levels.
	 */
	public List<Coord> getExits();

	/**
	 * @param t
	 * @return The positions of enemies of team {@code t}.
	 */
	public Union<Coord> getEnemiesOf(Team t);

	/**
	 * @param t
	 * @param considerItselfAsAlly
	 *            Whether members of {@code team t} itself should be considered.
	 * @return The positions of allies of team {@code t}.
	 */
	public Union<Coord> getAlliesOf(Team t, boolean considerItselfAsAlly);

}
