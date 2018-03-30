package com.hgames.rhogue.zone;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

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
	protected transient /* @Nullable */ List<Coord> externalBorder;
	protected transient /* @Nullable */ Zone extension;
	protected transient /* @Nullable */ Zone shrunk;

	private static final long serialVersionUID = 605447640108799431L;

	/**
	 * @param delegate
	 *            The zone delegated to. It should not be muted while {@code this}
	 *            is used otherwise discrepancies will occur.
	 */
	public CachingZone(Zone delegate) {
		if (delegate == null)
			throw new NullPointerException(
					getClass().getSimpleName() + ": Delegate " + Zone.class.getSimpleName() + " cannot be null");
		this.delegate = delegate;
	}

	@SuppressWarnings("deprecation")
	@Override
	public Zone add(Coord c) {
		return new CachingZone(delegate.add(c));
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
			cached = Boolean.valueOf(delegate.contains(c));
			assert cached != null;
			if (contained == null)
				contained = new HashMap<Coord, Boolean>();
			contained.put(c, cached);
		}
		assert cached != null;
		return cached.booleanValue();
	}

	@Override
	public boolean contains(Zone other) {
		Boolean cached = contains == null ? null : contains.get(other);
		if (cached == null) {
			cached = Boolean.valueOf(delegate.contains(other));
			assert cached != null;
			if (contains == null)
				contains = new HashMap<Zone, Boolean>();
			contains.put(other, cached);
		}
		return cached.booleanValue();
	}

	@Override
	public boolean intersectsWith(Zone other) {
		Boolean cached = intersectsWith == null ? null : intersectsWith.get(other);
		if (cached == null) {
			cached = Boolean.valueOf(delegate.intersectsWith(other));
			assert cached != null;
			if (intersectsWith == null)
				intersectsWith = new HashMap<Zone, Boolean>();
			intersectsWith.put(other, cached);
		}
		return cached.booleanValue();
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
				smallestX = Integer.valueOf(delegate.x(smallestOrBiggest));
			return smallestX.intValue();
		} else {
			if (biggestX == null)
				biggestX = Integer.valueOf(delegate.x(smallestOrBiggest));
			return biggestX.intValue();
		}
	}

	@Override
	public int y(boolean smallestOrBiggest) {
		if (smallestOrBiggest) {
			if (smallestY == null)
				smallestY = Integer.valueOf(delegate.y(smallestOrBiggest));
			return smallestY.intValue();
		} else {
			if (biggestY == null)
				biggestY = Integer.valueOf(delegate.y(smallestOrBiggest));
			return biggestY.intValue();
		}
	}

	@Override
	public List<Coord> getAll() {
		if (all == null)
			all = delegate.getAll();
		return all;
	}

	@Override
	public Coord getRandom(IRNG rng) {
		return delegate.getRandom(rng);
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
	public List<Coord> getExternalBorder() {
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
	public Zone shrink() {
		if (shrunk == null)
			shrunk = delegate.shrink();
		return shrunk;
	}

	@Override
	public Zone getDelegate() {
		return delegate.getDelegate();
	}

	@SuppressWarnings("deprecation")
	@Override
	public Zone remove(Coord c) {
		return new CachingZone(delegate.remove(c));
	}

	@Override
	public Zone union(Zone other) {
		return new CachingZone(delegate.union(other));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + delegate.toString() + "]";
	}
}
