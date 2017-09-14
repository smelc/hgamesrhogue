package com.hgames.rhogue.generation.map;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * A function {@code Coord=>Zone}.
 * 
 * @author smelC
 */
public interface ICellToZone {

	/**
	 * @param c
	 * @return The zone related to {@code c}. Usually it is the zone containing
	 *         {@code c} but implementors can do whatever they want.
	 */
	public /* @Nullable */ Zone get(Coord c);

}
