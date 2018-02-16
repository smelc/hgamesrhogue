package com.hgames.rhogue.generation.map.stair;

import java.util.Iterator;
import java.util.PriorityQueue;

import com.hgames.lib.collection.Queues;
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.grid.GridIterators;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * A bad stair generator, which is kept for reference (and because it doesn't
 * need rooms to be generated, contrary to {@link StairGenerator}) . It is bad
 * because it doesn't enforce stairs to be far away.
 * 
 * @author smelC
 */
public class BadStairGenerator extends AbstractStairGenerator {

	/**
	 * @param logger
	 * @param rng
	 *            The rng to use.
	 * @param dungeon
	 *            The dungeon for which the stair should be generated.
	 * @param objective
	 *            Where to put the stair, approximately
	 * @param upOrDown
	 */
	public BadStairGenerator(ILogger logger, IRNG rng, Dungeon dungeon, Coord objective, boolean upOrDown) {
		super(logger, rng, dungeon, objective, upOrDown);
	}

	@Override
	protected Coord getObjective0() {
		final /* @Nullable */ Coord other = dungeon.getStair(!upOrDown);
		if (other == null) {
			final Coord result = getRandomCell(null);
			if (logger != null && logger.isInfoEnabled())
				logger.infoLog(Tags.GENERATION,
						(upOrDown ? "upward" : "downward") + " stair objective chosen randomly");
			return result;
		} else {
			final int random = rng.nextInt(4);
			switch (random) {
			case 0:
			case 1:
			case 2: {
				final Direction otherDir = getDirectionFromMapCenter(other);
				if (logger != null && logger.isInfoEnabled())
					logger.infoLog(Tags.GENERATION, "other stair is in direction: " + otherDir);
				final int disturb = rng.nextInt(2);
				Direction chosenDir = otherDir.opposite();
				if (1 == disturb)
					chosenDir = chosenDir.clockwise();
				else if (2 == disturb)
					chosenDir = chosenDir.counterClockwise();
				final Coord result = getRandomCell(chosenDir);
				if (logger != null && logger.isInfoEnabled())
					logger.infoLog(Tags.GENERATION, (upOrDown ? "upward" : "downward")
							+ " stair objective chosen in direction: " + chosenDir);
				return result;
			}
			case 3:
				if (logger != null && logger.isInfoEnabled())
					logger.infoLog(Tags.GENERATION,
							(upOrDown ? "upward" : "downward") + " stair objective chosen randomly");
				return getRandomCell(null);
			default:
				throw new IllegalStateException(
						"Rng is incorrect. Received " + random + " when calling nextInt(4)");
			}
		}
	}

	/**
	 * @param center
	 * @return Candidates for stairs close to {@code center}. Or null.
	 */
	@Override
	protected Iterator<Coord> candidates0(Coord center) {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		final int rSize = ((width + height) / 6) + 1;
		final Iterator<Coord> it = new GridIterators.GrowingRectangle(center, rSize);
		final PriorityQueue<Coord> queue = new PriorityQueue<Coord>(rSize * 4,
				newDistanceComparatorFrom(center, false));
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
		return Queues.iteratorOf(queue);
	}

	protected final Direction getDirectionFromMapCenter(Coord c) {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		final Coord center = Coord.get(width / 2, height / 2);
		return Direction.getCardinalDirection(c.x - center.x, c.y - center.y);
	}

}
