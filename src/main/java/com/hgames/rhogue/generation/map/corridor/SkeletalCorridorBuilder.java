package com.hgames.rhogue.generation.map.corridor;

import java.util.EnumSet;

import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * @author smelC
 */
public abstract class SkeletalCorridorBuilder implements ICorridorBuilder {

	protected final Dungeon dungeon;
	/** Symbols through which {@code this} can carve corridors */
	protected final EnumSet<DungeonSymbol> allowedCarvings;
	/** Symbols which are acceptable neighbors to carved cells */
	protected final EnumSet<DungeonSymbol> allowedNeighbors;

	protected SkeletalCorridorBuilder(Dungeon dungeon, EnumSet<DungeonSymbol> allowedCarvings,
			EnumSet<DungeonSymbol> allowedNeighbors) {
		this.dungeon = dungeon;
		this.allowedCarvings = allowedCarvings;
		this.allowedNeighbors = allowedNeighbors;
	}

	@Override
	public Zone build(IRNG rng, Coord start, Coord end, Coord[] startEndBuf) {
		Zone candidate = build(rng, start, end);
		if (candidate != null)
			candidate = postprocess(candidate, start, end, startEndBuf);
		return candidate;
	}

	protected abstract Zone build(IRNG rng, Coord start, Coord end);

	/**
	 * @param z
	 *            A candidate bridge.
	 * @param start
	 * @param end
	 * @param startEndBuf
	 *            The buffer to fill with the neighbors of {@code start} and
	 *            {@code end}. Must be of length >= 2. Or null.
	 * @return {@code z} if it goes through {@link #isAllowedNeighbor(Coord)}
	 *         checks.
	 */
	private final /* @Nullable */ Zone postprocess(Zone z, Coord start, Coord end,
			/* @Nullable */ Coord[] startEndBuf) {
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
		for (Coord c : z) {
			if (c.equals(bridgeStart) || c.equals(bridgeEnd))
				continue;
			for (Direction out : Direction.OUTWARDS) {
				if (!isAllowedNeighbor(c.translate(out)))
					return null;
			}
		}
		if (startEndBuf != null) {
			startEndBuf[0] = bridgeStart;
			startEndBuf[1] = bridgeEnd;
		}
		return z;
	}

	protected boolean isCarvingAllowed(Coord c) {
		return dungeon.isValid(c) && isCarvingAllowed(dungeon.getSymbol(c));
	}

	/**
	 * @param sym
	 * @return Whether it's okay to turn {@code sym} into a corridor cell.
	 */
	protected boolean isCarvingAllowed(DungeonSymbol sym) {
		return allowedCarvings.contains(sym);
	}

	/**
	 * @param c
	 * @return Whether it's okay to carve a cell next to {@code c}.
	 */
	/* Subclassers should not need to call this method, but they may override */
	protected boolean isAllowedNeighbor(Coord c) {
		return dungeon.isValid(c) && isAllowedNeighbor(dungeon.getSymbol(c));
	}

	/**
	 * @param sym
	 * @return Whether it's okay to carve a cell next to {@code sym}.
	 */
	/* Subclassers should not need to call this method, but they may override */
	protected boolean isAllowedNeighbor(DungeonSymbol sym) {
		return allowedNeighbors.contains(sym);
	}

	/**
	 * A turn is bad if it isn't surrounded by a lot of
	 * {@link #isAllowedNeighbor(Coord)} cells
	 */
	protected int turnBadness(Coord c) {
		int result = 0;
		for (Direction out : Direction.OUTWARDS) {
			final Coord neighbor = c.translate(out);
			if (!isAllowedNeighbor(neighbor))
				result++;
		}
		return result;
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
			if (adj.booleanValue() == b)
				result++;
		}
		return result;
	}

	/**
	 * @return {@code true} if {@code c1} is cardinally adjacent to {@code c2},
	 *         {@code false} if diagonally adjacent to {@code c2}, null if not
	 *         adjacent.
	 */
	protected static Boolean adjacency(Coord c1, Coord c2) {
		for (Direction out : Direction.OUTWARDS) {
			if (c1.translate(out).equals(c2))
				return Boolean.valueOf(out.isCardinal() ? true : false);
		}
		return null;
	}

}
