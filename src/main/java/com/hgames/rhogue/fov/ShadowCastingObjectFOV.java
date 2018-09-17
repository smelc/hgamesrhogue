package com.hgames.rhogue.fov;

import java.util.List;

import com.hgames.lib.collection.array.ArrayBuilder;
import com.hgames.rhogue.grid.Positioned;
import com.hgames.rhogue.lighting.ILightSource;

import squidpony.squidgrid.Direction;

/**
 * Object oriented FOV casting. Each lit cell is a different object, which makes
 * possible to keep track of the emitter and do game-specific treatments.
 * 
 * @author smelC, based on original work by Eben Howard (<a href=
 *         "http://www.roguebasin.com/index.php?title=Improved_Shadowcasting_in_Java">roguebasin</a>);
 *         released in the public domain as part of the hgamesrhogue's library
 *         with Eben's permission.
 * @param <U>
 *            The type of light sources.
 * @param <T>
 *            The type of cells.
 */
public abstract class ShadowCastingObjectFOV<U extends ILightSource & Positioned, T extends IFOVCell<U>> {

	/** Can only be null if {@link #lightMap} is non-null and non shallow */
	protected final /* @Nullable */ ArrayBuilder<T> ab;
	/* Maybe lazily allocated and possibly shallow */
	protected /* @Nullable */ T[][] lightMap;

	protected final int width;
	protected final int height;

	/**
	 * Constructor where the resistance is omitted, being equivalent to having 0
	 * everywhere. Also, the light map will be build entirely lazily.
	 * 
	 * @param ab
	 *            How to build instances of {@code T[]}.
	 * @param width
	 * @param height
	 */
	public ShadowCastingObjectFOV(ArrayBuilder<T> ab, int width, int height) {
		if (ab == null)
			throw new NullPointerException("array builder shouldn't be null in this constructor");
		this.ab = ab;
		this.width = width;
		this.height = height;
	}

	/**
	 * Constructor for when you want to give the light map directly.
	 * {@code resistanceMap} and {@code lightMap} should not both be null.
	 * 
	 * @param ab
	 *            How to build instances of {@code T[]}. Can be null if
	 *            {@code lightMap} is neither null or shallow.
	 * @param lightMap
	 *            The light map should not be shallow if {@code ab} is null.
	 */
	public ShadowCastingObjectFOV(/* @Nullable */ ArrayBuilder<T> ab, T lightMap[][]) {
		if (lightMap == null)
			throw new NullPointerException("array builder shouldn't be both null");
		this.ab = ab;
		this.width = lightMap.length;
		this.height = this.width == 0 ? 0 : lightMap[0].length;
		this.lightMap = lightMap;
	}

	/**
	 * Computes the FOV of {@code sources}.
	 * 
	 * @param sources
	 */
	public void computeFOV(List<? extends U> sources) {
		final int sz = sources.size();
		for (int i = 0; i < sz; i++) {
			final U source = sources.get(i);
			calculateFOV(source);
		}
	}

	/** Clears the last computed FOV. */
	public void clearLightMap() {
		if (lightMap == null)
			return;
		for (int x = 0; x < width; x++) {
			final T[] ys = lightMap[x];
			if (ys == null)
				continue;
			assert ys.length == height;
			for (int y = 0; y < height; y++) {
				final T t = lightMap[x][y];
				if (t != null)
					t.clear();
			}
		}
	}

	/**
	 * @return The FOV computed with the previous call to {@link #computeFOV(List)}.
	 *         A reference to this instance' inner state is returned.
	 */
	public T[][] getFOV() {
		return lightMap;
	}

	/**
	 * @param x
	 * @param y
	 * @return Whether (x, y) is a valid cell, bounds wise
	 */
	public boolean isValid(int x, int y) {
		return 0 <= x && x < width && 0 <= y && y < height;
	}

