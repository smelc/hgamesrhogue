package com.hgames.rhogue.generation.map.rgenerator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

import com.hgames.lib.Ints;
import com.hgames.lib.collection.set.Sets;
import com.hgames.rhogue.generation.map.RoomComponent;
import com.hgames.rhogue.zone.ListZone;
import com.hgames.rhogue.zone.Rectangle;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * A room generator that generates caves.
 * 
 * @author smelC
 */
public class CaveRoomGenerator extends SkeletalRoomGenerator {

	/**
	 * The degree of caveness, 0 being the minimum (yielding a rectangle room), 100
	 * being the maximum.
	 */
	protected int caveness = 50;

	private /* @Nullable */ LinkedList<Coord> todos;

	private static final Direction[] DIRS_BUF = new Direction[4];

	static {
		for (int i = 0; i < 4; i++)
			DIRS_BUF[i] = Direction.CARDINALS[i];
	}

	/**
	 * @param caveness
	 *            The caveness level to use. Must be in [0, 100].
	 * @throws IllegalStateException
	 *             If {@code caveness} is not in {@code [0, 100]}.
	 */
	public CaveRoomGenerator(int caveness) {
		setCaveness(caveness);
	}

	/**
	 * @param caveness
	 *            The caveness level to use. Must be in [0, 100].
	 * @return {@code this}.
	 */
	public CaveRoomGenerator setCaveness(int caveness) {
		if (!Ints.inInterval(0, caveness, 100))
			throw new IllegalStateException("Excepted caveness level to be with [0, 100]. Received: " + caveness);
		this.caveness = caveness;
		return this;
	}

	@Override
	public Zone generate(IRNG rng, RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		final RectangleRoomGenerator delegate = new RectangleRoomGenerator();
		final Rectangle rectangle = delegate.generate(rng, component, translation, maxWidth, maxHeight);
		if (rectangle == null) {
			/* Should not happen */
			assert false;
			return null;
		}
		if (caveness == 0)
			return rectangle;
		final int rsz = rectangle.size();
		if (rsz <= getZoneMinSize())
			/* Do not shrink it */
			return rectangle;
		final Set<Coord> all = Sets.newLinkedHashSet(rectangle.iterator(), rsz);
		boolean change = false;
		/* The maximum number of cells to carve at every corner */
		final int maxCarvingPerCorner = (rsz / 8) + 1;
		for (Direction dir : Direction.DIAGONALS) {
			if (!roll(rng))
				continue;
			// XXX CH Try all 8 ""corners"" ? <- YES DO IT
			final Coord start = Rectangle.Utils.getCorner(rectangle, dir);
			assert rectangle.contains(start);
			if (!all.contains(start))
				/* Corner got eaten already */
				continue;
			if (todos == null)
				todos = new LinkedList<Coord>();
			else
				assert todos.isEmpty();
			change |= carve(rng, all, start, maxCarvingPerCorner);
			assert todos.isEmpty();
		}
		return change ? new ListZone(new ArrayList<Coord>(all)) : rectangle;
	}

	/** @return The size at which a zone cannot be carved anymore */
	protected int getZoneMinSize() {
		return 4;
	}

	protected boolean roll(IRNG rng) {
		return rng.nextInt(101) <= caveness;
	}

	/**
	 * @param zone
	 *            The zone to carve.
	 * @param start
	 *            The carving starting point (a corner).
	 * @return Whether something was carved.
	 */
	private boolean carve(IRNG rng, Collection<Coord> zone, Coord start, int maxCarvingPerCorner) {
		assert todos.isEmpty();
		todos.add(start);
		int carved = 0;
		final int mzsz = getZoneMinSize();
		while (!todos.isEmpty() && carved < maxCarvingPerCorner && mzsz < zone.size()) {
			final Coord next = todos.remove();
			if (!zone.contains(next))
				continue;
			zone.remove(next);
			carved++;
			if (!roll(rng))
				/* Stop here */
				break;
			// Not using in place shuffle, for gwt-compatibility
			rng.shuffle(Direction.CARDINALS, DIRS_BUF);
			for (Direction dir : DIRS_BUF) {
				final Coord candidate = next.translate(dir);
				if (zone.contains(candidate))
					todos.add(candidate);
			}
		}
		todos.clear();
		return 0 < carved;
	}
}
