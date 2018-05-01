package com.hgames.rhogue.zone;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.DungeonUtility;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * Abstraction over a list of {@link Coord}.
 * 
 * <p>
 * The correct method to implement a {@link Zone} efficiently is to first try
 * implementing the interface directly, looking at each method and thinking
 * whether you can do something smart for it. Once you've inspected all methods,
 * then extend {@link Zone.Skeleton} (instead of Object in the first place) so
 * that it'll fill for you the methods for which you cannot provide a smart
 * implementation.
 * </p>
 * 
 * <p>
 * Zones are {@link Serializable}, but serialization doesn't change the internal
 * representation. I find that overzealous for a simple interface.
 * </p>
 * 
 * @author smelC
 */
public interface Zone extends Serializable, Iterable<Coord> {

	/**
	 * @return Whether this zone is empty.
	 */
	boolean isEmpty();

	/**
	 * @return The number of cells that this zone contains (the size
	 *         {@link #getAll(boolean)}).
	 */
	int size();

	/**
	 * @param x
	 * @param y
	 * @return Whether this zone contains the coordinate (x,y).
	 */
	boolean contains(int x, int y);

	/**
	 * @param c
	 * @return Whether this zone contains {@code c}.
	 */
	boolean contains(Coord c);

	/**
	 * @param other
	 * @return true if all cells of {@code other} are in {@code this}.
	 */
	boolean contains(Zone other);

	/**
	 * @param other
	 * @return true if {@code this} and {@code other} have a common cell.
	 */
	boolean intersectsWith(Zone other);

	/**
	 * @return The approximate center of this zone, or null if this zone is empty.
	 */
	/* @Nullable */ Coord getCenter();

	/**
	 * @return The distance between the leftmost cell and the rightmost cell, or
	 *         anything negative if {@code this} zone is empty; may be 0 if all
	 *         cells are in one vertical line.
	 */
	int getWidth();

	/**
	 * @return The distance between the topmost cell and the lowest cell, or
	 *         anything negative if {@code this} zone is empty; may be 0 if all
	 *         cells are in one horizontal line.
	 */
	int getHeight();

	/**
	 * @return The approximation of the zone's diagonal, using {@link #getWidth()}
	 *         and {@link #getHeight()}.
	 */
	double getDiagonal();

	/**
	 * @param smallestOrBiggest
	 *            if true, finds the smallest x-coordinate value; if false, finds
	 *            the biggest.
	 * @return The x-coordinate of the Coord within {@code this} that has the
	 *         smallest (or biggest) x-coordinate. Or -1 if the zone is empty.
	 */
	int x(boolean smallestOrBiggest);

	/**
	 * @param smallestOrBiggest
	 *            if true, finds the smallest y-coordinate value; if false, finds
	 *            the biggest.
	 * @return The y-coordinate of the Coord within {@code this} that has the
	 *         smallest (or biggest) y-coordinate. Or -1 if the zone is empty.
	 */
	int y(boolean smallestOrBiggest);

	/**
	 * If you solely inspect the returned list (i.e. you don't need to mutate it),
	 * give {@code false} to save allocations.
	 * 
	 * @param fresh
	 *            {@code true} to request a fresh mutable list.
	 * @return All cells in this zone
	 */
	List<Coord> getAll(boolean fresh);

	/**
	 * @param rng
	 * @return A random cell within {@link #getAll(boolean)}, or null if this zone
	 *         is empty
	 */
	/* @Nullable */ Coord getRandom(IRNG rng);

	/**
	 * @param c
	 * @return {@code this} shifted by {@code (c.x,c.y)}
	 */
	Zone translate(Coord c);

	/**
	 * @param x
	 * @param y
	 * @return {@code this} shifted by {@code (x,y)}
	 */
	Zone translate(int x, int y);

	/**
	 * @return Cells in {@code this} that are adjacent to a cell not in
	 *         {@code this}. May be fresh, may be not.
	 */
	List<Coord> getInternalBorder();

	/**
	 * @return Cells adjacent to {@code this} that aren't in {@code this}. May be
	 *         fresh, may be not.
	 */
	List<Coord> getExternalBorder();

	/**
	 * @return A variant of {@code this} where cells adjacent to {@code this}
	 *         (orthogonally or diagonally) have been added (i.e. it's {@code this}
	 *         plus {@link #getExternalBorder()}).
	 */
	Zone extend();

	/**
	 * @return A variant of {@code this} where {@link #getInternalBorder()} has been
	 *         removed.
	 */
	Zone shrink();

	/**
	 * @param other
	 * @return A variant of {@code this}, or {@code this} itself; where
	 *         {@code other} has been added.
	 */
	Zone union(Zone other);

	/**
	 * @return The zone being delegated to, or {@code this} if it's not a delegating
	 *         zone.
	 */
	Zone getDelegate();

	/**
	 * A convenience partial implementation. Please try for all new implementations
	 * of {@link Zone} to be subtypes of this class. It usually prove handy at some
	 * point to have a common superclass.
	 *
	 * @author smelC
	 */
	abstract class Skeleton implements Zone {

		private transient Coord center = null;
		private transient int width = -2;
		private transient int height = -2;

