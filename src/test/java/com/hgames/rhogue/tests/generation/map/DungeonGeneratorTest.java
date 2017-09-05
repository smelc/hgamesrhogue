package com.hgames.rhogue.tests.generation.map;

import com.hgames.rhogue.generation.map.DungeonGenerator;
import com.hgames.rhogue.generation.map.DungeonGenerators;
import com.hgames.rhogue.generation.map.IDungeonDrawer;

import squidpony.squidmath.RNG;

/**
 * Command line tests of {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public class DungeonGeneratorTest {

	/** The seed used */
	public static final int SEED = 38;
	// FIXME CH See seed 54 for a lonely door

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final IDungeonDrawer drawer = new ConsoleDungeonDrawer(new DungeonSymbolDrawer());
		final int width = 60;
		final int height = 40;
		final DungeonGenerators dgens = new DungeonGenerators(new RNG(SEED), width, height);
		generate(dgens.guaranteesOneCircularRoom(true), drawer);
	}

	private static void generate(DungeonGenerator dgen, IDungeonDrawer drawer) {
		dgen.setDrawer(drawer);
		dgen.generate();
	}

}
