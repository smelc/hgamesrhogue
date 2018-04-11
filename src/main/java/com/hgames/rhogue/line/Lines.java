package com.hgames.rhogue.line;

import squidpony.squidmath.Bresenham;
import squidpony.squidmath.Coord;

/**
 * @author smelC
 * @see Bresenham
 */
public class Lines {

	/**
	 * @param src
	 * @param includeSrc
	 *            Whether to include {@code src} in the result.
	 * @param dest
	 * @param includeDest
	 *            Whether to include {@code dest} in the result.
	 * @return The line from {@code src} to {@code dest}
	 */
	public static Coord[] lineFrom(Coord src, boolean includeSrc, Coord dest, boolean includeDest) {
		final Coord[] line = Bresenham.line2D_(src, dest);
		assert line[0].equals(src);
		assert line[line.length - 1].equals(dest);
		if (includeSrc && includeDest)
			return line;
		int length = line.length;
		if (!includeSrc)
			length--;
		if (!includeDest)
			length--;
		final Coord[] result = new Coord[length];
		System.arraycopy(line, includeSrc ? 0 : 1, result, 0, result.length);
		return result;
	}

}
