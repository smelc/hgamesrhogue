package com.hgames.rhogue.generation.corridor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.hgames.rhogue.zone.Rectangle;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;

/**
 * A data structure storing straight corridors.
 * 
 * @author smelC
 * @see CorridorComputer
 */
@Deprecated
public class Corridors {

	/*
	 * Rectangle is a surapproximation of a width 1 corridor. But it is handy,
	 * because it is a Zone. Let's not optimize prematurely. If optimization is
	 * required, a corridor-specific Zone should be written.
	 */
	protected final List<Rectangle> horizontalCorridors;
	protected final List<Rectangle> verticalCorridors;

	protected transient List<Rectangle> all;

	/**
	 * @param horizontalCorridors
	 * @param verticalCorridors
	 */
	public Corridors(List<Rectangle> horizontalCorridors, List<Rectangle> verticalCorridors) {
		this.horizontalCorridors = horizontalCorridors;
		this.verticalCorridors = verticalCorridors;
	}

	/**
	 * @param horizontal
	 *            Whether to include horizontal corridors.
	 * @param vertical
	 *            Whether to include vertical corridors.
	 * @return Corridors stored in {@code this}. Can be an internal reference to
	 *         {@code this}'s state, maybe not.
	 */
	public List<Rectangle> getCorridors(boolean horizontal, boolean vertical) {
		if (vertical) {
			if (horizontal) {
				if (all == null) {
					all = new ArrayList<Rectangle>(size(true, true));
					all.addAll(verticalCorridors);
					all.addAll(horizontalCorridors);
				}
				return all;
			} else
				return verticalCorridors;
		} else
			return horizontal ? horizontalCorridors : Collections.<Rectangle> emptyList();
	}

	/**
	 * @param horizontal
	 *            Whether to consider horizontal corridors.
	 * @param vertical
	 *            Whether to consider vertical corridors.
	 * @param acc
	 *            Where to add computed cells, or null for this method to
	 *            allocated a {@link HashSet} and return it.
	 * @return The coordinates that are entries to corridors, i.e.
	 *         {@link Direction#LEFT} and {@link Direction#RIGHT} cells of
	 *         horizontal corridors and {@link Direction#UP} and
	 *         {@link Direction#DOWN} cells of vertical corridors.
	 */
	public Collection<Coord> getCorridorsDoorway(boolean horizontal, boolean vertical,
			/* @Nullable */ Collection<Coord> acc) {
		final Collection<Coord> result;
		if (acc == null) {
			final int size = (vertical ? verticalCorridors.size() * 2 : 0)
					+ (horizontal ? horizontalCorridors.size() * 2 : 0);
			result = new HashSet<Coord>(size);
		} else
			result = acc;
		if (vertical)
			addCorridorsDoorway(verticalCorridors, false, result);
		if (horizontal)
			addCorridorsDoorway(horizontalCorridors, true, result);
		return result;
	}

	/**
	 * @return All corridors in {@code this}. The returned list should not be
	 *         modified , it belongs to {@code this}; not the caller.
	 */
	public final List<Rectangle> getAll() {
		return getCorridors(true, true);
	}

	/**
	 * @return The number of coordinates in {@code this}. It is the size of the
	 *         set returned by {@link #getFatSet(Set)}.
	 */
	public final int getNbCoords() {
		int result = 0;
		for (Zone r : horizontalCorridors)
			result += r.size();
		for (Zone r : verticalCorridors)
			result += r.size();
		return result;
	}

	/**
	 * @param acc
	 *            Where to put the result, or null for this method to allocate a
	 *            set.
	 * @return All coordinates in corridors (the returned set is of size
	 *         {@link #getNbCoords()}).
	 */
	public final Set<Coord> getFatSet(/* @Nullable */ Set<Coord> acc) {
		final Set<Coord> result = acc == null ? new HashSet<Coord>(getNbCoords()) : acc;
		for (Zone r : horizontalCorridors)
			result.addAll(r.getAll());
		for (Zone r : verticalCorridors)
			result.addAll(r.getAll());
		return result;
	}

	/**
	 * @param includeHorizontal
	 *            Whether to count horizontal corridors.
	 * @param includeVertical
	 *            Whether to count vertical corridors.
	 * @return The number of corridors stored in {@code this}.
	 */
	public int size(boolean includeHorizontal, boolean includeVertical) {
		int result = 0;
		if (includeVertical)
			result += verticalCorridors.size();
		if (includeHorizontal)
			result += horizontalCorridors.size();
		return result;
	}

	@Override
	public String toString() {
		final int size = size(true, true);
		return size + " corridor" + (size == 1 ? "" : "s");
	}

	private void addCorridorsDoorway(List<Rectangle> corridors, boolean horizontal, Collection<Coord> acc) {
		final int nbc = corridors.size();
		for (int i = 0; i < nbc; i++) {
			final Rectangle corridor = corridors.get(i);
			if (horizontal) {
				final int x1 = corridor.x(true);
				final int x2 = corridor.x(false);
				final int y = corridor.y(true);
				assert y == corridor.y(false);
				acc.add(Coord.get(x1 + Direction.LEFT.deltaX, y));
				acc.add(Coord.get(x2 + Direction.RIGHT.deltaX, y));
			} else {
				final int y1 = corridor.y(true);
				final int y2 = corridor.y(false);
				final int x = corridor.x(true);
				assert x == corridor.x(false);
				acc.add(Coord.get(x, y1 + Direction.UP.deltaY));
				acc.add(Coord.get(x, y2 + Direction.DOWN.deltaY));
			}
		}
	}
}
