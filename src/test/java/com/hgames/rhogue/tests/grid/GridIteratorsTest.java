package com.hgames.rhogue.tests.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hgames.lib.collection.Collections;
import com.hgames.rhogue.grid.GridIterators;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * Tests of {@link GridIterators}.
 * 
 * @author smelC
 */
public class GridIteratorsTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final List<Set<Coord>> sets = new ArrayList<Set<Coord>>();
		final RNG rng = new RNG();
		final int mapWidth = 10;
		final int mapHeight = 15;
		for (int i = 0; i < 32; i++) {
			final Direction dir = rng.getRandomElement(Direction.CARDINALS);
			final int xstart = rng.nextInt(mapWidth);
			final int ystart = rng.nextInt(mapHeight);
			final Iterator<Coord> it = new GridIterators.RectangleRandomStartAndDirection(mapWidth, mapHeight,
					xstart, ystart, dir);
			final Set<Coord> set = new HashSet<Coord>();
			while (it.hasNext())
				set.add(it.next());
			if (set.size() != mapWidth * mapHeight)
				throw new IllegalStateException(
						"Expected size " + (mapWidth * mapHeight) + " but found " + set.size());
			int j = 0;
			for (Set<Coord> previous : sets) {
				if (!Collections.equivalent(previous, set))
					throw new IllegalStateException();
				System.out.println("Checked consistency of set " + j + " and set " + sets.size()
						+ " (with dir " + dir + ")");
				j++;
			}
			sets.add(set);
		}
	}

}
