package com.hgames.rhogue.generation.map.corridor;

import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * How to build corridors.
 * 
 * @author smelC
 */
public interface ICorridorBuilder {

	/**
	 * @param rng
	 *            The RNG to use.
	 * @param start
	 *            The bridge's footstep (won't be included in the result).
	 * @param end
	 *            The bridge's endstep (won't be included in the result).
	 * @param startEndBuf
	 *            Where to set the corridor's only cell that is cardinally adjacent
	 *            to {@code start} and the corridor's only cell that is cardinally
	 *            adjacent to {@code end}, or null. Must be of length >= 2.
	 * @return A zone connecting {@code start} and {@code end}. It doesn't contain
	 *         neither {@code start} nor {@code end}.
	 */
	public Zone build(RNG rng, Coord start, Coord end, /* @Nullable */ Coord[] startEndBuf);

}
