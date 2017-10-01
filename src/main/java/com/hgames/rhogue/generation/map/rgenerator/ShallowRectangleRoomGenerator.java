package com.hgames.rhogue.generation.map.rgenerator;

import com.hgames.lib.Ints;
import com.hgames.rhogue.generation.map.Dungeon;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * A room generator that produces a rectangular room whose internal border
 * complementary is chasm, except for the room's center.
 * 
 * @author smelC
 */
public class ShallowRectangleRoomGenerator extends AbstractShallowRoomGenerator {

	@Override
	protected Zone getZoneToCarve(Dungeon dungeon, Coord translation, int maxWidth, int maxHeight) {
		if (maxWidth < 3 || maxHeight < 3)
			/* Cannot do */
			return null;
		final int width = maxWidth - (Ints.isEven(maxWidth) ? 1 : 0);
		final int height = maxHeight - (Ints.isEven(maxHeight) ? 1 : 0);
		assert 3 <= width && Ints.isOdd(width);
		assert 3 <= height && Ints.isOdd(height);
		final Zone result = new Rectangle.Impl(Coord.get(0, 0), width, height);
		return result;
	}

}
