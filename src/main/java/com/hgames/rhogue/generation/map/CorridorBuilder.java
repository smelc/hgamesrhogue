package com.hgames.rhogue.generation.map;

import com.hgames.lib.Exceptions;
import com.hgames.rhogue.zone.SingleCellZone;
import com.hgames.rhogue.zone.ZoneUnion;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * @author smelC
 */
class CorridorBuilder {

	private final Dungeon dungeon;
	private final boolean allowATurn;
	/**
	 * {@code true} to request neighbors of inners of corridors (i.e. corridors
	 * except the endpoints) to satisfy {@link #isForbiddenNeighbor0(Coord)}.
	 */
	private final boolean onlyPerfectCarving;

	CorridorBuilder(Dungeon dungeon, boolean allowATurn, boolean onlyPerfectCarving) {
		this.dungeon = dungeon;
		this.allowATurn = allowATurn;
		this.onlyPerfectCarving = onlyPerfectCarving;
	}

			/**
			 * @param startEndBuf
			 *            Where to set the corridor's only cell that is
			 *            cardinally adjacent to {@code start} and the
			 *            corridor's only cell that is cardinally adjacent to
			 *            {@code end}.
			 */
			/* @Nullable */ Zone build(RNG stable, Coord start, Coord end,
					/* @Nullable */ Coord[] startEndBuf) {
		final Zone result = build(stable, start, end);
		if (result == null)
			return null;
		Coord bridgeStart = null;
		Coord bridgeEnd = null;
		assert nbAdjacentCells(start, result, true) == 1;
		assert nbAdjacentCells(end, result, true) == 1;
		for (Coord c : result) {
			boolean change = false;
			if (Boolean.TRUE.equals(adjacency(c, start))) {
				assert bridgeStart == null;
				bridgeStart = c;
				change |= true;
			}
			if (Boolean.TRUE.equals(adjacency(c, end))) {
				assert bridgeEnd == null;
				bridgeEnd = c;
				change |= true;
			}
			if (change && bridgeStart != null && bridgeEnd != null)
				break;
		}
		assert bridgeStart != null;
		assert bridgeEnd != null;
		if (onlyPerfectCarving) {
			for (Coord c : result) {
				if (c.equals(bridgeStart) || c.equals(bridgeEnd))
					continue;
				for (Direction out : Direction.OUTWARDS) {
					if (isForbiddenNeighbor(c.translate(out)))
						return null;
				}
			}
		}
		if (startEndBuf != null) {
			startEndBuf[0] = bridgeStart;
			startEndBuf[1] = bridgeEnd;
		}
		return result;
	}

