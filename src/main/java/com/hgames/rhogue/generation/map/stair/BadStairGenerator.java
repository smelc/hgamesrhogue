package com.hgames.rhogue.generation.map.stair;

import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.Dungeon;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A bad stair generator, which is kept for reference. It is bad because it
 * doesn't enforce stairs to be far away.
 * 
 * @author smelC
 */
public class BadStairGenerator extends AbstractStairGenerator {

	/**
	 * @param logger
	 * @param rng
	 * @param dungeon
	 * @param objective
	 * @param upOrDown
	 */
	public BadStairGenerator(ILogger logger, RNG rng, Dungeon dungeon, Coord objective, boolean upOrDown) {
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

	protected final Direction getDirectionFromMapCenter(Coord c) {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		final Coord center = Coord.get(width / 2, height / 2);
		return Direction.getCardinalDirection(c.x - center.x, c.y - center.y);
	}

}
