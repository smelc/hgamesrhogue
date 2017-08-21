package com.hgames.rhogue.generation.map;

import java.util.Iterator;
import java.util.Set;

import com.hgames.lib.Ints;
import com.hgames.lib.collection.set.Sets;
import com.hgames.rhogue.zone.SetZone;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * <b>FIXME UNTESTED for now</b>
 * 
 * @author smelC
 */
public class CaveRoomGenerator implements IRoomGenerator {

	protected final RNG rng;

	/**
	 * The degree of caveness, 0 being the minimum (yielding a rectangle room),
	 * 100 being the maximum.
	 */
	protected int caveness = 50;

	/**
	 * @param rng
	 *            The RNG to use.
	 */
	public CaveRoomGenerator(RNG rng) {
		this.rng = rng;
	}

	/**
	 * @param level
	 *            The caveness level to use. Must be in [0, 100].
	 * @return {@code this}.
	 */
	public CaveRoomGenerator setCaveness(int level) {
		if (!Ints.inInterval(0, level, 100))
			throw new IllegalStateException(
					"Excepted caveness level to be with [0, 100]. Received: " + level);
		this.caveness = level;
		return this;
	}

	@Override
	public Zone generate(int maxWidth, int maxHeight) {
		final ConstrainedRectangleRoomGenerator delegate = new ConstrainedRectangleRoomGenerator(rng, maxWidth, maxHeight);
		final Rectangle rectangle = delegate.generate(maxWidth, maxHeight);
		if (rectangle == null) {
			/* Should not happen */
			assert false;
			return null;
		}
		final int rsz = rectangle.size();
		if (rsz <= 4)
			/* Do not shrink it */
			return rectangle;
		final int maxSingleCave = rsz / 4;
		boolean touched = false;
		Set<Coord> copy = null;
		for (Direction diagonal : Direction.DIAGONALS) {
			if (copy != null && copy.size() <= 4)
				/* Do not shrink it anymore */
				break;
			final boolean caveIt = rng.nextInt(101) <= caveness;
			if (!caveIt)
				continue;
			// FIXME CH Continue me
		}
		if (touched) {
			assert copy != null;
			return new SetZone(copy);
		} else
			return rectangle;
	}

	private static Set<Coord> toSet(Rectangle rectangle) {
		final Iterator<Coord> iterator = Rectangle.Utils.cells(rectangle);
		return Sets.newHashSet(iterator, rectangle.size());
	}
}
