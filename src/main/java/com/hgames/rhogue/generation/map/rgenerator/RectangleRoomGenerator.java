package com.hgames.rhogue.generation.map.rgenerator;

import com.hgames.rhogue.generation.map.RoomComponent;
import com.hgames.rhogue.zone.Rectangle;

import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A room generator that generates simple rectangle rooms.
 * 
 * @author smelC
 */
public class RectangleRoomGenerator extends SkeletalRoomGenerator {

	/** A fresh instance */
	public RectangleRoomGenerator() {
	}

	@Override
	public Rectangle generate(RNG rng, RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		assert 0 < maxWidth;
		assert 0 < maxHeight;
		/* Irrelevant or honored */
		assert !hasSideSizeConstraint(false, true) || maxWidth <= getMaxSideSize(true);
		/* Irrelevant or honored */
		assert !hasSideSizeConstraint(false, false) || maxHeight <= getMaxSideSize(false);

		/* Avoid generating corridors (width or height == 1) if possible */
		int minw = maxWidth == 1 ? 1 : 2;
		if (hasSideSizeConstraint(true, true)) {
			final int minWidth = getMinSideSize(true);
			assert 0 <= minWidth;
			if (minw < minWidth)
				minw = minWidth;
		}
		assert !hasSideSizeConstraint(true, true) || minw <= getMinSideSize(true);
		int minh = maxHeight == 1 ? 1 : 2;
		if (hasSideSizeConstraint(true, false)) {
			final int minHeight = getMinSideSize(false);
			if (minh < minHeight)
				minh = minHeight;
		}
		assert !hasSideSizeConstraint(true, false) || minh <= getMinSideSize(false);
		final int w = rng.between(minw, maxWidth + 1);
		final int h = rng.between(minh, maxHeight + 1);
		return new Rectangle.Impl(Coord.get(0, 0), w, h);
	}

}
