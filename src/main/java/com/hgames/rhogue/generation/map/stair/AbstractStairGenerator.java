package com.hgames.rhogue.generation.map.stair;

import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

import com.hgames.lib.Objects;
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.grid.GridIterators;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * @author smelC
 */
public abstract class AbstractStairGenerator extends SkeletalStairGenerator {

	protected /* @Nullable */ ILogger logger;
	/** The {@link RNG} to use */
	protected final RNG rng;

	/** The objective given at construction */
	private final /* @Nullable */ Coord initialObjective;

	/** Whether the stair to generate is the stair up or the stair down. */
	protected final boolean upOrDown;

	/**
	 * @param dungeon
	 * @param objective
	 * @param upOrDown
	 *            Whether the stair to generate is the stair up or the stair
	 *            down.
	 */
	AbstractStairGenerator(/* @Nullable */ ILogger logger, RNG rng, Dungeon dungeon,
			/* @Nullable */ Coord objective, boolean upOrDown) {
		super(dungeon);
		this.logger = logger;
		this.rng = Objects.checkNotNull(rng);
		this.initialObjective = objective;
		this.upOrDown = upOrDown;
	}

	@Override
	public Queue<Coord> candidates() {
		final Coord objective = getObjective();
		if (objective == null)
			return null;
		if (logger != null && logger.isInfoEnabled())
			logger.infoLog(Tags.GENERATION, "Stair objective: " + objective);
		final Queue<Coord> result = candidates0(objective);
		if (result == null || result.isEmpty()) {
			logger.infoLog(Tags.GENERATION, "No candidate for stair " + (upOrDown ? "up" : "down"));
			return null;
		}

		if (logger != null && logger.isInfoEnabled())
			logger.infoLog(Tags.GENERATION,
					result.size() + " stair candidate" + (result.size() == 1 ? "" : "s"));
		return result;
	}

	private Coord getObjective() {
		return initialObjective == null ? getObjective0() : initialObjective;
	}

	/**
	 * @return Where to generate the stair, approximately.
	 */
	protected abstract Coord getObjective0();

	/**
	 * @param center
	 * @return Candidates for stairs close to {@code center}. Or null.
	 */
	protected /* @Nullable */ Queue<Coord> candidates0(Coord center) {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		final int rSize = ((width + height) / 6) + 1;
		final Iterator<Coord> it = new GridIterators.GrowingRectangle(center, rSize);
		final PriorityQueue<Coord> queue = new PriorityQueue<Coord>(rSize * 4,
				newDistanceComparatorFrom(center));
		int trials = 0;
		while (it.hasNext()) {
			final Coord next = it.next();
			trials++;
			if (isValidCandidate(next))
				queue.add(next);
		}
		if (queue.isEmpty()) {
			if (logger != null && logger.isInfoEnabled())
				logger.infoLog(Tags.GENERATION, trials + " cell" + (trials == 1 ? "" : "s")
						+ " have been unsuccessfully tried for the stair " + (upOrDown ? "up" : "down"));
		}
		return queue;
	}

	/**
	 * @param dir
	 * @return A random cell. In the direction {@code dir} (think about the map
	 *         being split in 8 parts) if {@code dir} is not null.
	 */
	protected final Coord getRandomCell(/* @Nullable */ Direction dir) {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		if (dir == null)
			return Coord.get(rng.nextInt(width), rng.nextInt(height));
		else {
			final boolean hasup = dir.hasUp();
			final boolean hasdown = dir.hasDown();
			assert !(hasup && hasdown);
			final boolean hasleft = dir.hasLeft();
			final boolean hasright = dir.hasRight();
			assert !(hasleft && hasright);
			final int w3 = width / 3;
			final int h3 = height / 3;
			int x = rng.nextInt(w3);
			if (!hasleft) {
				x += w3;
				/* Can be centered or to the right */
				if (hasright)
					/* To the right */
					x += w3;
			}
			int y = rng.nextInt(h3);
			if (!hasup) {
				/* Can be centered or downward */
				y += h3;
				if (hasdown)
					/* Is downward */
					y += h3;
			}
			assert x < width;
			assert y < width;
			return Coord.get(x, y);
		}
	}

	/**
	 * @param c
	 * @return A {@link Comparator} that orders {@link Coord coords} according
	 *         to their distance to {@code c}.
	 */
	protected static Comparator<Coord> newDistanceComparatorFrom(final Coord c) {
		return new Comparator<Coord>() {
			@Override
			public int compare(Coord o1, Coord o2) {
				return Double.compare(o1.distance(c), o2.distance(c));
			}
		};
	}

}
