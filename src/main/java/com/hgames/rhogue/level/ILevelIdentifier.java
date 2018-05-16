package com.hgames.rhogue.level;

import java.io.Serializable;

/**
 * @author smelC
 */
public interface ILevelIdentifier extends Serializable {

	/**
	 * @return Whether this is the first level
	 */
	public boolean isFirst();

	@Override
	public boolean equals(Object other);

	@Override
	public int hashCode();

}
