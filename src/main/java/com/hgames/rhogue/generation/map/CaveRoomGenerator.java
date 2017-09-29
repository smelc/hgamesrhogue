package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.hgames.lib.ByRef;
import com.hgames.lib.Ints;
import com.hgames.rhogue.grid.DoerInACircle;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.ListZone;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A room generator that generates caves.
 * 
 * @author smelC
 */
public class CaveRoomGenerator extends SkeletalRoomGenerator {

	protected final RNG rng;

	/**
	 * The degree of caveness, 0 being the minimum (yielding a rectangle room), 100
	 * being the maximum.
	 */
	protected int caveness;

	/**
	 * @param rng
	 *            The RNG to use.
	 * @param caveness
	 *            The caveness level to use. Must be in [0, 100].
	 * @throws IllegalStateException
	 *             If {@code caveness} is not in {@code [0, 100]}.
	 */
	public CaveRoomGenerator(RNG rng, int caveness) {
		this.rng = rng;
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
	public Zone generate(final Dungeon dungeon, final Coord translation, int maxWidth, int maxHeight) {
		final RectangleRoomGenerator delegate = new RectangleRoomGenerator(rng);
		final Rectangle rectangle = delegate.generate(null, null, maxWidth, maxHeight);
		if (rectangle == null) {
			/* Should not happen */
			assert false;
			return null;
		}
		if (caveness == 0)
			return rectangle;
		final int rsz = rectangle.size();
		final int zmsz = getZoneMinSize();
		if (rsz <= zmsz)
			/* Do not shrink it */
			return rectangle;
		final Set<Coord> all = new LinkedHashSet<Coord>(rectangle.getAll());
		/* Should not be eaten */
		final Coord center = rectangle.getCenter();
		int eaters = (caveness / 10) + 1;
		final List<Coord> internalBorder = rectangle.getInternalBorder();
		final int boundInit = 8;
		final ByRef<Integer> bound = ByRef.<Integer>make(boundInit);
		final DoerInACircle doer = new DoerInACircle() {
			@Override
			protected boolean doOnACell(int x, int y) {
				final Coord c = Coord.get(x, y);
				if (!center.equals(c)) {
					final boolean rmed = all.remove(c);
					if (rmed) {
						System.out.println("Carved a cell");
						bound.set(bound.get() - 1);
						dungeon.getBuilder().setSymbol(c.add(translation), DungeonSymbol.GRASS);
					}
				}
				/* else do not eat the center */

				/* Continue if not at min size */
				return all.size() <= zmsz && 0 < bound.get();
			}
		};
		final int minRadius = 1;
		final int maxRadius = rectangle.size() / 10;
		while (0 < eaters && zmsz < all.size()) {
			eaters--;
			final Coord start = rng.getRandomElement(internalBorder);
			if (!all.contains(start))
				/* Eaten already */
				continue;
			final int radius = rng.between(minRadius, maxRadius);
			System.out.println("Carving in circle of radius: " + radius);
			doer.doInACircle(start.x, start.y, radius);
			bound.set(boundInit);
		}
		for (Coord c : rectangle) {
			if (!all.contains(c))
				/* It got carved */
				dungeon.getBuilder().setSymbol(c.add(translation), DungeonSymbol.CHASM);
		}
		assert all.size() <= rectangle.size();
		final boolean change = all.size() < rectangle.size();
		assert !change || all.size() < rectangle.getAll().size();
		return change ? new ListZone(new ArrayList<Coord>(all)) : rectangle;
	}

	/** @return The size under which a zone cannot be carved anymore */
	protected int getZoneMinSize() {
		return 4;
	}

	protected boolean roll() {
		return rng.nextInt(101) <= caveness;
	}
}
