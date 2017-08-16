package com.hgames.rhogue.generation.map;

import com.hgames.lib.Ints;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A room generator that generate rectangle rooms.
 * 
 * @author smelC
 */
public class ConstrainedRectangleRoomGenerator implements IRoomGenerator {

	protected final RNG rng;

	protected int minWidth = 1;
	protected int maxWidth = Integer.MAX_VALUE;

	protected int minHeight = 1;
	protected int maxHeight = Integer.MAX_VALUE;

	/**
	 * @param rng
	 *            The rng to use.
	 */
	public ConstrainedRectangleRoomGenerator(RNG rng) {
		this.rng = rng;
	}

	/**
	 * @param rng
	 *            The rng to use.
	 * @param maxWidth
	 *            The maximum width of generated rectangles.
	 * @param maxHeight
	 *            The maximum height of generated rectangles.
	 */
	public ConstrainedRectangleRoomGenerator(RNG rng, int maxWidth, int maxHeight) {
		this.rng = rng;
		setMaxWidth(maxWidth);
		setMaxHeight(maxHeight);
	}

	/**
	 * @param m
	 *            The minimum width of generated rooms.
	 * @return this
	 */
	public ConstrainedRectangleRoomGenerator setMinWidth(int m) {
		if (m < 1)
			throw new IllegalStateException(
					"Mininum width of a rectangle room cannot be < 1, but received " + m);
		this.minWidth = m;
		return this;
	}

	/**
	 * @param m
	 *            The maximum width of generated rooms.
	 * @return this
	 */
	public ConstrainedRectangleRoomGenerator setMaxWidth(int m) {
		if (m < 1)
			throw new IllegalStateException(
					"Maximum width of a rectangle room cannot be < 1, but received " + m);
		this.maxWidth = m;
		return this;
	}

	/**
	 * @param m
	 *            The minimum width of generated rooms.
	 * @return this
	 */
	public ConstrainedRectangleRoomGenerator setMinHeight(int m) {
		if (m < 1)
			throw new IllegalStateException(
					"Mininum height of a rectangle room cannot be < 1, but received " + m);
		this.minHeight = m;
		return this;
	}

	/**
	 * @param m
	 *            The maximum height of generated rooms.
	 * @return this
	 */
	public ConstrainedRectangleRoomGenerator setMaxHeight(int m) {
		if (m < 1)
			throw new IllegalStateException(
					"Maximum width of a rectangle room cannot be < 1, but received " + m);
		this.maxWidth = m;
		return this;
	}

	@Override
	public /* @Nullable */ Rectangle generate(int maxWidthConstraint, int maxHeightConstraint) {
		final int w;
		if (maxWidthConstraint < minWidth)
			/* Cannot do */
			return null;
		else if (maxWidth <= maxWidthConstraint)
			w = maxWidth;
		else {
			assert Ints.inInterval(minWidth, maxWidthConstraint, maxWidth);
			w = rng.between(minWidth, maxWidthConstraint + 1);
		}
		final int h;
		if (maxHeightConstraint < minHeight)
			/* Cannot do */
			return null;
		else if (maxHeight <= maxHeightConstraint)
			h = maxHeight;
		else {
			assert Ints.inInterval(minHeight, maxHeightConstraint, maxHeight);
			h = rng.between(minHeight, maxHeightConstraint + 1);
		}
		return new Rectangle.Impl(Coord.get(0, 0), w, h);
	}

}
