package com.hgames.rhogue.generation.corridor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * A data structure storing straight corridors.
 * 
 * @author smelC
 * @see CorridorComputer
 */
public class Corridors {

	/*
	 * Rectangle is a surapproximation of a width 1 corridor. But it is handy,
	 * because it is a Zone. Let's not optimize prematurely. If optimization is
	 * required, a corridor-specific Zone should be written.
	 */
	protected final List<Rectangle> verticalCorridors;
	protected final List<Rectangle> horizontalCorridors;

	protected transient List<Rectangle> all;

	/**
	 * @param verticalCorridors
	 * @param horizontalCorridors
	 */
	public Corridors(List<Rectangle> verticalCorridors, List<Rectangle> horizontalCorridors) {
		this.verticalCorridors = verticalCorridors;
		this.horizontalCorridors = horizontalCorridors;
	}

	/**
	 * @param vertical
	 *            Whether to include vertical corridors.
	 * @param horizontal
	 *            Whether to include horizontal corridors.
	 * @return Corridors stored in {@code this}. Can be an internal reference to
	 *         {@code this}'s state, maybe not.
	 */
	public List<Rectangle> getCorridors(boolean vertical, boolean horizontal) {
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
	 * @return All corridors in {@code this}.
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
		for (Zone r : verticalCorridors)
			result += r.size();
		for (Zone r : horizontalCorridors)
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
		for (Zone r : verticalCorridors)
			result.addAll(r.getAll());
		for (Zone r : horizontalCorridors)
			result.addAll(r.getAll());
		return result;
	}

	/**
	 * @param includeVertical
	 *            Whether to count vertical corridors.
	 * @param includeHorizontal
	 *            Whether to count horizontal corridors.
	 * @return The number of corridors stored in {@code this}.
	 */
	public int size(boolean includeVertical, boolean includeHorizontal) {
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

}
