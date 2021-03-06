package com.hgames.rhogue.generation.map.dungeon;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.dungeon.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.dungeon.flood.DungeonGrassFloodFill;
import com.hgames.rhogue.generation.map.dungeon.flood.FloodFill;
import com.hgames.rhogue.generation.map.dungeon.flood.IFloodObjective;
import com.hgames.rhogue.zone.ListZone;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * Generation of grass.
 * 
 * @author smelC
 */
public class GrassComponent implements GeneratorComponent {

	@Override
	public boolean generate(DungeonGenerator gen, GenerationData gdata) {
		if (gen.grassPercentage <= 0 || gen.grassPatches <= 0)
			/* Nothing to do */
			return true;
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		final int cObjective = (Dungeons.getSizeOfRoomsAndCorridors(dungeon) * gen.grassPercentage) / 100;
		if (cObjective == 0)
			return true;
		final ILogger logger = gen.logger;
		if (logger != null && logger.isInfoEnabled())
			logger.infoLog(Tags.GENERATION, "Size objective of grass: " + cObjective + " cells");
		final IRNG rng = gen.rng;
		/* The number of cells turned into grass */
		int cells = 0;
		/* The number of grass patches done */
		int patches = 0;
		final int patchObjective = (cObjective / gen.grassPatches)
				+ ((rng.nextBoolean() ? 1 : -1) * (rng.between(1, gen.grassPatches / 5)));
		if (patchObjective <= 0)
			return true;
		if (logger != null && logger.isInfoEnabled())
			logger.infoLog(Tags.GENERATION, "Size objective of each grass patch: " + patchObjective + " cells");
		final List<Zone> rooms = dungeon.getRooms();
		int frustration = 0;
		final FloodFill fill = new DungeonGrassFloodFill(dungeon.map);
		final LinkedHashSet<Coord> buf = new LinkedHashSet<Coord>(patchObjective);
		nextPool: while (patches < gen.grassPatches && cells < cObjective && frustration < 8) {
			final Zone src = rng.getRandomElement(rooms);
			if (src.size() == 1) {
				/* It could be some kind of corridor */
				frustration++;
				continue;
			}
			int innerF = 0; /* Inner loop frustration */
			final List<Coord> all = src.getAll(false);
			final IFloodObjective objective = new IFloodObjective() {
				int dones = 0;
				/*
				 * A slightly moving objective every roll, to avoid all patches of grass to be
				 * of the same size.
				 */
				final int szo = patchObjective + ((rng.nextBoolean() ? 1 : -1) * patchObjective / 3);

				@Override
				public void record(Coord c) {
					dones++;
				}

				@Override
				public boolean isMet() {
					return szo <= dones;
				}
			};
			while (innerF < 4) {
				final Coord start = rng.getRandomElement(all);
				assert dungeon.isValid(start);
				assert buf.isEmpty();
				fill.flood(rng, start.x, start.y, objective, false, buf);
				if (buf.isEmpty()) {
					/* Failure */
					innerF++;
				} else {
					/* Success. Make the variant move. */
					patches++;
					cells += buf.size();
					/* Not using SetZone, as we would need to copy 'buf' anyway */
					builder.addGrassPool(new ListZone(new ArrayList<Coord>(buf)), EnumSet.of(DungeonSymbol.FLOOR));
					/*
					 * Should be done after 'addGrassPool' as it checks that symbols turned into
					 * grass are valid.
					 */
					builder.setSymbols(buf.iterator(), DungeonSymbol.GRASS);
					buf.clear();
					gen.draw(dungeon);
					continue nextPool;
				}
			}
			assert innerF <= 4;
			if (4 == innerF)
				frustration++;
		}

		return true;
	}

}
