package com.hgames.rhogue.title;

import com.hgames.rhogue.level.ILevelIdentifier;

/**
 * A skeletal implementation of {@link Title}.
 * 
 * @author smelC
 *
 * @param <I>
 */
public abstract class SkeletalTitle<I extends ILevelIdentifier> implements Title<I> {

	@Override
	public boolean isBefore(I levelID1, I levelID2) {
		return levelID1.equals(levelID2) || isStrictlyBefore(levelID1, levelID2);
	}

}
