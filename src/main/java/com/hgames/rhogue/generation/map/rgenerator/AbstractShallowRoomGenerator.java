package com.hgames.rhogue.generation.map.rgenerator;

import java.util.List;

import com.hgames.lib.collection.list.Lists;
import com.hgames.rhogue.generation.map.DungeonGenerator.ZoneType;
import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.generation.map.RoomComponent;
import com.hgames.rhogue.zone.ListZone;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * An abstract room generator to generate a room where only the internal border
 * and the center have been kept.
 * 
 * @author smelC
 */
public abstract class AbstractShallowRoomGenerator extends SkeletalRoomGenerator {

	protected final IRoomGenerator delegate;

	/**
	 * @param delegate
	 *            How to build the room. This generator is used to generate the full
	 *            room, and then it is carved with
	 *            {@link #getZoneToCarve(Zone, IRNG, RoomComponent, Coord, int, int)}.
	 */
	protected AbstractShallowRoomGenerator(IRoomGenerator delegate) {
		this.delegate = delegate;
	}

	@Override
	public /* @Nullable */ Zone generate(IRNG rng, RoomComponent component, Coord translation, int maxWidth,
			int maxHeight) {
		final Zone toCarve = delegate.generate(rng, component, translation, maxWidth, maxHeight);
		if (toCarve == null)
			/* Cannot do */
			return null;
		final Zone carving = getZoneToCarve(toCarve, rng, component, translation, maxWidth, maxHeight);
		if (carving == null)
			/* Cannot do */
			return null;
		assert toCarve.contains(carving) : "Initial zone (" + toCarve + ") doesn't completely contain the carved zone: "
				+ carving;
		final List<Coord> result = Lists.newArrayList(toCarve.iterator(), toCarve.size());
		final boolean change = result.removeAll(carving.getAll(false));
		if (change) {
			final Zone chasm = carving.translate(translation);
			// Replace carved zone by chasms
			component.addZone(this, chasm, null, ZoneType.CHASM, DungeonSymbol.CHASM);
			return new ListZone(result);
		} else {
			/* This is likely not intended */
			assert false;
			return toCarve;
		}
	}

	/**
	 * @param full
	 *            The zone to carve
	 * @param rng
	 *            The corresponding parameter given at {@link #generate}
	 * @param component
	 *            The corresponding parameter given at {@link #generate}
	 * @param translation
	 *            The corresponding parameter given at {@link #generate}
	 * @param maxWidth
	 *            The corresponding parameter given at {@link #generate}
	 * @param maxHeight
	 *            The corresponding parameter given at {@link #generate}
	 * @return The zone to carve within {@code full}, or null if it cannot be done.
	 */
	protected abstract /* @Nullable */ Zone getZoneToCarve(Zone full, IRNG rng, RoomComponent component,
			Coord translation, int maxWidth,
			int maxHeight);

}
