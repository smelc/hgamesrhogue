package com.hgames.rhogue.zone;

import java.util.Collections;
import java.util.List;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * @author smelC
 */
public final class EmptyZone extends Zone.Skeleton implements Zone {

	/** The singleton empty zone. */
	public static final Zone INSTANCE = new EmptyZone();

	private static final long serialVersionUID = 1298295201124745841L;

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean contains(int x, int y) {
		return false;
	}

	@Override
	public boolean contains(Coord c) {
		return false;
	}

	@Override
	public List<Coord> getAll() {
		return Collections.emptyList();
	}

}