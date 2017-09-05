package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.List;

import com.hgames.lib.Exceptions;
import com.hgames.lib.collection.Collections;
import com.hgames.rhogue.zone.SingleCellZone;
import com.hgames.rhogue.zone.ZoneUnion;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.ListZone;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Bresenham;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * @author smelC
 */
// FIXME CH Introduce an interface, and split this implem
// into two, separating 'allowATurn' and 'bresenham'.
class CorridorBuilder {

	private final Dungeon dungeon;

	/**
	 * Whether to allow turns when building corridors using two lines and
	 * bresenham.
	 */
	private final boolean allowATurn;

	/**
	 * Whether to fallback to using bresenham if using one (or two, if
	 * {@link #allowATurn} holds) line(s) failed.
	 */
	private final boolean bresenham;

	/**
	 * {@code true} to request neighbors of inners of corridors (i.e. corridors
	 * except the endpoints) to satisfy
	 * {@link #isForbiddenNeighbor0(Coord, boolean)}.
	 */
	private final boolean onlyPerfectCarving;

	CorridorBuilder(Dungeon dungeon, boolean allowATurn, boolean bresenham, boolean onlyPerfectCarving) {
		this.dungeon = dungeon;
		this.allowATurn = allowATurn;
		this.bresenham = bresenham;
		this.onlyPerfectCarving = onlyPerfectCarving;
	}

			/**
			 * @param startEndBuf
			 *            Where to set the corridor's only cell that is
			 *            cardinally adjacent to {@code start} and the
			 *            corridor's only cell that is cardinally adjacent to
			 *            {@code end}.
			 * @return A zone connecting {@code start} and {@code end}. It
			 *         doesn't contain neither {@code start} nor {@code end}.
			 */
			/* @Nullable */ Zone build(RNG stable, Coord start, Coord end,
					/* @Nullable */ Coord[] startEndBuf) {
		Zone result = buildOneOrTwoLines(stable, start, end);
		if (result != null) {
			result = postprocess(result, start, end, startEndBuf, false);
			if (result != null)
				return result;
		}
		if (!bresenham)
			return null;
		result = buildBresenham(stable, start, end);
		if (result == null)
			return null;
		result = postprocess(result, start, end, startEndBuf, true);
		return result;
	}

	private Zone postprocess(Zone z, Coord start, Coord end, /* @Nullable */ Coord[] startEndBuf,
			boolean bresenham_) {
		assert z != null;
		Coord bridgeStart = null;
		Coord bridgeEnd = null;
		/* Number of cells cardinally adjacent to 'start' is 1 */
		assert nbAdjacentCells(start, z, true) == 1 : "Expected number of cells cardinally adjacent to "
				+ start + " to be 1, but found " + nbAdjacentCells(start, z, true);
		/* Number of cells cardinally adjacent to 'end' is 1 */
		assert nbAdjacentCells(end, z, true) == 1 : "Expected number of cells cardinally adjacent to " + end
				+ " to be 1, but found " + nbAdjacentCells(end, z, true);

		for (Coord c : z) {
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
			for (Coord c : z) {
				if (c.equals(bridgeStart) || c.equals(bridgeEnd))
					continue;
				for (Direction out : Direction.OUTWARDS) {
					assert !bresenham_ || bresenham;
					if (isForbiddenNeighbor(c.translate(out), bresenham_))
						return null;
				}
			}
		}
		if (startEndBuf != null) {
			startEndBuf[0] = bridgeStart;
			startEndBuf[1] = bridgeEnd;
		}
		return z;
	}

	/**
	 * @return Whether it's okay to turn this cell into a corridor, looking only
	 *         at this cell.
	 */
	protected boolean isSingleCellCarvingAllowed(Coord c, boolean bresenham_) {
		assert dungeon.isValid(c);
		if (Dungeons.isOnEdge(dungeon, c))
			return false;
		final DungeonSymbol sym = dungeon.getSymbol(c);
		if (sym == null)
			return false;
		switch (sym) {
		case DEEP_WATER:
			return bresenham_;
		case CHASM:
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

	private final boolean isForbiddenNeighbor(Coord c, boolean bresenham_) {
		return dungeon.isValid(c) && isForbiddenNeighbor0(c, bresenham_);
	}

	protected boolean isForbiddenNeighbor0(Coord c, boolean bresenham_) {
		final DungeonSymbol sym = dungeon.getSymbol(c);
		/*
		 * If this method is changed for 'bresenham_', DungeonGenerator::
		 * generateCorridors should be adapted.
		 */
		switch (sym) {
		case DEEP_WATER:
			/* forbidden is bresenham, otherwise allowed */
			return !bresenham_;
		case CHASM:
		case DOOR:
		case FLOOR:
		case GRASS:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
			/* Always forbidden */
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
	private /* @Nullable */ Zone buildOneOrTwoLines(RNG stable, Coord start, Coord end) {
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

	private /* @Nullable */ Zone buildBresenham(RNG stable, Coord start, Coord end) {
		final Coord[] array = Bresenham.line2D_(start, end);
		final List<Coord> result = new ArrayList<Coord>(array.length + (array.length / 4));
		assert array[0].equals(start);
		assert array[array.length - 1].equals(end);
		Coord prev = null;
		for (int i = 0; i < array.length; i++) {
			/*
			 * If 'extremity' holds we should not add 'now', but the turn maybe
			 */
			final boolean extremity = i == 0 || i == array.length - 1;
			final Coord now = array[i];
			if (prev != null) {
				assert prev.isAdjacent(now);
				final Direction dir = prev.toGoTo(now);
				if (!dir.isCardinal()) {
					/*
					 * Need to add a turn, because we want the result to be
					 * cardinally walkable.
					 */
					final Coord candidate1 = prev.translate(dir.clockwise());
					final Coord candidate2 = prev.translate(dir.counterClockwise());
					final int bad1 = turnBadness(candidate1);
					final int bad2 = turnBadness(candidate2);
					final Coord chosenTurn;
					if (bad1 == bad2)
						chosenTurn = stable.nextBoolean() ? candidate1 : candidate2;
					else if (bad1 < bad2)
						chosenTurn = candidate1;
					else
						chosenTurn = candidate2;
					final Coord finally_;
					if (isSingleCellCarvingAllowed(chosenTurn, true))
						finally_ = chosenTurn;
					else {
						final Coord other = chosenTurn == candidate1 ? candidate2 : candidate1;
						if (!isSingleCellCarvingAllowed(other, true))
							return null;
						finally_ = other;
					}
					assert isSingleCellCarvingAllowed(finally_, true);
					result.add(finally_);
				}
			}
			if (!extremity) {
				if (!isSingleCellCarvingAllowed(now, true))
					return null;
				result.add(now);
			}
			prev = now;
		}
		assert Collections.isSet(result);
		return result.isEmpty() ? null : new ListZone(result);
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
			if (!isSingleCellCarvingAllowed(inCorridor, false))
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
