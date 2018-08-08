package com.hgames.rhogue.generation.map.dungeon;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hgames.rhogue.grid.Grids;
import com.hgames.rhogue.zone.ListZone;

import squidpony.squidmath.Coord;

/**
 * Helper to {@link DungeonGenerator}, to weightwatcher its API. Don't make me
 * public.
 * 
 * @author smelC
 */
class DungeonGeneratorHelper {

	/**
	 * Removes lonelies from {@code z}.
	 * 
	 * @param doer
	 *            An optional callback called on cells removed.
	 */
	static void replaceLonelies(ListZone z, /* @Nullable */ CellDoer doer) {
		final List<Coord> all = z.getState();
		while (true) {
			final int effect = replaceLonelies(all, doer);
			if (effect == 0)
				break;
		}
	}

	/**
	 * Removes lonelies from {@code coords}.
	 * 
	 * @param doer
	 *            An optional callback called on cells removed.
	 * @return The number of cells removed
	 */
	private static int replaceLonelies(Collection<? extends Coord> coords, /* @Nullable */ CellDoer doer) {
		final Iterator<? extends Coord> it = coords.iterator();
		int result = 0;
		while (it.hasNext()) {
			final Coord c = it.next();
			if (Grids.borderness(coords, c) >= 7) {
				it.remove();
				result++;
				if (doer != null)
					doer.doOnCell(c);
			}
		}
		return result;
	}

}
