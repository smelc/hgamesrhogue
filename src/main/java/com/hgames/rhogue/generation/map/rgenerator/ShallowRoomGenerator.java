package com.hgames.rhogue.generation.map.rgenerator;

import java.util.List;

import com.hgames.lib.collection.list.Lists;
import com.hgames.rhogue.generation.map.dungeon.RoomComponent;
import com.hgames.rhogue.zone.Zone;
import com.hgames.rhogue.zone.Zones;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * A room generator that produces a room whose internal border complementary is
 * chasm.
 * 
 * @author smelC
 */
public class ShallowRoomGenerator extends AbstractShallowRoomGenerator {

	protected boolean carveCenter;

	/**
	 * A fresh instance
	 * 
	 * @param delegate
	 *            The delegate to build the zone to carve.
	 * @param carveCenter
	 *            Whether the center (a single cell) must be carved too, or left
	 *            unaffected.
	 */
	public ShallowRoomGenerator(IRoomGenerator delegate, boolean carveCenter) {
		super(delegate);
	}

	@Override
	protected /* @Nullable */ Zone getCarving(Zone full, IRNG rng, RoomComponent component, Coord translation,
			int maxWidth, int maxHeight) {
		final Zone shrink = full.shrink();
		if (shrink.isEmpty())
			return null;
		final Coord center = full.getCenter();
		assert shrink.contains(center);
		final List<Coord> result = Lists.newArrayList(shrink.iterator(), shrink.size());
		if (carveCenter)
			result.remove(full.getCenter());
		if (result.isEmpty())
			/* Happens if 'full' is too small */
			return null;
		return Zones.build(result);
	}

}
