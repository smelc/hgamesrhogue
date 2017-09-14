package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import squidpony.squidgrid.zone.Zone;

/**
 * An iterator over zones of a dungeon, that starts from a zone, and then visits
 * nearby zones.
 * 
 * @author smelC
 */
public class DungeonZonesCrawler implements Iterable<Zone> {

	protected final Dungeon dungeon;
	protected final Zone start;

	/**
	 * @param dungeon
	 * @param start
	 *            Where to start crawling from.
	 */
	public DungeonZonesCrawler(Dungeon dungeon, Zone start) {
		assert Dungeons.hasRoomOrCorridor(dungeon, start);
		this.dungeon = dungeon;
		this.start = start;
	}

	@Override
	public Iterator<Zone> iterator() {
		class Result implements Iterator<Zone> {

			private final Set<Zone> dones = new HashSet<Zone>();
			private int nextIdxInCurrents = 0;
			private List<Zone> currents;

			private Result() {
				this.currents = new ArrayList<Zone>();
				this.currents.add(start);
			}

			@Override
			public boolean hasNext() {
				final int nbc = currents.size();
				if (nextIdxInCurrents < nbc)
					return true;
				else {
					final List<Zone> nexts = new ArrayList<Zone>();
					/*
					 * Look for neighbors of members of 'currents' that haven't
					 * been used yet
					 */
					for (int i = 0; i < nbc; i++) {
						final Zone done = currents.get(i);
						final List<Zone> neighbors = dungeon.getNeighbors(done);
						final int nbn = neighbors.size();
						for (int j = 0; j < nbn; j++) {
							final Zone neighbor = neighbors.get(j);
							if (!dones.contains(neighbor) && !nexts.contains(neighbor))
								nexts.add(neighbor);
						}
					}
					currents = nexts;
					nextIdxInCurrents = 0;
					return !currents.isEmpty();
				}
			}

			@Override
			public Zone next() {
				final Zone result = next0();
				final boolean added = dones.add(result);
				assert added;
				return result;
			}

			private Zone next0() {
				final int nbc = currents.size();
				if (nextIdxInCurrents < nbc)
					return currents.get(nextIdxInCurrents++);
				else {
					final List<Zone> prev = currents;
					if (hasNext()) {
						/* hasNext computed the successors: */
						assert prev != currents;
						assert nextIdxInCurrents == 0;
						final Zone result = currents.get(nextIdxInCurrents);
						nextIdxInCurrents++;
						return result;
					} else
						throw new NoSuchElementException();

				}
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}

		}

		return new Result();
	}

}
