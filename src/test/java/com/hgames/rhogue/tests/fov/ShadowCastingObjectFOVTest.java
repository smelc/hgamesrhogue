package com.hgames.rhogue.tests.fov;

import java.util.ArrayList;
import java.util.List;

import com.hgames.lib.Exceptions;
import com.hgames.lib.color.Colors;
import com.hgames.lib.color.IColor;
import com.hgames.rhogue.fov.FOVCell;
import com.hgames.rhogue.fov.IFOVCell;
import com.hgames.rhogue.fov.ShadowCastingObjectFOV;
import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.generation.map.DungeonGenerator;
import com.hgames.rhogue.generation.map.DungeonGenerators;
import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.generation.map.DungeonSymbolDrawer;
import com.hgames.rhogue.generation.map.draw.ConsoleDungeonDrawer;
import com.hgames.rhogue.generation.map.draw.IDungeonDrawer;
import com.hgames.rhogue.grid.Positioned;
import com.hgames.rhogue.lighting.ILightSource;
import com.hgames.rhogue.rng.DefaultRNG;

import squidpony.squidmath.IRNG;

/**
 * Tests of {@link ShadowCastingObjectFOVTest}.
 * 
 * @author smelC
 */
public class ShadowCastingObjectFOVTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		final IRNG rng = new DefaultRNG();
		final int width = 30;
		final int height = 30;
		for (int i = 0; i < 8; i++) {
			final int w = width + ((rng.nextBoolean() ? -1 : 1) * rng.nextInt(8));
			final int h = height + ((rng.nextBoolean() ? -1 : 1) * rng.nextInt(8));
			final DungeonGenerators dgens = new DungeonGenerators(rng, w, h);
			final DungeonGenerator dgen = rng.nextBoolean() ? dgens.basic()
					: (rng.nextBoolean() ? dgens.cave() : dgens.fancy());
			final Dungeon dungeon = dgen.generate();
			if (dungeon == null)
				continue;
			doFOV(rng, dungeon.getMap());
		}
	}

	private static void doFOV(IRNG rng, DungeonSymbol[][] map) {
		final int width = map.length;
		final int height = width == 0 ? 0 : map[0].length;
		@SuppressWarnings("unchecked")
		final FOVCell<LightSource>[][] lightMap = new FOVCell[width][height];
		final double[][] resistanceMap = toResistanceMap(map);
		final ShadowCastingObjectFOV<LightSource, FOVCell<LightSource>> fov = new ShadowCastingObjectFOV<LightSource, FOVCell<LightSource>>(
				null, resistanceMap, lightMap) {
			@Override
			protected FOVCell<LightSource> buildCell() {
				return new FOVCell<LightSource>();
			}
		};
		final List<LightSource> sources = buildSources(rng, map);
		fov.computeFOV(sources);
		final IFOVCell<LightSource>[][] result = fov.getFOV();
		final IDungeonDrawer ddrawer = new ConsoleDungeonDrawer(new DungeonSymbolDrawer()) {
			@Override
			protected char print(DungeonSymbol sym, int x, int y) {
				for (LightSource source : sources) {
					if (source.getX() == x && source.getY() == y)
						return source.getSymbol();
				}
				final IFOVCell<LightSource> cell = result[x][y];
				if (cell != null) {
					final double lighting = cell.getLighting();
					if (0.0 < lighting) {
						assert lighting <= 1.0;
						if (lighting == 1.0)
							return '^';
						else
							return Long.toString(Math.round(lighting * 10)).charAt(0);
					}
				}
				return super.print(sym, x, y);
			}
		};
		ddrawer.draw(map);
	}

	private static double[][] toResistanceMap(DungeonSymbol[][] map) {
		final int width = map.length;
		final int height = width == 0 ? 0 : map[0].length;
		final double[][] result = new double[width][height];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				final DungeonSymbol sym = map[x][y];
				switch (sym) {
				case CHASM:
				case DEEP_WATER:
				case FLOOR:
				case GRASS:
				case SHALLOW_WATER:
					// Leave 0.0
					continue;
				case HIGH_GRASS:
					// Reduces light
					result[x][y] = 0.3f;
					continue;
				case DOOR:
				case STAIR_DOWN:
				case STAIR_UP:
				case WALL:
					// Blocks light
					result[x][y] = 1f;
					continue;
				}
				throw Exceptions.newUnmatchedISE(sym);
			}
		}
		return result;
	}

	private static List<LightSource> buildSources(IRNG rng, DungeonSymbol[][] map) {
		final List<LightSource> result = new ArrayList<LightSource>();
		for (int i = 0; i < 8; i++) {
			final LightSource source = buildSource(rng, map);
			if (source != null)
				result.add(source);
		}
		return result;
	}

	private static /* @Nullable */ LightSource buildSource(IRNG rng, DungeonSymbol[][] map) {
		final int width = map.length;
		final int height = width == 0 ? 0 : map[0].length;
		for (int i = 0; i < 64; i++) {
			final int x = rng.nextInt(width);
			final int y = rng.nextInt(height);
			final DungeonSymbol sym = map[x][y];
			switch (sym) {
			case CHASM:
			case DOOR:
			case FLOOR:
			case GRASS:
			case HIGH_GRASS:
			case SHALLOW_WATER:
				return new LightSource('s', rng.between(3, 7), x, y);
			case DEEP_WATER:
			case STAIR_DOWN:
			case STAIR_UP:
			case WALL:
				/* Cannot contain a light source */
				continue;
			}
			throw Exceptions.newUnmatchedISE(sym);
		}
		return null;
	}

	/**
	 * @author smelC
	 */
	private static final class LightSource implements ILightSource, Positioned {

		private final char sym;
		private final int intensity;
		private final int x;
		private final int y;

		protected LightSource(char sym, int intensity, int x, int y) {
			this.sym = sym;
			this.intensity = intensity;
			this.x = x;
			this.y = y;
		}

		@Override
		public IColor getLightColor() {
			return Colors.WHITE;
		}

		@Override
		public int getLightIntensity() {
			return intensity;
		}

		@Override
		public int getX() {
			return x;
		}

		@Override
		public int getY() {
			return y;
		}

		@Override
		public boolean setX(int x) {
			return false;
		}

		@Override
		public boolean setY(int y) {
			return false;
		}

		@Override
		public boolean ensureCoord(int ensuredX, int ensuredY) {
			/* Because this.x and this.y cannot be set */
			return getX() == ensuredX && getY() == ensuredY;
		}

		protected char getSymbol() {
			return sym;
		}

		@Override
		public String toString() {
			return "LightSource [sym=" + sym + ", intensity=" + intensity + ", x=" + x + ", y=" + y + "]";
		}
	}

}
