package com.hgames.rhogue.tests.generation.map;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.hgames.lib.iterator.Iterators;
import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.generation.map.DungeonGenerator;
import com.hgames.rhogue.generation.map.DungeonGenerators;
import com.hgames.rhogue.generation.map.DungeonSymbolDrawer;
import com.hgames.rhogue.generation.map.DungeonZonesCrawler;
import com.hgames.rhogue.generation.map.draw.ConsoleDungeonDrawer;
import com.hgames.rhogue.generation.map.draw.IDungeonDrawer;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.RNG;

/**
 * Command line tests of {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public class DungeonGeneratorTest {

	/** The seed used */
	public static final int SEED = 34;
	// FIXME CH See seed 36 for disconnected component (unstable on lower right)
	// FIXME CH See seed 54 for lonely doors

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final IDungeonDrawer drawer = new ConsoleDungeonDrawer(new DungeonSymbolDrawer());
		final int width = 60;
		final int height = 40;
		final DungeonGenerators dgens = new DungeonGenerators(new RNG(SEED), width, height);
		generate(dgens.fancy(), drawer);
	}

	private static void generate(DungeonGenerator dgen, IDungeonDrawer drawer) {
		dgen.setDrawer(drawer);
		final Dungeon dungeon = dgen.generate();
		assert testDungeonZonesCrawler(dungeon);
	}

	private static boolean testDungeonZonesCrawler(Dungeon dungeon) {
		final List<Zone> rooms = dungeon.getRooms();
		if (rooms.isEmpty())
			return true;
		final Zone start = new RNG().getRandomElement(rooms);
		final Iterator<Zone> crawler = new DungeonZonesCrawler(dungeon, start).iterator();
		final Set<Zone> expected = new HashSet<Zone>(rooms.size());
		expected.addAll(dungeon.getRooms());
		expected.addAll(dungeon.getCorridors());
		final Set<Zone> visited = Iterators.toSet(crawler, rooms.size());
		if (!expected.containsAll(visited))
			return false;
		if (!visited.containsAll(expected))
			return false;
		return true;
	}

}
