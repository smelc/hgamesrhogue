package com.hgames.rhogue.generation.map;

import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;

/**
 * A component of {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public interface DungeonGeneratorComponent {

	/**
	 * @param gen
	 *            The running generator, that is calling {@code this}.
	 * @param gdata
	 * @return Whether the dungeon is valid and generation should continue.
	 */
	public boolean generate(DungeonGenerator gen, GenerationData gdata);

}
