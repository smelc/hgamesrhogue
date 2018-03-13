package com.hgames.rhogue.generation.map.stair;

import java.util.Comparator;
import java.util.Iterator;

import com.hgames.lib.Objects;
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.Dungeon;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * @author smelC
 */
public abstract class AbstractStairGenerator extends SkeletalStairGenerator {

	protected /* @Nullable */ ILogger logger;
	/** The {@link IRNG} to use */
	protected final IRNG rng;

	/** The objective given at construction */
	private final /* @Nullable */ Coord initialObjective;

	/** Whether the stair to generate is the stair up or the stair down. */
	protected final boolean upOrDown;

	/**
	 * @param dungeon
	 * @param objective
	 * @param upOrDown
	 *            Whether the stair to generate is the stair up or the stair down.
	 */
	AbstractStairGenerator(/* @Nullable */ ILogger logger, IRNG rng, Dungeon dungeon, /* @Nullable */ Coord objective,
			boolean upOrDown) {
		super(dungeon);
		this.logger = logger;
		this.rng = Objects.checkNotNull(rng);
		this.initialObjective = objective;
		this.upOrDown = upOrDown;
	}

	@Override
	public Iterator<Coord> candidates() {
		final Coord objective = getObjective();
		if (objective == null)
			return null;
		if (logger != null && logger.isInfoEnabled())
			logger.infoLog(Tags.GENERATION, "Stair objective: " + objective);
		final Iterator<Coord> result = candidates0(objective);
		assert result != null;
		if (!result.hasNext() && logger != null && logger.isInfoEnabled())
			logger.infoLog(Tags.GENERATION, "No candidate for stair " + (upOrDown ? "up" : "down"));
		return result;
	}

	protected abstract Iterator<Coord> candidates0(Coord objective);

	private Coord getObjective() {
		return initialObjective == null ? getObjective0() : initialObjective;
	}

	/**
	 * @return Where to generate the stair, approximately. It must be within a room
	 *         or corridor.
	 */
	protected abstract Coord getObjective0();

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
	 * @param inverted
	 * @return A {@link Comparator} that orders {@link Coord coords} according to
	 *         their distance to {@code c} (by closeness if {@code inverted} is
	 *         {@code false}, otherwise by distance).
	 */
	protected static Comparator<Coord> newDistanceComparatorFrom(final Coord c, final boolean inverted) {
		return new Comparator<Coord>() {
			@Override
			public int compare(Coord o1, Coord o2) {
				final int base = Double.compare(o1.distance(c), o2.distance(c));
				return inverted ? -base : base;
			}
		};
	}

}
