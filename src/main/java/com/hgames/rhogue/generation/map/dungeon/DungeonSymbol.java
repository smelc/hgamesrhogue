package com.hgames.rhogue.generation.map.dungeon;

/**
 * Possible symbols in {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public enum DungeonSymbol {
	/** A pit or chasm in brogue terms */
	CHASM,
	/** Dangerous water */
	DEEP_WATER,
	/** A door */
	DOOR,
	/** A floor */
	FLOOR,
	/** Grass through which the rogue sees */
	GRASS,
	/** Grass through which the rogue doesn't see */
	HIGH_GRASS,
	/** Water in which you can walk without any penaly */
	SHALLOW_WATER,
	/** The stair to the next level */
	STAIR_DOWN,
	/** The stair to the previous level */
	STAIR_UP,
	/** A wall */
	WALL,
}
