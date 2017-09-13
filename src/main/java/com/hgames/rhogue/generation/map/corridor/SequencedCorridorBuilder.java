package com.hgames.rhogue.generation.map.corridor;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A corridor builder that tries {@link ICorridorBuilder} in sequence, returning
 * the first successfully built corridor.
 * 
 * @author smelC
 */
public class SequencedCorridorBuilder implements ICorridorBuilder {

	protected final ICorridorBuilder[] builders;

	/**
	 * @param builders
	 */
	public SequencedCorridorBuilder(ICorridorBuilder... builders) {
		this.builders = builders;
	}

	@Override
	public /* @Nullable */ Zone build(RNG rng, Coord start, Coord end, Coord[] startEndBuf) {
		for (ICorridorBuilder builder : builders) {
			final Zone z = builder.build(rng, start, end, startEndBuf);
			if (z != null)
				return z;
		}
		return null;
	}

}
