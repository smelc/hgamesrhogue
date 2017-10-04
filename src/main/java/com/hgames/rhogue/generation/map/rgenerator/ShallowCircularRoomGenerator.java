package com.hgames.rhogue.generation.map.rgenerator;

import com.hgames.rhogue.generation.map.RoomComponent;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A room generator that produces a circular room whose internal border
 * complementary is chasm, except for the room's center.
 * 
 * @author smelC
 */
public class ShallowCircularRoomGenerator extends AbstractShallowRoomGenerator {

	protected final CircularRoomGenerator delegate;

	/**
	 * @param rng
	 */
	public ShallowCircularRoomGenerator(RNG rng) {
		this.delegate = new CircularRoomGenerator(rng);
		this.keepCenter = false;
	}

	@Override
	protected Zone getZoneToCarve(RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		final Zone result = delegate.generate(component, translation, maxWidth, maxHeight);
		return result;
	}

}
