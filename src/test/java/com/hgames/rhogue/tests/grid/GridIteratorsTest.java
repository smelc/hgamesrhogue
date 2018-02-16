package com.hgames.rhogue.tests.grid;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hgames.lib.collection.Collections;
import com.hgames.lib.iterator.Iterators;
import com.hgames.rhogue.grid.GridIterators;
import com.hgames.rhogue.rng.DefaultRNG;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

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
		testGrowingRectangle();
		testRectangleRandomStartAndDirection();
	}

	private static void testGrowingRectangle() {
		final Coord zz = Coord.get(0, 0);
		Iterator<Coord> it = new GridIterators.GrowingRectangle(zz, 0);
		{
			if (Iterators.size(it) != 1)
				throw new IllegalStateException();
			System.out.println("Checked size of GrowingRectangle(_, 0)");
		}
		{
			it = new GridIterators.GrowingRectangle(zz, 1);
			final int sz = Iterators.size(it);
			if (sz != 9)
				throw new IllegalStateException();
			System.out.println("Checked size of GrowingRectangle(_, 1)");
			it = new GridIterators.GrowingRectangle(zz, 1);
			final Set<Coord> equiv = new HashSet<Coord>(9);
			equiv.add(zz);
			for (Direction out : Direction.OUTWARDS)
				equiv.add(zz.translate(out));
			final List<Coord> asList = Iterators.toList(it, 9);
			if (!Collections.isSet(asList))
				throw new IllegalStateException();
			it = new GridIterators.GrowingRectangle(zz, 1);
			final Set<Coord> asSet = Iterators.toSet(it, 9);
			if (!Collections.equivalent(asSet, equiv))
				throw new IllegalStateException();
			System.out.println("Checked content of GrowingRectangle(_, 1)");
		}
		{
			it = new GridIterators.GrowingRectangle(zz, 2);
			if (Iterators.size(it) != 25)
				throw new IllegalStateException();
			System.out.println("Checked size of GrowingRectangle(_, 2)");
		}
	}

	private static void testRectangleRandomStartAndDirection() {
		final List<Set<Coord>> sets = new ArrayList<Set<Coord>>();
		final IRNG rng = new DefaultRNG();
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
