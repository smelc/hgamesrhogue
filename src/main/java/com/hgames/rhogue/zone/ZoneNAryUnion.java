package com.hgames.rhogue.zone;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * @author smelC
 */
public class ZoneNAryUnion extends Zone.Skeleton implements Zone {

	protected final List<Zone> union;

	private static final long serialVersionUID = 927176890730965063L;

	/**
	 * @param union
	 *            Disjoint zones.
	 */
	public ZoneNAryUnion(List<Zone> union) {
		assert Zones.allDisjoint(union);
		this.union = union;
	}

	/**
	 * This call may capture {@code union}.
	 * 
	 * @param union
	 * @return The nary union of {@code union}, being smart if possible.
	 */
	public static Zone create(List<Zone> union) {
		switch (union.size()) {
		case 0:
			return EmptyZone.INSTANCE;
		case 1:
			return union.get(0);
		case 2:
			return new ZoneUnion(union.get(0), union.get(1));
		default:
			return new ZoneNAryUnion(union);
		}
	}

	/**
	 * This call may capture {@code u1} and {@code u2}.
	 * 
	 * @param u1
	 * @param u2
	 * @return The nary union of {@code union}, being smart if possible.
	 */
	public static Zone create(List<Zone> u1, List<Zone> u2) {
		if (u1.isEmpty())
			return create(u2);
		if (u2.isEmpty())
			return create(u1);
		final List<Zone> all = new ArrayList<Zone>(u1.size() + u2.size());
		all.addAll(u1);
		all.addAll(u2);
		return create(all);
	}

	@Override
	public Iterator<Coord> iterator() {
		final Iterator<Zone> it = union.iterator();
		return new Iterator<Coord>() {

			Iterator<Coord> current = null;

			@Override
			public boolean hasNext() {
				while (current == null || !current.hasNext()) {
					if (!it.hasNext())
						return false;
					final Zone next = it.next();
					current = next.iterator();
				}
				assert current != null && current.hasNext();
				return true;
			}

			@Override
			public Coord next() {
				if (hasNext())
					return current.next();
				else
					throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		};
	}

	@Override
	public boolean isEmpty() {
		final int nbu = union.size();
		for (int i = 0; i < nbu; i++) {
			final Zone sub = union.get(i);
			if (!sub.isEmpty())
				return false;
		}
		return true;
	}

	@Override
	public int size() {
		int result = 0;
		final int nbu = union.size();
		for (int i = 0; i < nbu; i++) {
			final Zone sub = union.get(i);
			result += sub.size();
		}
		return result;
	}

	@Override
	public boolean contains(int x, int y) {
		final int nbu = union.size();
		for (int i = 0; i < nbu; i++) {
			final Zone sub = union.get(i);
			if (sub.contains(x, y))
				return true;
		}
		return false;
	}

	@Override
	public boolean contains(Coord c) {
		return contains(c.x, c.y);
	}

	@Override
	public boolean intersectsWith(Zone other) {
		final int nbu = union.size();
		for (int i = 0; i < nbu; i++) {
			final Zone sub = union.get(i);
			if (sub.intersectsWith(other))
				return true;
		}
		return false;
	}

	@Override
	public /* @Nullable */ Coord getCenter() {
		final int nbu = union.size();
		if (nbu == 0)
			return null;
		assert 0 < nbu;
		int x = 0;
		int y = 0;
		for (int i = 0; i < nbu; i++) {
			final Zone sub = union.get(i);
			final Coord scenter = sub.getCenter();
			x += scenter.x;
			y += scenter.y;
		}
		return Coord.get(x / nbu, y / nbu);
	}

	@Override
	public List<Coord> getAll(boolean fresh) {
		final List<Coord> result = new ArrayList<Coord>(size());
		final int nbu = union.size();
		for (int i = 0; i < nbu; i++) {
			final Zone sub = union.get(i);
			result.addAll(sub.getAll(false));
		}
		return result;
	}

	@Override
	public Coord getRandom(IRNG rng) {
		final int nbu = union.size();
		return nbu == 0 ? null : rng.getRandomElement(union).getRandom(rng);
	}

	@Override
	public Zone translate(Coord c) {
		return translate(c.x, c.y);
	}

	@Override
	public Zone getDelegate() {
		return this;
	}

	@Override
	public Zone union(Zone other) {
		if (union.contains(other))
			/* No change */
			return this;
		else {
			final List<Zone> bigger = new ArrayList<Zone>(union.size() + 1);
			bigger.addAll(union);
			bigger.add(other);
			return new ZoneNAryUnion(bigger);
		}
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		result.append(getClass().getSimpleName());
		result.append("[");
		final int nbu = union.size();
		for (int i = 0; i < nbu; i++) {
			final Zone sub = union.get(i);
			result.append(sub.toString());
			if (i < nbu - 1)
				result.append("|");
		}
		result.append("]");
		return result.toString();
	}

}
