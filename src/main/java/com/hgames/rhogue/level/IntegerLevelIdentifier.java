package com.hgames.rhogue.level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Integer-based level identifiers, the first level being at "depth" 1 (see
 * {@link #isFirst()}).
 * 
 * @author smelC
 */
public final class IntegerLevelIdentifier implements ILevelIdentifier {

	private final Integer depth;

	private static final long serialVersionUID = 2241334832120655823L;

	/** A buffer to avoid clients to have to pool */
	private static List<IntegerLevelIdentifier> BUFFER;

	/**
	 * @param depth
	 */
	protected IntegerLevelIdentifier(Integer depth) {
		this.depth = depth;
	}

	@Override
	public boolean isFirst() {
		return depth.intValue() == 1;
	}

	/** @return The level's depth */
	public Integer getDepth() {
		return depth;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((depth == null) ? 0 : depth.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntegerLevelIdentifier other = (IntegerLevelIdentifier) obj;
		if (depth == null) {
			if (other.depth != null)
				return false;
		} else if (!depth.equals(other.depth))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(depth);
	}

	/**
	 * @param i
	 * @return The identifier corresponding to {@code i}
	 */
	public static IntegerLevelIdentifier get(Integer i) {
		return ILICache.get(i);
	}

	/** @return A buffer for {@link IntegerLevelIdentifier}s. */
	public List<IntegerLevelIdentifier> getBuffer() {
		if (BUFFER == null)
			BUFFER = new ArrayList<IntegerLevelIdentifier>();
		else
			BUFFER.clear();
		return BUFFER;
	}

	/**
	 * @author smelC
	 */
	public static class ILICache {

		private static Map<Integer, IntegerLevelIdentifier> cache;

		protected static IntegerLevelIdentifier get(Integer depth) {
			if (cache == null)
				cache = new HashMap<Integer, IntegerLevelIdentifier>();
			if (!cache.containsKey(depth))
				cache.put(depth, new IntegerLevelIdentifier(depth));
			return cache.get(depth);
		}

	}

}
