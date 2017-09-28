package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * A zone backed up by a {@link Set}. It's good if you call
 * {@link Zone#contains(Coord)} a lot on it.
 * 
 * @author smelC
 */
public class SetZone extends Zone.Skeleton implements Zone {

	protected final Set<Coord> set;

	private static final long serialVersionUID = -4601216047166803945L;

	/**
	 * @param set
	 *            The set to delegate to.
	 */
	public SetZone(Set<Coord> set) {
		this.set = set;
	}

	@Override
	public boolean isEmpty() {
		return set.isEmpty();
	}

	@Override
	public int size() {
		return set.size();
	}

	@Override
	public Iterator<Coord> iterator() {
		return set.iterator();
	}

	@Override
	public boolean contains(Coord c) {
		return set.contains(c);
	}

	@Override
	public boolean contains(int x, int y) {
		return contains(Coord.get(x, y));
	}

	@Override
	public boolean contains(Zone other) {
		if (other instanceof SetZone) {
			final SetZone other_ = (SetZone) other;
			/* Avoid allocating the List backing getAll() in 'other' */
			return set.containsAll(other_.set);
		} else
			/* Avoid allocating the List backing getAll() in 'this' */
			return set.containsAll(other.getAll());
	}

	@Override
	public List<Coord> getAll() {
		return new ArrayList<Coord>(set);
	}

}