	/**
	 * @param source
	 * @param cell
	 *            A cell lit by {@code source}.
	 * @param v
	 *            The value of the light emitted by {@code source}.
	 * @param x
	 *            The x-position of {@code cell}.
	 * @param y
	 *            The y-position of {@code cell}.
	 * @return Whether the cell changed.
	 */
	/*
	 * Subclassers may override to do something more (alternatively override
	 * FOVCell:unionLight).
	 */
	protected boolean unionLight(U source, T cell, double v, int x, int y) {
		return cell.unionLight(source, v);
	}

	/** @return A fresh unlit cell */
	protected abstract T buildCell();

	/** @return The resistance at (x, y) */
	protected abstract double getResistance(int x, int y);

	protected void calculateFOV(U source) {
		final int srcX = source.getX();
		final int srcY = source.getY();
		/* Allocate lightMap enough if needed */
		if (lightMap == null)
			lightMap = ab.build(width, height, true);
		T[] ys = lightMap[srcX];
		if (ys == null) {
			ys = ab.build(height);
			lightMap[srcX] = ys;
		}
		T start = lightMap[srcX][srcY];
		if (start == null) {
			start = buildCell();
			lightMap[srcX][srcY] = start;
		}
		unionLight(source, start, 1.0d, srcX, srcY);

		final int radius = source.getLightIntensity();
		for (Direction d : Direction.DIAGONALS) {
			castLight(source, 1, 1.0f, 0.0f, 0, d.deltaX, d.deltaY, 0, radius);
			castLight(source, 1, 1.0f, 0.0f, d.deltaX, 0, 0, d.deltaY, radius);
		}
	}

	/**
	 * @param source
	 *            The light's emitter
	 * @param row
	 * @param start_
	 *            The maximum light
	 * @param end
	 *            The minimum light
	 * @param xx
	 * @param xy
	 * @param yx
	 * @param yy
	 * @param radius
	 *            The caster's radius.
	 */
	private void castLight(U source, int row, float start_, float end, int xx, int xy, int yx, int yy, int radius) {
		float start = start_;
		if (start < end)
			return;
		final int srcX = source.getX();
		final int srcY = source.getY();
		float newStart = 0.0f;
		boolean blocked = false;
		for (int distance = row; distance <= radius && !blocked; distance++) {
			final int deltaY = -distance;
			for (int deltaX = -distance; deltaX <= 0; deltaX++) {
				final float leftSlope = (deltaX - 0.5f) / (deltaY + 0.5f);
				final float rightSlope = (deltaX + 0.5f) / (deltaY - 0.5f);

				final int curX = srcX + (deltaX * xx) + (deltaY * xy);
				final int curY = srcY + (deltaX * yx) + (deltaY * yy);
				if (!isValid(curX, curY) || start < rightSlope)
					continue;
				else if (end > leftSlope)
					break;

				// check if it's within the lightable area and light if needed
				if (radiusOf(deltaX, deltaY) <= radius) {
					final float bright = (float) (1 - (radiusOf(deltaX, deltaY) / radius));
					/* Allocate lightMap enough if needed */
					if (lightMap == null)
						lightMap = ab.build(width, height, true);
					T ys[] = lightMap[curX];
					if (ys == null) {
						ys = ab.build(height);
						lightMap[curX] = ys;
					}
					T t = lightMap[curX][curY];
					if (t == null) {
						t = buildCell();
						lightMap[curX][curY] = t;
					}
					unionLight(source, t, bright, curX, curY);
				}

				if (blocked) {
					// previous cell was a blocking one
					if (getResistance(curX, curY) >= 1) {
						// Hitting a wall
						newStart = rightSlope;
						continue;
					} else {
						blocked = false;
						start = newStart;
					}
				} else if (getResistance(curX, curY) >= 1 && distance < radius) {
					// hit a wall within sight line
					blocked = true;
					castLight(source, distance + 1, start, leftSlope, xx, xy, yx, yy, radius);
					newStart = rightSlope;
				}
			}
		}
	}

	private static double radiusOf(double dx, double dy) {
		// A sphere's radius
		return Math.sqrt(dx * dx + dy * dy);
	}

}
