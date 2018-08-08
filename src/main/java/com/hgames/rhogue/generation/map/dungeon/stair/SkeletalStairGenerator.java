package com.hgames.rhogue.generation.map.dungeon.stair;

import java.util.ArrayList;
import java.util.List;

import com.hgames.lib.Exceptions;
import com.hgames.lib.Objects;
import com.hgames.lib.collection.multiset.EnumMultiset;
import com.hgames.rhogue.generation.map.dungeon.Dungeon;
import com.hgames.rhogue.generation.map.dungeon.DungeonSymbol;
import com.hgames.rhogue.generation.map.dungeon.Dungeons;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * A very skeletal implementation of {@link IStairGenerator}.
 * 
 * @author smelC
 */
public abstract class SkeletalStairGenerator implements IStairGenerator {

	protected final Dungeon dungeon;

	private static final List<Coord> COORD_LIST_BUF = new ArrayList<Coord>(4);

	/**
	 * @param dungeon
	 *            The dungeon for which the stair should be generated.
	 */
	protected SkeletalStairGenerator(Dungeon dungeon) {
		this.dungeon = Objects.checkNotNull(dungeon);
	}

	/* Subclassers may override */
	protected boolean isValidCandidate(Coord c) {
		final DungeonSymbol sym = dungeon.getSymbol(c);
		if (sym == null)
			return false;
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case FLOOR:
		case GRASS:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
			return false;
		case WALL:
			break;
		}
		/* Cardinal neighbors */
		final EnumMultiset<DungeonSymbol> cneighbors = Dungeons.getNeighbors(dungeon, c.x, c.y, false);
		boolean reachable = false;
		/* This pattern to avoid missing a case */
		for (DungeonSymbol dsym : DungeonSymbol.values()) {
			switch (dsym) {
			case CHASM:
			case DEEP_WATER:
				/* No constraint */
				continue;
			case GRASS:
			case SHALLOW_WATER:
			case FLOOR:
				if (cneighbors.contains(dsym))
					reachable |= true;
				continue;
			case DOOR:
			case HIGH_GRASS:
			case STAIR_DOWN:
			case STAIR_UP:
				if (cneighbors.contains(dsym))
					/* Stair should not be cardinally adjacent to those */
					return false;
				continue;
			case WALL:
				/* Constraint checked on diagonal neighbors (see below) */
				continue;
			}
			throw Exceptions.newUnmatchedISE(dsym);
		}
		if (!reachable)
			/* Not cardinally accessible from a safe cell */
			return false;
		/* Diagonal neighbors */
		COORD_LIST_BUF.clear();
		int nbw = 0;
		for (Direction dir : Direction.OUTWARDS) {
			final Coord neighbor = c.translate(dir);
			final DungeonSymbol dsym = dungeon.getSymbol(neighbor);
			if (dsym == null)
				continue;
			switch (dsym) {
			case CHASM:
			case DEEP_WATER:
			case HIGH_GRASS:
				continue;
			case FLOOR:
			case GRASS:
			case SHALLOW_WATER:
				/* Can safely go from such cells to the candidate stair */
				COORD_LIST_BUF.add(neighbor);
				continue;
			case DOOR:
			case STAIR_DOWN:
			case STAIR_UP:
				/* Stair should not be adjacent to a door or stair */
				return false;
			case WALL:
				nbw++;
				continue;
			}
			throw Exceptions.newUnmatchedISE(dsym);
		}
		/**
		 * Because we want stairs in such positions:
		 * 
		 * <pre>
		 * ###
		 * #>#
		 * ...
		 * </pre>
		 * 
		 * because we wanna avoid
		 * 
		 * <pre>
		 * .##
		 * #>#
		 * ...
		 * </pre>
		 * 
		 * which would force cause possible placement weirdness upon arriving
		 * into the level (since placement in different rooms would be
		 * possible).
		 */
		if (nbw < 5)
			return false;
		/**
		 * Because we wanna forbid stairs in such positions:
		 * 
		 * <pre>
		 * ####
		 * #>##
		 * #..#
		 * </pre>
		 */
		final int sources = COORD_LIST_BUF.size();
		if (sources < 3)
			return false;
		/**
		 * Check that all sources are adjacent to each other. To avoid:
		 * 
		 * <pre>
		 * ..#
		 * #>#
		 * .##
		 * </pre>
		 */
		if (!haveACrossRoad(COORD_LIST_BUF))
			return false;
		return true;
	}

	/**
	 * @param list
	 * @return {@code true} if there's a member of {@code list} to which all
	 *         other members are adjacent.
	 */
	private static boolean haveACrossRoad(List<Coord> list) {
		final int size = list.size();
		outer: for (int i = 0; i < size; i++) {
			final Coord c = list.get(i);
			for (int j = 0; j < size; j++) {
				if (i == j)
					continue;
				final Coord other = list.get(i);
				if (!c.isAdjacent(other))
					continue outer;
			}
			return true;
		}
		return true;
	}

}
