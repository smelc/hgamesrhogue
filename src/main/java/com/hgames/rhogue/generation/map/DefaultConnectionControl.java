package com.hgames.rhogue.generation.map;

import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.connection.IConnectionControl;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;

import squidpony.squidgrid.zone.Zone;

/**
 * @author smelC
 */
public class DefaultConnectionControl implements IConnectionControl {

	/** A singleton of this class */
	public static final IConnectionControl INSTANCE = new DefaultConnectionControl();

	@Override
	public boolean forceDoor(DungeonGenerator gen, Dungeon dungeon, IRoomGenerator rg, Zone z) {
		return rg.getMaxConnections() == 1;
	}

	@Override
	public boolean acceptsConnection(DungeonGenerator gen, Dungeon dungeon, IRoomGenerator gen1, Zone z1,
			IRoomGenerator gen2, Zone z2) {
		if (!validGenerator(gen.logger, gen1) || !validGenerator(gen.logger, gen2))
			/* Be nice, it's not critical */
			return true;
		final int max1 = gen1.getMaxConnections();
		final int max2 = gen2.getMaxConnections();
		if (max1 == Integer.MAX_VALUE && max2 == Integer.MAX_VALUE)
			return true;
		if (max1 <= Dungeons.getNumberOfConnections(dungeon, z1))
			return false;
		if (max2 <= Dungeons.getNumberOfConnections(dungeon, z2))
			return false;
		return true;
	}

	protected boolean validGenerator(ILogger logger, IRoomGenerator rg) {
		assert rg != null : "No " + IRoomGenerator.class.getSimpleName();
		if (rg == null && logger != null && logger.isErrEnabled())
			logger.errLog(Tags.GENERATION, "No " + IRoomGenerator.class.getSimpleName());
		return rg != null;
	}

}
