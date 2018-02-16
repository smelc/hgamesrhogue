package com.hgames.rhogue.generation.map.corridor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.hgames.lib.collection.Collections;
import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.zone.ListZone;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Bresenham;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * @author smelC
 */
public class BresenhamCorridorBuilder extends SkeletalCorridorBuilder {

	/**
	 * @param dungeon
	 * @param allowedCarvings
	 * @param allowedNeighbors
	 */
	public BresenhamCorridorBuilder(Dungeon dungeon, EnumSet<DungeonSymbol> allowedCarvings,
			EnumSet<DungeonSymbol> allowedNeighbors) {
		super(dungeon, allowedCarvings, allowedNeighbors);
	}

	@Override
	protected Zone build(IRNG rng, Coord start, Coord end) {
		final Coord[] array = Bresenham.line2D_(start, end);
		final List<Coord> result = new ArrayList<Coord>(array.length + (array.length / 4));
		assert array[0].equals(start);
		assert array[array.length - 1].equals(end);
		Coord prev = null;
		for (int i = 0; i < array.length; i++) {
			/*
			 * If 'extremity' holds we should not add 'now', but the turn maybe
			 */
			final boolean extremity = i == 0 || i == array.length - 1;
			final Coord now = array[i];
			if (prev != null) {
				assert prev.isAdjacent(now);
				final Direction dir = prev.toGoTo(now);
				if (!dir.isCardinal()) {
					/*
					 * Need to add a turn, because we want the result to be
					 * cardinally walkable.
					 */
					final Coord candidate1 = prev.translate(dir.clockwise());
					final Coord candidate2 = prev.translate(dir.counterClockwise());
					final int bad1 = turnBadness(candidate1);
					final int bad2 = turnBadness(candidate2);
					final Coord chosenTurn;
					if (bad1 == bad2)
						chosenTurn = rng.nextBoolean() ? candidate1 : candidate2;
					else if (bad1 < bad2)
						chosenTurn = candidate1;
					else
						chosenTurn = candidate2;
					final Coord finally_;
					if (isCarvingAllowed(chosenTurn))
						finally_ = chosenTurn;
					else {
						final Coord other = chosenTurn == candidate1 ? candidate2 : candidate1;
						if (!isCarvingAllowed(other))
							return null;
						finally_ = other;
					}
					assert isCarvingAllowed(finally_);
					result.add(finally_);
				}
			}
			if (!extremity) {
				if (!isCarvingAllowed(now))
					return null;
				result.add(now);
			}
			prev = now;
		}
		assert Collections.isSet(result);
		return result.isEmpty() ? null : new ListZone(result);
	}

}
