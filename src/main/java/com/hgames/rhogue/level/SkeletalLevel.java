package com.hgames.rhogue.level;

import java.io.Serializable;

import com.hgames.rhogue.grid.IMapCell;

/**
 * A very skeletal implementation of {@link ILevel}.
 * 
 * @author smelC
 * @param <I>
 * @param <MC>
 */
public abstract class SkeletalLevel<I extends ILevelIdentifier, MC extends IMapCell>
		implements ILevel<I, MC>, Serializable {

	protected final I identifier;

	private static final long serialVersionUID = 2110903870083959154L;

	protected SkeletalLevel(I identifier) {
		this.identifier = identifier;
	}

	@Override
	public final I getIdentifier() {
		return identifier;
	}

}