		private static final long serialVersionUID = 4436698111716212256L;

		@Override
		/* Convenience implementation, feel free to override */
		public int size() {
			return getAll(false).size();
		}

		@Override
		/* Convenience implementation, feel free to override */
		public boolean contains(int x, int y) {
			for (Coord in : this) {
				if (in.x == x && in.y == y)
					return true;
			}
			return false;
		}

		@Override
		/* Convenience implementation, feel free to override */
		public boolean contains(Coord c) {
			return contains(c.x, c.y);
		}

		@Override
		/* Convenience implementation, feel free to override */
		public boolean contains(Zone other) {
			for (Coord c : other) {
				if (!contains(c))
					return false;
			}
			return true;
		}

		@Override
		public boolean intersectsWith(Zone other) {
			final int tsz = size();
			final int osz = other.size();
			final Iterable<Coord> iteratedOver = tsz < osz ? this : other;
			final Zone other_ = tsz < osz ? other : this;
			for (Coord c : iteratedOver) {
				if (other_.contains(c))
					return true;
			}
			return false;
		}

		@Override
		/*
		 * Convenience implementation, feel free to override, in particular if you can
		 * avoid allocating the list usually allocated by getAll().
		 */
		public Iterator<Coord> iterator() {
			return getAll(false).iterator();
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public int getWidth() {
			if (width == -2)
				width = isEmpty() ? -1 : x(false) - x(true);
			return width;
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public int getHeight() {
			if (height == -2)
				height = isEmpty() ? -1 : y(false) - y(true);
			return height;
		}

		@Override
		public double getDiagonal() {
			final int w = getWidth();
			final int h = getHeight();
			return Math.sqrt((w * w) + (h * h));
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public int x(boolean smallestOrBiggest) {
			return smallestOrBiggest ? smallest(true) : biggest(true);
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public int y(boolean smallestOrBiggest) {
			return smallestOrBiggest ? smallest(false) : biggest(false);
		}

		@Override
		/* Convenience implementation, feel free to override. */
		/*
		 * A possible enhancement would be to check that the center is within the zone,
		 * and if not to return the coord closest to the center, that is in the zone .
		 */
		public /* @Nullable */ Coord getCenter() {
			if (center == null) {
				/* Need to compute it */
				if (isEmpty())
					return null;
				int x = 0, y = 0;
				float nb = 0;
				for (Coord c : this) {
					x += c.x;
					y += c.y;
					nb++;
				}
				/* Remember it */
				center = Coord.get(Math.round(x / nb), Math.round(y / nb));
			}
			return center;
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public Coord getRandom(IRNG rng) {
			final List<Coord> all = getAll(false);
			return all.isEmpty() ? null : rng.getRandomElement(all);
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public Zone translate(Coord c) {
			return translate(c.x, c.y);
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public Zone translate(int x, int y) {
			final List<Coord> initial = getAll(false);
			final int sz = initial.size();
			final List<Coord> shifted = new ArrayList<Coord>(sz);
			for (int i = 0; i < sz; i++) {
				final Coord c = initial.get(i);
				shifted.add(Coord.get(c.x + x, c.y + y));
			}
			assert shifted.size() == sz;
			return new ListZone(shifted);
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public List<Coord> getInternalBorder() {
			return size() <= 1 ? getAll(false) : DungeonUtility.border(getAll(false), null);
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public List<Coord> getExternalBorder() {
			final List<Coord> result = new ArrayList<Coord>(size());
			final List<Coord> internalBorder = getInternalBorder();
			final int ibsz = internalBorder.size();
			for (int i = 0; i < ibsz; i++) {
				final Coord b = internalBorder.get(i);
				for (Direction dir : Direction.OUTWARDS) {
					final Coord borderNeighbor = b.translate(dir);
					if (!contains(borderNeighbor))
						result.add(borderNeighbor);
				}
			}
			return result;
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public Zone extend() {
			final List<Coord> list = getAll(true);
			list.addAll(getExternalBorder());
			return new ListZone(list);
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public Zone shrink() {
			final List<Coord> list = getAll(true);
			list.removeAll(getInternalBorder());
			return new ListZone(list);
		}

		@Override
		public Zone getDelegate() {
			return this;
		}

		@Override
		/* Convenience implementation, feel free to override. */
		public Zone union(Zone other) {
			return new ZoneUnion(this, other);
		}

		private int smallest(boolean xOrY) {
			if (isEmpty())
				return -1;
			int min = Integer.MAX_VALUE;
			final List<Coord> all = getAll(false);
			final int nba = all.size();
			for (int i = 0; i < nba; i++) {
				final Coord c = all.get(i);
				final int val = xOrY ? c.x : c.y;
				if (val < min)
					min = val;
			}
			return min;
		}

		private int biggest(boolean xOrY) {
			int max = -1;
			final List<Coord> all = getAll(false);
			final int nba = all.size();
			for (int i = 0; i < nba; i++) {
				final Coord c = all.get(i);
				final int val = xOrY ? c.x : c.y;
				if (max < val)
					max = val;
			}
			return max;
		}
	}
}
