package com.hgames.rhogue.generation.map.flood;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.grid.DoerInACircle;
import com.hgames.rhogue.grid.Grids;

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
		if (!canBeFloodOn_(start))
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
		if (result != null)
			postprocess(result);
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

	/**
	 * <ol>
	 * <li>To avoid the fill to be to squarish, let's eat its border <- NOT
	 * NEEDED FINALLY</li>
	 * <li>For every cell in the result, remove it if it is too lonely. This
	 * avoids:
	 * 
	 * <pre>
	 * ######
	 * #~#~## <-
	 * ##~~~#
	 * #~~~~#
	 * ######
	 * </pre>
	 * 
	 * </li>
	 * </ol>
	 * 
	 * @param result
	 */
	protected void postprocess(final Collection<? extends Coord> result) {
		// eatBorders(result);
		removeLonelies(result);
		// // keepBiggestComponent(result);
		if (result.size() < 9)
			/* Not a valid fill */
			result.clear();
	}

	protected void eatBorders(final Collection<? extends Coord> result) {
		/*
		 * XXX We should not eat the border if it's connecting it to walkable
		 * cells.
		 */
		int eaters = (result.size() / 24) + 1;
		Iterator<? extends Coord> it = result.iterator();
		while (it.hasNext() && 0 < eaters) {
			final Coord eatCenter = it.next();
			if (!Grids.isOnBorder(result, eatCenter))
				continue;
			final DoerInACircle doer = new DoerInACircle() {
				@Override
				protected boolean doOnACell(int x, int y) {
					final Coord rmed = Coord.get(x, y);
					result.remove(rmed);
					return result.isEmpty();
				}
			};
			doer.doInACircle(eatCenter.x, eatCenter.y, 2);
			it = result.iterator();
			eaters--;
		}
	}

	protected void removeLonelies(Collection<? extends Coord> result) {
		for (int i = 0; i < Integer.MAX_VALUE; i++) {
			final int effect = removeLonelies0(result);
			if (effect == 0)
				break;
		}
	}

	/** @return The number of cells removed */
	protected int removeLonelies0(Collection<? extends Coord> coords) {
		final Iterator<? extends Coord> it = coords.iterator();
		int result = 0;
		while (it.hasNext()) {
			final Coord c = it.next();
			if (Grids.borderness(coords, c) >= 7) {
				it.remove();
				result++;
			}
		}
		return result;
	}

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
		/* Gives better result than Direction.OUTWARDS */
		return Direction.CARDINALS;
	}

}