	/**
	 * @return Whether it's okay to turn this cell into a corridor, looking only
	 *         at this cell.
	 */
	protected boolean isSingleCellCarvingAllowed(Coord c) {
		assert dungeon.isValid(c);
		if (Dungeons.isOnEdge(dungeon, c))
			return false;
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
			return true;
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

	private final boolean isForbiddenNeighbor(Coord c) {
		return dungeon.isValid(c) && isForbiddenNeighbor0(c);
	}

	protected boolean isForbiddenNeighbor0(Coord c) {
		final DungeonSymbol sym = dungeon.getSymbol(c);
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
			return true;
		case WALL:
			return false;
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

	/**
	 * @param start
	 *            The starting point (won't make it to the result, but a cell in
	 *            the result will be cardinally adjacent to it).
	 * @param end
	 *            The ending point (won't make it to the result, but a cell in
	 *            the result will be cardinally adjacent to it).
	 * @return The corridor built or null if impossible.
	 */
	private /* @Nullable */ Zone build(RNG stable, Coord start, Coord end) {
		final Zone firstPart;
		final Zone secondPart;
		if (needTurn(start, end)) {
			if (!allowATurn)
				return null;
			final boolean b = stable.nextBoolean();
			final Coord turn1 = getTurn(start, end, b);
			final Coord turn2 = getTurn(start, end, !b);
			assert dungeon.isValid(turn1);
			assert dungeon.isValid(turn2);
			final Coord pivot = turnBadness(turn1) < turnBadness(turn2) ? turn1 : turn2;
			/**
			 * The conditionals about {@link #adjacency(Coord, Coord)} account
			 * for this pattern:
			 * 
			 * <pre>
			 * E    1
			 * 2    S
			 * </pre>
			 * 
			 * where {@code 1} denotes {@code turn1} and {@code 2} denotes
			 * {@code turn2}. In this case the pivot is adjacent to the start or
			 * the end and one of the line is useless.
			 * 
			 * This is required for the assertion about intersectsWith to hold.
			 */
			if (Boolean.TRUE.equals(adjacency(start, pivot)))
				firstPart = null;
			else {
				firstPart = buildLine(start, false, pivot, true);
				if (firstPart == null)
					/* Cannot do */
					return null;
			}
			if (Boolean.TRUE.equals(adjacency(pivot, end)))
				secondPart = null;
			else {
				secondPart = buildLine(pivot, false, end, false);
				if (secondPart == null)
					/* Cannot do */
					return null;
			}
			assert !(firstPart == null && secondPart == null);
			assert firstPart == null || secondPart == null || !firstPart.intersectsWith(secondPart);
		} else {
			firstPart = buildLine(start, false, end, false);
			secondPart = null;
		}

		return firstPart == null ? (secondPart == null ? null : firstPart)
				: secondPart == null ? firstPart : new ZoneUnion(firstPart, secondPart);
	}

	/** @return The line built, or null if impossible */
	private /* @Nullable */ Zone buildLine(Coord start, boolean includeStart, Coord end, boolean includeEnd) {
		/* is 'x' fixed ? */
		final boolean vertical = start.x == end.x;
		assert vertical || start.y == end.y;
		final Coord bottomLeft;
		if (vertical) {
			assert start.x == end.x;
			// Recall that in SquidLib, a larget y denotes a lower cell
			bottomLeft = start.y >= end.y ? start : end;
		} else {
			assert start.y == end.y;
			bottomLeft = start.x <= end.x ? start : end;
		}
		final Coord other = bottomLeft == start ? end : start;
		return buildLine0(bottomLeft, bottomLeft == start ? includeStart : includeEnd, other,
				bottomLeft == start ? includeEnd : includeStart);
	}

	private /* @Nullable */ Zone buildLine0(Coord start, boolean includeStart, Coord end,
			boolean includeEnd) {
		final boolean vertical = start.x == end.x;
		assert vertical || start.y == end.y;
		// Check that 'start' is at the bottom left of 'end'
		// Recall that in SquidLib, a smaller y denotes a higher cell
		assert (vertical && start.y >= end.y) || (!vertical && start.x <= end.x);
		if (vertical) {
			return buildLine(includeStart ? start : start.translate(Direction.UP),
					includeEnd ? end : end.translate(Direction.DOWN));
		} else {
			return buildLine(includeStart ? start : start.translate(Direction.RIGHT),
					includeEnd ? end : end.translate(Direction.LEFT));
		}
	}

	private /* @Nullable */ Zone buildLine(Coord start, Coord end) {
		final Zone result;
		if (start.equals(end))
			result = new SingleCellZone(start);
		else {
			final boolean vertical = start.x == end.x;
			/* +1 because 'end' is inclusive */
			final int width = vertical ? 1 : Math.abs(start.x - end.x) + 1;
			final int height = vertical ? Math.abs(start.y - end.y) + 1 : 1;
			final int blx = Math.min(start.x, end.x);
			final int bly = Math.max(start.y, end.y);
			final Coord bottomLeft = Coord.get(blx, bly);
			result = new Rectangle.Impl(bottomLeft, width, height);
		}
		assert result.contains(start);
		assert result.contains(end);
		for (Coord inCorridor : result) {
			if (!isSingleCellCarvingAllowed(inCorridor))
				return null;
		}
		return result;
	}

	private boolean needTurn(Coord start, Coord end) {
		if (start.x == end.x)
			return false;
		if (start.y == end.y)
			return false;
		return true;
	}

	private /* @Nullable */ Coord getTurn(Coord start, Coord end, boolean takeLeft) {
		if (start.x == end.x)
			/* No turn needed */
			return null;
		if (start.y == end.y)
			/* No turn needed */
			return null;
		final boolean startOnLeft = start.x < end.x;
		// final boolean startLower = end.y < start.y;
		final int x = takeLeft ? (startOnLeft ? start.x : end.x) : (startOnLeft ? end.x : start.x);
		final int y = takeLeft ? (startOnLeft ? end.y : start.y) : (startOnLeft ? start.y : end.y);
		return Coord.get(x, y);
	}

	/**
	 * A turn is bad if it isn't surrounded by a lot of
	 * {@link #getGoodTurnNeigbor()} cells
	 */
	private int turnBadness(Coord c) {
		int result = 0;
		final DungeonSymbol good = getGoodTurnNeigbor();
		for (Direction out : Direction.OUTWARDS) {
			final Coord neighbor = c.translate(out);
			final DungeonSymbol sym = dungeon.getSymbol(neighbor);
			if (sym == null) {
				result++;
				continue;
			}
			if (sym != good)
				result++;
		}
		return result;
	}

	private DungeonSymbol getGoodTurnNeigbor() {
		return DungeonSymbol.WALL;
	}

	/**
	 * @return The number of cells b-adjacent to {@code c} in {@code coords}.
	 *         b-adjacence is defined in {@link #adjacency(Coord, Coord)}
	 */
	private static int nbAdjacentCells(Coord c, Iterable<Coord> coords, boolean b) {
		int result = 0;
		for (Coord d : coords) {
			final Boolean adj = adjacency(c, d);
			if (adj == null)
				continue;
			if (adj == b)
				result++;
		}
		return result;
	}

	/**
	 * @return {@code true} if {@code c1} is cardinally adjacent to {@code c2},
	 *         {@code false} if diagonally adjacent to {@code c2}, null if not
	 *         adjacent.
	 */
	private static Boolean adjacency(Coord c1, Coord c2) {
		for (Direction out : Direction.OUTWARDS) {
			if (c1.translate(out).equals(c2))
				return out.isCardinal() ? true : false;
		}
		return null;
	}
}
