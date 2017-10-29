package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.DungeonGenerator.ICorridorControl;
import com.hgames.rhogue.generation.map.connection.IConnectionFinder;
import com.hgames.rhogue.generation.map.stair.IStairGenerator;
import com.hgames.rhogue.generation.map.stair.StairGenerator;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;

/**
 * @author smelC
 *
 */
public class StairsComponent implements GeneratorComponent {

	@Override
	public boolean generate(DungeonGenerator gen, GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		final Coord up = generateStair(gen, gdata, true, null, false);
		final ILogger logger = gen.logger;
		if (up == null) {
			warnLog(gen, "Cannot place upward stair");
			return false;
		} else if (logger != null && logger.isInfoEnabled())
			infoLog(gen, "Placed upward stair at " + up);
		assert dungeon.getSymbol(dungeon.upwardStair) == DungeonSymbol.STAIR_UP;
		gen.draw(dungeon);
		final Coord down = generateStair(gen, gdata, false, null, false);
		if (down == null) {
			warnLog(gen, "Cannot place downward stair");
			return false;
		} else if (logger != null && logger.isInfoEnabled())
			infoLog(gen, "Placed downward stair at " + down);
		assert dungeon.getSymbol(dungeon.downwardStair) == DungeonSymbol.STAIR_DOWN;
		gen.draw(dungeon);
		return true;
	}

	/**
	 * @param objective
	 *            Where to place the stair, or null to let this method choose.
	 * @param lastHope
	 *            true if failing will doom generation for sure.
	 * @return Where it got generated, if it did (otherwise null)
	 */
	protected /* @Nullable */ Coord generateStair(DungeonGenerator gen, GenerationData gdata, boolean upOrDown,
			/* @Nullable */ Coord objective, boolean lastHope) {
		final Dungeon dungeon = gdata.dungeon;
		final IStairGenerator generator = getStairGenerator(gen, gdata, objective, upOrDown);
		final Iterator<Coord> candidates = generator.candidates();
		if (candidates == null || !candidates.hasNext()) {
			infoLog(gen, "No candidate for stair " + (upOrDown ? "up" : "down"));
			return null;
		}

		/* 'lastHope' => 'trieds' won't be used */
		final List<Coord> trieds = lastHope ? null : new ArrayList<Coord>(32);
		final /* @Nullable */ Coord other = dungeon.getStair(!upOrDown);
		while (candidates.hasNext()) {
			final Coord candidate = candidates.next();
			if (other == null || (!other.equals(candidate) && gdata.pathExists(other, candidate, false, false))) {
				if (punchStair(gdata, candidate, upOrDown))
					return candidate;
			}
			if (trieds != null)
				trieds.add(candidate);
		}
		if (lastHope)
			return null;
		if (other == null)
			return null;
		/*
		 * It may be a connectivity problem, if the stair objective is only close to
		 * rooms that aren't connected to the other stair. Let's try to fix that.
		 */
		final List<Zone> dests = new ArrayList<Zone>(
				gdata.zonesConnectedTo(true, false, Collections.singletonList(other)));
		assert !dests.isEmpty();
		final Collection<Zone> sources = gdata.zonesConnectedTo(true, false, trieds);
		infoLog(gen,
				"Could not generate " + (upOrDown ? "upward" : "downward")
						+ " stair, trying to fix connectivity issue (around " + objective + ") if any ("
						+ sources.size() + " sources and " + dests.size() + " destinations).");
		final boolean built = gen.generateCorridors(gdata, sources, dests,
				new ICorridorControl.Impl(dungeon, false, true, false, Integer.MAX_VALUE, true));
		if (!built) {
			infoLog(gen, "Could not fix connectivity issue. Failing.");
			return null;
		} else
			infoLog(gen, "Fixed connectivity issue by creating corridor(s)");
		return generateStair(gen, gdata, upOrDown, objective, true);
	}

	protected IStairGenerator getStairGenerator(DungeonGenerator gen, GenerationData gdata,
			/* @Nullable */ Coord objective, boolean upOrDown) {
		final Dungeon dungeon = gdata.dungeon;
		final IConnectionFinder connections = new IConnectionFinder() {
			@Override
			public boolean areConnected(Zone z0, Zone z1, int intermediates) {
				return Dungeons.areConnected(dungeon, z0, z1, intermediates);
			}
		};
		return new StairGenerator(gen.logger, gen.rng, dungeon, objective, upOrDown, gdata, connections);
	}

	/** @return Whether punching was done */
	protected boolean punchStair(GenerationData gdata, Coord c, boolean upOrDown) {
		final Dungeon dungeon = gdata.dungeon;
		dungeon.getBuilder().setStair(c.x, c.y, upOrDown);
		return true;
	}

	/**
	 * You should avoid calling this method too much if {@code logger} is null or if
	 * info isn't enabled, because building {@code log} can be costly if it's not a
	 * constant.
	 * 
	 * @param log
	 */
	protected final void infoLog(DungeonGenerator gen, String log) {
		if (gen.logger != null)
			gen.logger.infoLog(Tags.GENERATION, log);
	}

	protected final void warnLog(DungeonGenerator gen, String log) {
		if (gen.logger != null)
			gen.logger.warnLog(Tags.GENERATION, log);
	}

	protected final void errLog(DungeonGenerator gen, String log) {
		if (gen.logger != null)
			gen.logger.errLog(Tags.GENERATION, log);
	}

}
