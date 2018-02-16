package com.hgames.rhogue.generation.map.stair;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

import com.hgames.lib.Ints;
import com.hgames.lib.Predicate;
import com.hgames.lib.choice.DoublePriorityCell;
import com.hgames.lib.iterator.Iterators;
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.generation.map.Dungeon;
import com.hgames.rhogue.generation.map.DungeonGenerator;
import com.hgames.rhogue.generation.map.DungeonZonesCrawler;
import com.hgames.rhogue.generation.map.Dungeons;
import com.hgames.rhogue.generation.map.ICellToZone;
import com.hgames.rhogue.generation.map.connection.IConnectionFinder;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * A stair generator that uses the existing rooms to place stairs. It makes sure
 * that stairs aren't too close.
 * 
 * @author smelC
 */
public class StairGenerator extends AbstractStairGenerator {

	protected final DungeonGenerator gen;
	protected final ICellToZone containerFinder;
	protected final IConnectionFinder connections;

	private static final DoublePriorityCell<Zone> DP_CELL = DoublePriorityCell.createEmptyInverted();

	/**
	 * @param logger
	 * @param rng
	 * @param gen
	 * @param dungeon
	 * @param objective
	 * @param upOrDown
	 * @param containerFinder
	 *            A function which given a Coord, gives its enclosing Zone.
	 * @param connections
	 */
	public StairGenerator(ILogger logger, IRNG rng, DungeonGenerator gen, Dungeon dungeon, Coord objective,
			boolean upOrDown, ICellToZone containerFinder, IConnectionFinder connections) {
		super(logger, rng, dungeon, objective, upOrDown);
		this.gen = gen;
		this.containerFinder = containerFinder;
		this.connections = connections;
	}

	@Override
	protected Coord getObjective0() {
		final /* @Nullable */ Coord other = dungeon.getStair(!upOrDown);
		if (other == null) {
			/*
			 * Choose a zone that is not in the center of the dungeon and take its center.
			 */
			final int width = dungeon.getWidth();
			final int height = dungeon.getHeight();
			final List<Zone> rooms = dungeon.getRooms();
			final int nbr = rooms.size();
			/* The dungeon's center */
			final Coord dcenter = Coord.get(width / 2, height / 2);
			DP_CELL.clear();
			final int reachObjective = getMinStairComponentSize();
			for (int i = 0; i < nbr; i++) {
				final Zone room = rooms.get(i);
				final Coord rcenter = room.getCenter();
				if (!room.contains(rcenter))
					/* A shallow room */
					continue;
				if (!reachesAtLeast(room, reachObjective))
					/* This zone isn't connected enough */
					continue;
				if (!isCenteredDungeonWise(rcenter, width, height))
					return rcenter;
				DP_CELL.union(room, rcenter.distance(dcenter));
			}
			/*
			 * All rooms are in the center, let's take the one the farther away
			 */
			final Zone best = DP_CELL.get();
			DP_CELL.clear();
			return best == null ? null : best.getCenter();
		} else {
			final List<Zone> rooms = dungeon.getRooms();
			final int nbr = rooms.size();
			/*
			 * Order zones by distance from 'other'. Better zone is the one farther away.
			 */
			final PriorityQueue<Zone> candidates = new PriorityQueue<Zone>(nbr, new Comparator<Zone>() {
				@Override
				public int compare(Zone z1, Zone z2) {
					final int base = Double.compare(distance(z1), distance(z2));
					/* Invert, because we look for the zone farther away */
					return -base;
				}

				private double distance(Zone z) {
					return z.getCenter().distance(other);
				}
			});
			candidates.addAll(rooms);
			return candidates.isEmpty() ? null : candidates.remove().getCenter();
		}
	}

	@Override
	protected Iterator<Coord> candidates0(Coord objective) {
		final Zone start = findContainer(objective);
		if (start == null)
			return Collections.emptyIterator();
		final Iterator<Zone> delegate_ = new DungeonZonesCrawler(dungeon, start).iterator();
		/* Filter away Zone whose IRoomGenerator specifies !isAcceptingStairs() */
		final Iterator<Zone> delegate = Iterators.filter(delegate_, new Predicate<Zone>() {
			@Override
			public boolean apply(Zone z) {
				final IRoomGenerator rg = gen.getRoomGenerator(z);
				if (rg == null) {
					assert dungeon.getCorridors().contains(z);
					/* We don't want stairs in corridors */
					return false;
				}
				return rg.isAcceptingStairs();
			}
		});
		final Iterator<Coord> base = new Iterator<Coord>() {

			private Iterator<Coord> current;

			@Override
			public boolean hasNext() {
				if (current == null || !current.hasNext()) {
					if (delegate.hasNext()) {
						current = delegate.next().getExternalBorder().iterator();
						return hasNext();
					} else
						return false;
				} else {
					assert current.hasNext();
					return true;
				}
			}

			@Override
			public Coord next() {
				if (current == null || !current.hasNext()) {
					if (delegate.hasNext()) {
						current = delegate.next().getExternalBorder().iterator();
						return next();
					} else
						throw new NoSuchElementException();
				} else {
					assert current.hasNext();
					return current.next();
				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return Iterators.filter(base, new Predicate<Coord>() {
			@Override
			public boolean apply(Coord c) {
				return isValidCandidate(c);
			}
		});
	}

	protected Zone findContainer(Coord c) {
		Zone result = containerFinder.get(c);
		if (result == null) {
			/* Fallback (bad) */
			assert false : "Zone containing " + c + " cannot be found";
			result = Dungeons.findRoomOrCorridorContaining(dungeon, c.x, c.y);
		}
		return result;
	}

	protected boolean areConnected(Zone z0, Zone z1) {
		return connections.areConnected(z0, z1, Integer.MAX_VALUE);
	}

	/** @return The minimum size of a zone connected to a stair */
	protected int getMinStairComponentSize() {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		return (width * height) / 32;
	}

	/**
	 * @param z
	 * @param objective
	 * @return Whether at least {@code objective} cells are reachable from
	 *         {@code z}.
	 */
	private boolean reachesAtLeast(Zone z, int objective) {
		int result = z.size();
		if (objective <= result)
			return true;
		final Iterator<Zone> connectedsToZ = new DungeonZonesCrawler(dungeon, z).iterator();
		while (connectedsToZ.hasNext()) {
			result += connectedsToZ.next().size();
			if (objective <= result)
				return true;
		}
		return false;
	}

	/**
	 * @param c
	 * @param width
	 *            The dungeon's width
	 * @param height
	 *            The dungeon's height
	 */
	private static boolean isCenteredDungeonWise(Coord c, int width, int height) {
		final int xmin = width / 3;
		final int xmax = xmin * 2;
		if (Ints.inInterval(xmin, c.x, xmax))
			return true;
		final int ymin = height / 3;
		final int ymax = ymin * 2;
		if (Ints.inInterval(ymin, c.y, ymax))
			return true;
		return false;
	}

}
