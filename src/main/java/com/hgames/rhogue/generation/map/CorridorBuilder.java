package com.hgames.rhogue.generation.map;

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

	CorridorBuilder(Dungeon dungeon, boolean allowATurn) {
		this.dungeon = dungeon;
		this.allowATurn = allowATurn;
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
	Zone build(RNG stable, Coord start, Coord end) {
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
			firstPart = buildLine(start, false, pivot, true);
			secondPart = buildLine(pivot, false, end, false);
		} else {
			firstPart = buildLine(start, false, end, false);
			secondPart = null;
		}

		return firstPart == null ? (secondPart == null ? null : firstPart)
				: secondPart == null ? firstPart : new ZoneUnion(firstPart, secondPart);
	}

	/** @return The line built */
	private Zone buildLine(Coord start, boolean includeStart, Coord end, boolean includeEnd) {
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

	private Zone buildLine0(Coord start, boolean includeStart, Coord end, boolean includeEnd) {
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

	private Zone buildLine(Coord start, Coord end) {
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
}
