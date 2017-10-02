package com.hgames.rhogue.generation.map.rgenerator;

import java.util.List;

import com.hgames.lib.collection.list.Lists;
import com.hgames.rhogue.generation.map.Dungeon;

import squidpony.squidgrid.zone.ListZone;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * An abstract room generator to generate a room where only the internal border
 * and the center have been kept.
 * 
 * @author smelC
 */
public abstract class AbstractShallowRoomGenerator extends SkeletalRoomGenerator {

	protected boolean keepCenter = true;

	@Override
	public /* @Nullable */ Zone generate(Dungeon dungeon, Coord translation, int maxWidth, int maxHeight) {
		final Zone zone = getZoneToCarve(dungeon, translation, maxWidth, maxHeight);
		if (zone == null)
			return null;
		final List<Coord> all = Lists.newArrayList(zone.iterator(), zone.size());
		final Zone toRemove = zone.shrink();
		final Coord center = zone.getCenter();
		boolean change = false;
		for (Coord c : toRemove) {
			if (!keepCenter || !center.equals(c)) {
				final boolean rmed = all.remove(c);
				if (rmed)
					System.out.println("Removed " + c.add(translation));
				change = true;
			}
		}
		if (change) {
			final Zone result = new ListZone(all);
			return result;
		} else {
			/* This is likely not intended */
			assert false;
			return zone;
		}
	}

	protected abstract Zone getZoneToCarve(Dungeon dungeon, Coord translation, int maxWidth, int maxHeight);

}
