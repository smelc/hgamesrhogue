package com.hgames.rhogue.generation.map.corridor;

import java.util.EnumSet;

import com.hgames.lib.Exceptions;
import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.generation.map.DungeonSymbol;

/**
 * Facilities to build instances of {@link ICorridorBuilder}.
 * 
 * @author smelC
 */
public final class CorridorBuilders {

	private static final EnumSet<DungeonSymbol> PERFECT_CARVING = EnumSet.of(DungeonSymbol.WALL);
	private static final EnumSet<DungeonSymbol> IMPERFECT_CARVING;

	private static final EnumSet<DungeonSymbol> PERFECT_NEIGHBORS = EnumSet.of(DungeonSymbol.WALL);
	private static final EnumSet<DungeonSymbol> IMPERFECT_NEIGHBORS;
	private static final boolean ALLOW_A_TURN = true;

	static {
		IMPERFECT_CARVING = EnumSet.noneOf(DungeonSymbol.class);
		for (DungeonSymbol sym : DungeonSymbol.values()) {
			switch (sym) {
			case DEEP_WATER:
			case WALL:
				/* Carving allowed */
				IMPERFECT_CARVING.add(sym);
				continue;
			case CHASM:
			case DOOR:
			case FLOOR:
			case HIGH_GRASS:
			case SHALLOW_WATER:
			case GRASS:
			case STAIR_DOWN:
			case STAIR_UP:
				/* Carving disallowed */
				continue;
			}
			throw Exceptions.newUnmatchedISE(sym);
		}

		IMPERFECT_NEIGHBORS = EnumSet.noneOf(DungeonSymbol.class);
		for (DungeonSymbol sym : DungeonSymbol.values()) {
			switch (sym) {
			case CHASM:
			case DEEP_WATER:
			case WALL:
				/* Allowed neighbors */
				IMPERFECT_NEIGHBORS.add(sym);
				continue;
			case DOOR:
			case FLOOR:
			case HIGH_GRASS:
			case SHALLOW_WATER:
			case GRASS:
			case STAIR_DOWN:
			case STAIR_UP:
				/*
				 * Disallowed neighbors. Recall that this applies to inners of
				 * corridors (not the cell adjacent to the start, nor the cell
				 * adjacent to the end).
				 */
				continue;
			}
			throw Exceptions.newUnmatchedISE(sym);
		}
	}

	/**
	 * @param dungeon
	 * @param perfect
	 *            Whether to use {@link #PERFECT_CARVING} and
	 *            {@link #PERFECT_NEIGHBORS} or {@link #IMPERFECT_CARVING} and
	 *            {@link #IMPERFECT_NEIGHBORS}.
	 * @param bresenham
	 *            true to use {@link BresenhamCorridorBuilder}, false to use
	 *            {@link OneOrTwoLinesCorridorBuilder}.
	 * @return An instance of {@link ICorridorBuilder}.
	 */
	public static ICorridorBuilder create(Dungeon dungeon, boolean perfect, boolean bresenham) {
		final EnumSet<DungeonSymbol> allowedCarvings = getCarvings(perfect);
		final EnumSet<DungeonSymbol> allowedNeighbors = getNeighbors(perfect);
		return bresenham ? new BresenhamCorridorBuilder(dungeon, allowedCarvings, allowedNeighbors)
				: new OneOrTwoLinesCorridorBuilder(dungeon, allowedCarvings, allowedNeighbors, ALLOW_A_TURN);
	}

	/**
	 * @param dungeon
	 * @param perfect
	 * @param bresenhamFirst
	 *            Whether to put the instance of
	 *            {@link BresenhamCorridorBuilder} first or second.
	 * @return The sequence of {@link BresenhamCorridorBuilder} and
	 *         {@link OneOrTwoLinesCorridorBuilder}.
	 */
	public static ICorridorBuilder createCombination(Dungeon dungeon, boolean perfect,
			boolean bresenhamFirst) {
		final EnumSet<DungeonSymbol> allowedCarvings = getCarvings(perfect);
		final EnumSet<DungeonSymbol> allowedNeighbors = getNeighbors(perfect);
		final ICorridorBuilder bresenham = new BresenhamCorridorBuilder(dungeon, allowedCarvings,
				allowedNeighbors);
		final ICorridorBuilder oneOrTwoLines = new OneOrTwoLinesCorridorBuilder(dungeon, allowedCarvings,
				allowedNeighbors, ALLOW_A_TURN);
		final ICorridorBuilder[] array = new ICorridorBuilder[2];
		array[0] = bresenhamFirst ? bresenham : oneOrTwoLines;
		array[1] = bresenhamFirst ? oneOrTwoLines : bresenham;
		assert array[0] != array[1];
		return new SequencedCorridorBuilder(array);
	}

	private static EnumSet<DungeonSymbol> getCarvings(boolean perfect) {
		return perfect ? PERFECT_CARVING : IMPERFECT_CARVING;
	}

	private static EnumSet<DungeonSymbol> getNeighbors(boolean perfect) {
		return perfect ? PERFECT_NEIGHBORS : IMPERFECT_NEIGHBORS;
	}

}
