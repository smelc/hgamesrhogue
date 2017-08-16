package com.hgames.rhogue.tests.generation.map;

import com.hgames.rhogue.generation.map.DungeonGenerator;
import com.hgames.rhogue.generation.map.DungeonGenerators;
import com.hgames.rhogue.generation.map.IDungeonDrawer;

import squidpony.squidmath.RNG;

/**
 * Tests of {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public class DungeonGeneratorTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final IDungeonDrawer drawer = new ConsoleDungeonDrawer(new DungeonSymbolDrawer());
		final DungeonGenerators dgens = new DungeonGenerators(new RNG(32), 60, 40);
		generate(dgens.rogueLikeGenerator(), drawer);
	}

	private static void generate(DungeonGenerator dgen, IDungeonDrawer drawer) {
		dgen.setDrawer(drawer);
		dgen.generate();
	}

}
