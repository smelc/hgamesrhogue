package com.hgames.rhogue.generation.map.rgenerator;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import com.hgames.lib.Arrays;
import com.hgames.rhogue.generation.map.draw.IDungeonDrawer;
import com.hgames.rhogue.generation.map.dungeon.DungeonSymbol;
import com.hgames.rhogue.generation.map.dungeon.RoomComponent;
import com.hgames.rhogue.zone.Zone;
import com.hgames.rhogue.zone.Zones;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * A room generator that generates caves.
 * 
 * @author smelC
 */
public class CaveRoomGenerator extends SkeletalRoomGenerator {

	protected int initialWallProbability = 45;
	protected int iterations = 5;

	private static final boolean WALL = false;
	private static final boolean FLOOR = !WALL;

	private static final int MIN_SIDE_SIZE = 5;

	/**
	 * A fresh instance
	 */
	public CaveRoomGenerator() {
		/* Because caves need minimal space to look good */
		setMinSideSize(MIN_SIDE_SIZE, true);
		setMinSideSize(MIN_SIDE_SIZE, false);
	}

	@Override
	public Zone generate(IRNG rng, RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		return generate(rng, maxWidth, maxHeight, null);
	}

	/**
	 * @param rng
	 * @param maxWidth
	 * @param maxHeight
	 * @param drawer
	 * 			How to draw (for debug)
	 * @return The generated zone
	 */
	public Zone generate(IRNG rng, int maxWidth, int maxHeight, /*@Nullable*/ IDungeonDrawer drawer) {
		if (maxWidth < MIN_SIDE_SIZE) { assert false; return null; }
		if (maxHeight < MIN_SIDE_SIZE) { assert false; return null; }

		final boolean boulderizeOpenArea = rng.nextBoolean() && (8 <= maxWidth && 8 <= maxHeight);

		/* Walls are encoded with 'false', floors with 'true' */
		final boolean[][] now = new boolean[maxWidth][maxHeight];
		final boolean[][] after = new boolean[maxWidth][maxHeight];

		/* Initialize */
		for (int x = 0; x < maxWidth; x++) {
			for (int y = 0; y < maxHeight; y++) {
				now[x][y] = !rng.roll(initialWallProbability, 100);
			}
		}

		if (drawer != null) drawer.draw(toSymbols(now));

		for (int i = 0; i < iterations; i++) {
			for (int x = 0; x < maxWidth; x++) {
				for (int y = 0; y < maxHeight; y++) {
					final int neighborWalls = neighborWalls(now, x, y, true);
					if (boulderizeOpenArea && neighborWalls == 0)
						after[x][y] = WALL;
					else
						/* It's a floor if neighborWalls < 5 */
						after[x][y] = neighborWalls < 5;
				}
			}
			pourInto(after, now);
			if (drawer != null) drawer.draw(toSymbols(now));
		}

		if (boulderizeOpenArea)
			sanitize(now);

		if (!stronglyConnected(now, after)) return null;

		final List<Coord> result = new ArrayList<Coord>((maxWidth * maxHeight) / 2);
		for (int x = 0; x < maxWidth; x++) {
			for (int y = 0; y < maxHeight; y++) {
				if (now[x][y]) result.add(Coord.get(x, -y));
			}
		}

		return result.size() < 4 ? null : Zones.build(result);
	}

	private static int neighborWalls(boolean[][] map, int x, int y, boolean oobCounts) {
		int result = 0;
		for (Direction dir : Direction.OUTWARDS) {
			final int xprime = x + dir.deltaX;
			final int yprime = y + dir.deltaY;
			if (!Arrays.isValid(map, xprime, yprime)) { 
				if (oobCounts) result++;
				continue;
			}
			if (map[xprime][yprime] == WALL) result++;
		}
		return result;
	}

	private static void pourInto(boolean[][] a1, boolean[][] a2) {
		for (int x = 0; x < a1.length; x++) {
			final boolean[] ys = a1[x];
			for (int y = 0; y < ys.length; y++)
				a2[x][y] = a1[x][y];
		}
	}

	private static void sanitize(boolean[][] map) {
		final int width = map.length;
		final int height = width == 0 ? 0 : map[0].length;
		int roll = 0;
		while (roll < 64) {
			boolean change = false;
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					final boolean cell = map[x][y] == FLOOR;
					/* Check that cell has a cardinal neighbor of same type */
					boolean sane = false;
					for (Direction dir : Direction.CARDINALS) {
						final int xprime = x + dir.deltaX;
						final int yprime = y + dir.deltaY;
						if (Arrays.isValid(map, xprime, yprime) &&
								map[xprime][yprime] == cell) {
							sane = true; break;
						}
					}
					if (!sane) {
						/* Invert */
						map[x][y] = !cell;
						change = true;
					}
				}
			}
			if (!change) break;
			roll++;
		}
		if (roll == 64)
			System.err.println("Emergency exit in " + CaveRoomGenerator.class.getSimpleName() + "::sanitize");
	}

	/** @return if all cells of {@code map} are cardinally connected */
	private static boolean stronglyConnected(boolean[][] map, boolean[][] buf) {
		final int width = map.length;
		final int height = width == 0 ? 0 : map[0].length;

		Coord start = null;

		/* Let's put false everywhere in 'buf', to use it as a reachable marker */
		/* and look for a starting point in 'map' at the same time */

		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (start == null && map[x][y] == FLOOR) start = Coord.get(x, y);
				buf[x][y] = false;
			}
		}

		if (start == null) return false;

		final Queue<Coord> todos = new LinkedList<Coord>();
		todos.add(start);
		while (!todos.isEmpty()) {
			final Coord todo = todos.remove();
			if (buf[todo.x][todo.y]) continue;
			buf[todo.x][todo.y] = FLOOR;
			for (Direction dir : Direction.CARDINALS) {
				final Coord neighbor = todo.translate(dir);
				if (!Arrays.isValid(map, neighbor.x, neighbor.y)) continue;
				if (map[neighbor.x][neighbor.y] == FLOOR && !buf[neighbor.x][neighbor.y])
					/* Go to a walkable neighbor */
					todos.add(neighbor);
			}
		}

		/* Now check that map's entirety has been visited */
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++)
				if (map[x][y] == FLOOR && buf[x][y] != FLOOR) return false;
		}

		return true;
	}

	private static DungeonSymbol[][] toSymbols(boolean[][] map) {
		final int width = map.length;
		final int height = width == 0 ? 0 : map[0].length;
		final DungeonSymbol[][] syms = new DungeonSymbol[width][height];
		for (int x = 0; x < width ; x++) {
			for (int y = 0; y < height; y++) {
				syms[x][y] = map[x][y] == FLOOR ? DungeonSymbol.FLOOR : DungeonSymbol.WALL;
			}
		}
		return syms;
	}
}
