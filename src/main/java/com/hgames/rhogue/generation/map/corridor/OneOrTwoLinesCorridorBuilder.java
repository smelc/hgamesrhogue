package com.hgames.rhogue.generation.map.corridor;

import java.util.EnumSet;

import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.zone.SingleCellZone;
import com.hgames.rhogue.zone.ZoneUnion;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A corridor builder that builds corridors consisting of a single line or of
 * two lines connected by a turn.
 * 
 * @author smelC
 */
public class OneOrTwoLinesCorridorBuilder extends SkeletalCorridorBuilder {

	/** Whether to allow a turn, i.e. to allow two lines corridors. */
	private final boolean allowATurn;

	/**
	 * @param dungeon
	 * @param allowedCarvings
	 * @param allowedNeighbors
	 * @param allowATurn
	 */
	public OneOrTwoLinesCorridorBuilder(Dungeon dungeon, EnumSet<DungeonSymbol> allowedCarvings,
			EnumSet<DungeonSymbol> allowedNeighbors, boolean allowATurn) {
		super(dungeon, allowedCarvings, allowedNeighbors);
		this.allowATurn = allowATurn;
	}

	/**
	 * @param start
	 *            The starting point (won't make it to the result, but a cell in the
	 *            result will be cardinally adjacent to it).
	 * @param end
	 *            The ending point (won't make it to the result, but a cell in the
	 *            result will be cardinally adjacent to it).
	 * @return The corridor built or null if impossible.
	 */
	@Override
	protected /* @Nullable */ Zone build(RNG stable, Coord start, Coord end) {
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
			 * The conditionals about {@link #adjacency(Coord, Coord)} account for this
			 * pattern:
			 * 
			 * <pre>
			 * E    1
			 * 2    S
			 * </pre>
			 * 
			 * where {@code 1} denotes {@code turn1} and {@code 2} denotes {@code turn2}. In
			 * this case the pivot is adjacent to the start or the end and one of the line
			 * is useless.
			 * 
			 * This is required for the assertion about intersectsWith to hold.
			 */
			if (Boolean.TRUE.equals(adjacency(start, pivot)))
				firstPart = null;
			else {
				firstPart = buildLine(start, false, pivot, true);
				assert firstPart != null;
				if (!isCarvingAllowed(firstPart, start, end))
					return null;
			}
			if (Boolean.TRUE.equals(adjacency(pivot, end)))
				secondPart = null;
			else {
				secondPart = buildLine(pivot, false, end, false);
				assert secondPart != null;
				if (!isCarvingAllowed(secondPart, start, end))
					return null;
			}
			assert !(firstPart == null && secondPart == null);
			assert firstPart == null || secondPart == null || !firstPart.intersectsWith(secondPart);
			assert firstPart == null || isCarvingAllowed(firstPart, start, end);
			assert secondPart == null || isCarvingAllowed(secondPart, start, end);
		} else {
			firstPart = buildLine(start, false, end, false);
			if (!isCarvingAllowed(firstPart, start, end))
				return null;
			secondPart = null;
		}

		final Zone result = firstPart == null ? (secondPart == null ? null : firstPart)
				: secondPart == null ? firstPart : new ZoneUnion(firstPart, secondPart);
		assert isCarvingAllowed(result, start, end);
		return result;
	}

	/**
	 * @param z
	 *            A piece of a corridor.
	 * @param start
	 *            The starting point (won't make it to the result, but a cell in the
	 *            result will be cardinally adjacent to it).
	 * @param end
	 *            The ending point (won't make it to the result, but a cell in the
	 *            result will be cardinally adjacent to it).
	 * @return Whether {@code z} is valid according to
	 *         {@link #isCarvingAllowed(Coord)}.
	 */
	private boolean isCarvingAllowed(Zone z, Coord start, Coord end) {
		for (Coord c : z) {
			if (c.equals(start)) {
				assert false;
				return false;
			}
			if (c.equals(end)) {
				assert false;
				return false;
			}
			if (!isCarvingAllowed(c))
				return false;
		}
		return true;
	}

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
		// Not checking isCarvingAllowed here, since 'start' or 'end
		// should maybe not be checked
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
}
