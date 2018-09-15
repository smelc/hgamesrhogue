package com.hgames.rhogue.title;

import com.hgames.rhogue.level.ILevelIdentifier;

/**
 * A title whose list of levels is a sequence. This interface avoids useless
 * casts by avoiding to use concrete titles and avoids the need to allocate a
 * collection to pass to {@link Title#addConnections}.
 * 
 * @author smelC
 *
 * @param <I>
 */
public interface LinearTitle<I extends ILevelIdentifier> extends Title<I> {

	/** @return The first level's identifier */
	public I getFirst();

	/**
	 * @param levelID
	 * @return The level after {@code levelID} or null if none.
	 */
	public /* @Nullable */ I getNext(I levelID);

	/**
	 * @param levelID
	 * @return The level before {@code levelID} or null if none.
	 */
	public /* @Nullable */ I getPrevious(I levelID);

	/**
	 * @param levelID
	 * @return Whether {@code levelID} is the first level
	 */
	public boolean isFirst(I levelID);

	/**
	 * @param levelID
	 * @return Whether {@code levelID} is the last level
	 */
	public boolean isLast(I levelID);

	/**
	 * @param levelID1
	 * @param levelID2
	 * @return Whether levelID1 is strictly before levelID2
	 */
	public boolean isStrictlyBefore(I levelID1, I levelID2);

}
