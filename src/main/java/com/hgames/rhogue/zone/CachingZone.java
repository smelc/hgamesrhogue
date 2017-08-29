package com.hgames.rhogue.zone;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * A wrapper around a {@link Zone} that caches potentially expensive
 * computations.
 * 
 * @author smelC
 */
public class CachingZone implements Zone {

	protected final Zone delegate;
	protected transient /* @Nullable */ Map<Coord, Boolean> contained;
	protected transient /* @Nullable */ Map<Zone, Boolean> contains;
	protected transient /* @Nullable */ Map<Zone, Boolean> intersectsWith;
	protected transient /* @Nullable */ Coord center;

	protected transient /* @Nullable */ Integer smallestX;
	protected transient /* @Nullable */ Integer biggestX;
	protected transient /* @Nullable */ Integer smallestY;
	protected transient /* @Nullable */ Integer biggestY;

	protected transient /* @Nullable */ List<Coord> all;
	protected transient /* @Nullable */ List<Coord> internalBorder;
	protected transient /* @Nullable */ Collection<Coord> externalBorder;
	protected transient /* @Nullable */ Zone extension;

	private static final long serialVersionUID = 605447640108799431L;

	/**
	 * @param delegate
	 *            The zone delegated to. It should not be muted while
	 *            {@code this} is used otherwise discrepancies will occur.
	 */
	public CachingZone(Zone delegate) {
		this.delegate = delegate;
	}

	@Override
	public Iterator<Coord> iterator() {
		return delegate.iterator();
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public boolean contains(int x, int y) {
		return contains(Coord.get(x, y));
	}

	@Override
	public boolean contains(Coord c) {
		Boolean cached = contained == null ? null : contained.get(c);
		if (cached == null) {
			cached = delegate.contains(c);
			assert cached != null;
			if (contained == null)
				contained = new HashMap<Coord, Boolean>();
			contained.put(c, cached);
		}
		assert cached != null;
		return cached;
	}

	@Override
	public boolean contains(Zone other) {
		Boolean cached = contains == null ? null : contains.get(other);
		if (cached == null) {
			cached = delegate.contains(other);
			assert cached != null;
			if (contains == null)
				contains = new HashMap<Zone, Boolean>();
			contains.put(other, cached);
		}
		return cached;
	}

	@Override
	public boolean intersectsWith(Zone other) {
		Boolean cached = intersectsWith == null ? null : intersectsWith.get(other);
		if (cached == null) {
			cached = delegate.intersectsWith(other);
			assert cached != null;
			if (intersectsWith == null)
				intersectsWith = new HashMap<Zone, Boolean>();
			intersectsWith.put(other, cached);
		}
		return cached;
	}

	@Override
	public Coord getCenter() {
		if (center == null)
			center = delegate.getCenter();
		assert center != null;
		return center;
	}

	@Override
	public int getWidth() {
		return delegate.getWidth();
	}

	@Override
	public int getHeight() {
		return delegate.getHeight();
	}

	@Override
	public double getDiagonal() {
		return delegate.getHeight();
	}

	@Override
	public int x(boolean smallestOrBiggest) {
		if (smallestOrBiggest) {
			if (smallestX == null)
				smallestX = delegate.x(smallestOrBiggest);
			return smallestX;
		} else {
			if (biggestX == null)
				biggestX = delegate.x(smallestOrBiggest);
			return biggestX;
		}
	}

	@Override
	public int y(boolean smallestOrBiggest) {
		if (smallestOrBiggest) {
			if (smallestY == null)
				smallestY = delegate.y(smallestOrBiggest);
			return smallestY;
		} else {
			if (biggestY == null)
				biggestY = delegate.y(smallestOrBiggest);
			return biggestY;
		}
	}

	@Override
	public List<Coord> getAll() {
		if (all == null)
			all = delegate.getAll();
		return all;
	}

	@Override
	public Zone translate(Coord c) {
		return new CachingZone(delegate.translate(c));
	}

	@Override
	public Zone translate(int x, int y) {
		return new CachingZone(delegate.translate(x, y));
	}

	@Override
	public List<Coord> getInternalBorder() {
		if (internalBorder == null)
			internalBorder = delegate.getInternalBorder();
		return internalBorder;
	}

	@Override
	public Collection<Coord> getExternalBorder() {
		if (externalBorder == null)
			externalBorder = delegate.getExternalBorder();
		return externalBorder;
	}

	@Override
	public Zone extend() {
		if (extension == null)
			extension = delegate.extend();
		return extension;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + delegate.toString() + "]";
	}
}