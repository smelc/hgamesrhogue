package com.hgames.rhogue.zone;

import java.util.List;

import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A zone defined by a {@link List}.
 * 
 * @author smelC
 */
public class ListZone extends Zone.Skeleton {

	protected final List<Coord> coords;

	private static final long serialVersionUID = 1166468942544595692L;

	/**
	 * A fresh zone that <b>captures</b> the given list.
	 * 
	 * @param coords
	 */
	public ListZone(List<Coord> coords) {
		this.coords = coords;
	}

	@Override
	public boolean isEmpty() {
		return coords.isEmpty();
	}

	@Override
	public int size() {
		return coords.size();
	}

	@Override
	public boolean contains(Coord c) {
		return coords.contains(c);
	}

	@Override
	public boolean contains(int x, int y) {
		return coords.contains(Coord.get(x, y));
	}

	@Override
	public List<Coord> getAll() {
		return coords;
	}

	@Override
	public Coord getRandom(RNG rng) {
		return rng.getRandomElement(coords);
	}

	/**
	 * @return The list that backs up {@code this}. Use at your own risks.
	 */
	public List<Coord> getState() {
		return coords;
	}

	@Override
	public String toString() {
		return coords.toString();
	}
}