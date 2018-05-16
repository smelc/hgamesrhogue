package com.hgames.rhogue.title;

import java.util.Collection;
import java.util.List;

import com.hgames.rhogue.level.ILevelIdentifier;

/**
 * A title, which binds a game to a given list of levels. Use this interface to
 * instantiate your generic game with different campaigns.
 * 
 * @author smelC
 * 
 * @param <I>
 */
public interface Title<I extends ILevelIdentifier> {

	/**
	 * Adds to {@code acc} identifiers of levels that are connected to
	 * {@code levelID}.
	 * 
	 * @param levelID
	 * @param acc
	 * @param before
	 *            Whether to add levels that are before {@code levelID}
	 * @param after
	 *            Whether to add levels that are after {@code levelID}
	 */
	public void addConnections(I levelID, boolean before, Collection<I> acc, boolean after);

	/**
	 * @param levelID1
	 * @param levelID2
	 * @return Whether levelID1 and levelID2 are adjacent in the list of levels.
	 */
	public boolean areConnected(I levelID1, I levelID2);

	/**
	 * @param levelID
	 * @return Whether such a level exists
	 */
	public boolean exists(I levelID);

	/**
	 * @return The game's acronym, such as "dm". May be used in filepaths so avoid
	 *         fancy stuff.
	 */
	public String getAcronym();

	/** @return All possible level identifiers in this game. */
	public List<I> getAllLevelIDs();

	/** @return The name of the game, such as "Dungeon Mercenary" */
	public String getName();

	/**
	 * @param levelID1
	 * @param levelID2
	 * @return Whether levelID1 is before levelID2 or is equal to it.
	 */
	public boolean isBefore(I levelID1, I levelID2);

	/**
	 * @param levelID1
	 * @param levelID2
	 * @return Whether levelID1 is strictly before levelID2
	 */
	public boolean isStrictlyBefore(I levelID1, I levelID2);

}
