package com.hgames.rhogue.generation.map;

import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;

/**
 * A component of {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public interface GeneratorComponent {

	/**
	 * @param gen
	 *            The running generator, that is calling {@code this}.
	 * @param gdata
	 * @return An implementation-dependent flag.
	 */
	public boolean generate(DungeonGenerator gen, GenerationData gdata);

}
