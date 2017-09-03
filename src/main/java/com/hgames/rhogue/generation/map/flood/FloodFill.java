package com.hgames.rhogue.generation.map.flood;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import com.hgames.rhogue.generation.map.DungeonSymbol;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * An abstract flood fill algorithm. It is not tied to {@link DungeonSymbol}.
 * See {@link DungeonFloodFill} for that.
 * 
 * @author smelC
 */
public abstract class FloodFill {

	protected final int width;
	protected final int height;

	/**
	 * A fresh instance.
	 * 
	 * @param width
	 *            The dungeon's width.
	 * @param height
	 *            The dungeon's height
	 */
	public FloodFill(int width, int height) {
		this.width = width;
		this.height = height;
	}

	/**
	 * @param rng
	 * @param x
	 *            Where to start.
	 * @param y
	 *            Where to start.
	 * @param objective
	 *            How to control when to stop the floodfill.
	 * @param buf
	 *            Where to store the result, or {@code null} for this method to
	 *            allocate a fresh set.
	 * @return The flood or null (or empty) if none.
	 */
	public /* @Nullable */ LinkedHashSet<Coord> flood(RNG rng, int x, int y, IFloodObjective objective,
			/* @Nullable */ LinkedHashSet<Coord> buf) {
		final Coord start = Coord.get(x, y);
		if (!canBeFloodOn(start))
			return buf;
		final LinkedList<Coord> todos = new LinkedList<Coord>();
		final Set<Coord> dones = new HashSet<Coord>();
		final LinkedHashSet<Coord> result = buf == null ? new LinkedHashSet<Coord>() : buf;
		todos.add(start);
		final Direction[] outwards = getOutwards();
		while (!todos.isEmpty() && !objective.isMet()) {
			final Coord head = todos.remove();
			if (!dones.add(head))
				continue;
			assert canBeFloodOn_(head);
			result.add(head);
			objective.record(head);
			if (!objective.isMet()) {
				for (Direction dir : outwards) {
					final Coord neighbor = head.translate(dir);
					if (canBeFloodOn_(neighbor) && !dones.contains(neighbor))
						todos.add(neighbor);
				}
			}
		}
		return result;
	}

	private final boolean canBeFloodOn_(Coord c) {
		return validInDungeon(c) && canBeFloodOn(c);
	}

	/**
	 * The callers guarantees that {@link #validInDungeon(Coord)} holds for
	 * {@code c}
	 */
	protected abstract boolean canBeFloodOn(Coord c);

	protected boolean validInDungeon(Coord c) {
		if (c.x <= 0 || c.y <= 0)
			/* On edge or oob */
			return false;
		if (width - 1 <= c.x || height - 1 <= c.y)
			/* On edge or oob */
			return false;
		return true;
	}

	protected Direction[] getOutwards() {
		return Direction.OUTWARDS;
	}

}
