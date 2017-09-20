package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.List;

import com.hgames.rhogue.generation.map.draw.IDungeonDrawer;

/**
 * A drawer that stores dungeon being built efficiently, for retrieving them
 * later on.
 * 
 * @author smelC
 */
public class DungeonProgression implements IDungeonDrawer {

	private Progression[][] progression;

	@Override
	public void draw(DungeonSymbol[][] dungeon) {
		final int width = dungeon.length;
		final int height = width == 0 ? 0 : dungeon[0].length;
		if (progression == null) {
			progression = new Progression[width][height];
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++)
					progression[x][y] = new Progression();
			}
		} else {
			/* Check consistency */
			final int pwidth = progression.length;
			if (width != pwidth)
				throw new IllegalStateException("Witnessing two different widths in dungeon progression: "
						+ pwidth + " and then " + width);
			final int pheight = pwidth == 0 ? 0 : progression[0].length;
			if (height != pheight)
				throw new IllegalStateException("Witnessing two different heights in dungeon progression: "
						+ pheight + " and then " + height);
		}

		for (int x = 0; x < width; x++) {
			for (int y = height - 1; 0 <= y; y--)
				progression[x][y].extend(dungeon[x][y]);
		}
	}

	/**
	 * @return A structure encoding the progression of the dungeons received by
	 *         {@link #draw(DungeonSymbol[][])}.
	 */
	public /* @Nullable */ Progression[][] getProgression() {
		return progression;
	}

	/** @return Whether {@link #getProgression()} contains something. */
	public boolean isEmpty() {
		if (progression == null)
			return true;
		if (progression.length == 0)
			return true;
		if (progression[0].length == 0)
			return true;
		return progression[0][0].isEmpty();
	}

	/**
	 * Efficient storing of the progression of a value that changes rarely.
	 * 
	 * @author smelC
	 */
	public static final class Progression {

		private final List<Atom<DungeonSymbol>> list;
		private int size;

		protected Progression() {
			this.list = new ArrayList<Atom<DungeonSymbol>>();
			this.size = 0;
		}

		protected void extend(DungeonSymbol value) {
			final int sz = list.size();
			if (sz == 0)
				list.add(new Atom<DungeonSymbol>(value));
			else {
				final Atom<DungeonSymbol> last = list.get(sz - 1);
				if (equals(value, last.value))
					last.extend();
				else
					list.add(new Atom<DungeonSymbol>(value));
			}
			size++;
		}

		/**
		 * @param idx
		 * @return Elements at {@code idx}
		 * @throws IndexOutOfBoundsException
		 *             if the index is out of range (
		 *             <tt>index &lt; 0 || index &gt;= size()</tt>)
		 * 
		 */
		public DungeonSymbol get(int idx) {
			if (idx < 0)
				throw new IndexOutOfBoundsException();
			int jdx = idx;
			final int sz = list.size();
			for (int i = 0; i < sz; i++) {
				final Atom<DungeonSymbol> atom = list.get(i);
				if (jdx <= atom.repeats)
					return atom.value;
				jdx -= atom.repeats;
			}
			throw new IndexOutOfBoundsException();
		}

		/** @return The number of elements. */
		public int size() {
			return size;
		}

		/** @return Whether this progression is empty. */
		public boolean isEmpty() {
			return size() == 0;
		}

		protected static <T> boolean equals(T t1, T t2) {
			if (t1 == null)
				return t2 == null;
			else
				return t1.equals(t2);
		}

		@Override
		public String toString() {
			return list.toString();
		}

		/**
		 * A repetition.
		 * 
		 * @author smelC
		 */
		private static final class Atom<T> {

			private T value;
			private int repeats;

			private Atom(T value) {
				this.value = value;
				this.repeats = 1;
			}

			private void extend() {
				this.repeats++;
			}

			@Override
			public String toString() {
				return value.toString() + "x" + repeats;
			}
		}

	}
}
