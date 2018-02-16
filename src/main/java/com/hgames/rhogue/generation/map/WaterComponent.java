package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.DungeonGenerator.FloodFillObjective;
import com.hgames.rhogue.generation.map.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.DungeonGenerator.ZoneType;
import com.hgames.rhogue.generation.map.flood.DungeonWaterStartFloodFill;
import com.hgames.rhogue.generation.map.flood.FloodFill;
import com.hgames.rhogue.zone.ListZone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * Generation of water.
 * 
 * <p>
 * This component behaves differently as to whether
 * {@link DungeonGenerator#startWithWater} is set. If not set, it'll build pools
 * that are connected to existing walkable areas.
 * </p>
 * 
 * @author smelC
 */
public class WaterComponent implements GeneratorComponent {

	@Override
	public boolean generate(DungeonGenerator gen, GenerationData gdata) {
		if (gen.waterPercentage == 0 || gen.waterPools == 0)
			/* Nothing to do */
			return true;
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		Set<Coord> candidates = gdata.getWaterFillStartCandidates();
		if (candidates.isEmpty())
			candidates = new LinkedHashSet<Coord>();
		gdata.removeWaterFillStartCandidates();
		final IRNG rng = gen.rng;
		final int width = gen.width;
		final int height = gen.height;
		if (candidates.isEmpty()) {
			if (gen.startWithWater) {
				for (int i = 0; i < gen.waterPools; i++) {
					/* So that's it's not too much on the edge */
					final int x = rng.between(width / 4, width - (width / 4));
					final int y = rng.between(height / 4, height - (height / 4));
					candidates.add(Coord.get(x, y));
				}
			} else
				throw new IllegalStateException("Implement me");
		}
		/* The number of cells filled */
		int filled = 0;
		final FloodFill fill = new DungeonWaterStartFloodFill(gdata.dungeon.map, width, height);
		final FloodFillObjective objective = new FloodFillObjective(dungeon, gen.startWithWater);
		final int msz = dungeon.size();
		final int totalObjective = (msz / 100) * gen.waterPercentage;
		final int poolObjective = totalObjective / gen.waterPools;
		int poolsDone = 0;
		final Iterator<Coord> it = candidates.iterator();
		final LinkedHashSet<Coord> spill = new LinkedHashSet<Coord>();
		while (it.hasNext() && poolsDone < gen.waterPools && filled < totalObjective) {
			/* Prepare iteration */
			objective.prepare(poolObjective);
			spill.clear();
			/* Go */
			final Coord candidate = it.next();
			fill.flood(rng, candidate.x, candidate.y, objective, gen.startWithWater, spill);
			if (spill.isEmpty())
				continue;
			final int sz = spill.size();
			for (Coord spilt : spill) {
				assert Dungeons.findRoomOrCorridorContaining(dungeon, spilt.x,
						spilt.y) == null : ("Cells spilt on should not belong to a zone. You should fix 'impassable'. Cell spilt on: "
								+ spilt + " belonging to zone: "
								+ Dungeons.findRoomOrCorridorContaining(dungeon, spilt.x, spilt.y));
				builder.setSymbol(spilt, DungeonSymbol.DEEP_WATER);
				filled++;
			}
			gen.addZone(gdata, new ListZone(new ArrayList<Coord>(spill)), null, null, ZoneType.DEEP_WATER);
			if (gen.logger != null && gen.logger.isInfoEnabled())
				gen.logger.infoLog(Tags.GENERATION, "Created water pool of size " + sz); // + ": " + spill);
			gen.draw(dungeon);
			poolsDone++;
		}
		return true;
	}

}
