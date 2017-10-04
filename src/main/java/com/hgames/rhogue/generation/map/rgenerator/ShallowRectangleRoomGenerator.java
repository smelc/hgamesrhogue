package com.hgames.rhogue.generation.map.rgenerator;

import com.hgames.lib.Ints;
import com.hgames.rhogue.generation.map.RoomComponent;
import com.hgames.rhogue.zone.SingleCellZone;

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
	protected Zone getZoneToCarve(RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		if (maxWidth < 3 || maxHeight < 3)
			/* Cannot do */
			return null;
		final int width = maxWidth - (Ints.isEven(maxWidth) ? 1 : 0);
		final int height = maxHeight - (Ints.isEven(maxHeight) ? 1 : 0);
		assert 3 <= width && Ints.isOdd(width);
		assert 3 <= height && Ints.isOdd(height);
		final Zone result;
		final Coord zerozero = Coord.get(0, 0);
		if (width == 1 && height == 1)
			result = new SingleCellZone(zerozero);
		else
			result = new Rectangle.Impl(zerozero, width, height);
		return result;
	}

}
