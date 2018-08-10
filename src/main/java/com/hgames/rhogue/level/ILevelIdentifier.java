package com.hgames.rhogue.level;

import java.io.Serializable;

/**
 * @author smelC
 */
public interface ILevelIdentifier extends Serializable {

	@Override
	public boolean equals(Object other);

	@Override
	public int hashCode();

}
