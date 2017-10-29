package com.hgames.rhogue.generation.map.rgenerator;

import java.util.List;

import com.hgames.lib.collection.list.Lists;
import com.hgames.rhogue.generation.map.DungeonGenerator.ZoneType;
import com.hgames.rhogue.zone.ListZone;
import com.hgames.rhogue.zone.Zone;
import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.generation.map.RoomComponent;

import squidpony.squidmath.Coord;

/**
 * An abstract room generator to generate a room where only the internal border
 * and the center have been kept.
 * 
 * @author smelC
 */
public abstract class AbstractShallowRoomGenerator extends SkeletalRoomGenerator {

	// Change implementors so that they deal with it in 'getZoneToCarve'
	@Deprecated
	protected boolean keepCenter = false;

	@Override
	public /* @Nullable */ Zone generate(RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		final Zone zone = getZoneToCarve(component, translation, maxWidth, maxHeight);
		if (zone == null)
			return null;
		final List<Coord> all = Lists.newArrayList(zone.iterator(), zone.size());
		final Zone toRemove = zone.shrink();
		assert zone.contains(toRemove) : "Shallow zone (" + zone + ") doesn't contain the carved zone: " + toRemove;
		final Coord center = zone.getCenter();
		boolean change = false;
		for (Coord c : toRemove) {
			if (!keepCenter || !center.equals(c)) {
				all.remove(c);
				change = true;
			}
		}
		if (change) {
			final Zone chasm = toRemove.translate(translation);
			component.addZone(this, chasm, null, ZoneType.CHASM, DungeonSymbol.CHASM);
			return new ListZone(all);
		} else {
			/* This is likely not intended */
			assert false;
			return zone;
		}
	}

	protected abstract Zone getZoneToCarve(RoomComponent component, Coord translation, int maxWidth, int maxHeight);

}
