package com.hgames.rhogue.generation.map;

import squidpony.squidgrid.zone.Zone;

/**
 * A room generator, i.e. a function that knows out to generate a {@link Zone}
 * that fit within a rectangle.
 * 
 * @author smelC
 */
public interface IRoomGenerator {

	/**
	 * @param maxWidth
	 *            The width of the rectangle in which the returned zone must
	 *            fit.
	 * @param maxHeight
	 *            The height of the rectangle in which the returned zone must
	 *            fit.
	 * @return A zone such that
	 *         {@code new Rectangle(Coord.get(0, 0), maxWidth, maxHeight).contains(result)}
	 *         holds. The returned zone must be (0,0)-based, i.e. its top left
	 *         coordinate may at most be (0, 0) (recall that (0,0) denotes the
	 *         top left coordinate in SquidLib's conventions).
	 * 
	 *         <p>
	 *         Or null if the size constraint cannot be honored.
	 *         </p>
	 */
	public /* @Nullable */ Zone generate(int maxWidth, int maxHeight);

}
