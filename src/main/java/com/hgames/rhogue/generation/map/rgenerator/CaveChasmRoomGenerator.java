package com.hgames.rhogue.generation.map.rgenerator;

import com.hgames.rhogue.generation.map.draw.ConsoleDungeonDrawer;
import com.hgames.rhogue.generation.map.dungeon.DungeonSymbolDrawer;
import com.hgames.rhogue.generation.map.dungeon.RoomComponent;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * A generator whose rooms are rectangular but the inside is chasm, with a cave like shape like this
 * one: <a href="https://i.imgur.com/ez7zHQl.png">example</a>.
 * 
 * @author smelC
 */
public class CaveChasmRoomGenerator extends AbstractShallowRoomGenerator {

	/* A field to minimize allocations */
	private /*@Nullable*/ CaveRoomGenerator caves;

	/** A fresh instance */
	public CaveChasmRoomGenerator() {
		super(new RectangleRoomGenerator());
	}

	@Override
	public Zone generate(IRNG rng, RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		final RectangleRoomGenerator rg = ((RectangleRoomGenerator) delegate);
		/* We want the zone being carved to be at maximum size */
		rg.setMinSideSize(maxWidth, true);
		rg.setMinSideSize(maxHeight, false);
		rg.setMaxSideSize(maxWidth, true);
		rg.setMaxSideSize(maxHeight, false);
		return super.generate(rng, component, translation, maxWidth, maxHeight);
	}

	@Override
	protected Zone getCarving(Zone full, IRNG rng, RoomComponent component, Coord translation, int maxWidth,
			int maxHeight) {
		final int caveWidth = maxWidth - 1;
		final int caveHeight = maxHeight - 1;
		if (caveWidth <= CaveRoomGenerator.MIN_SIDE_SIZE || caveHeight <= CaveRoomGenerator.MIN_SIDE_SIZE)
			return null;

		if (caves == null) caves = new CaveRoomGenerator();
		return caves.generate(rng, caveWidth, caveHeight, new ConsoleDungeonDrawer(new DungeonSymbolDrawer()));
	}
}
