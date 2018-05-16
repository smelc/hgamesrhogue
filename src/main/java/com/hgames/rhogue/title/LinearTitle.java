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

}
