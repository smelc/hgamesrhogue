package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

import com.hgames.lib.collection.Multimaps;

import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.ListZone;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;

/**
 * API to mutate a {@link Dungeon}. Methods in this file are sorted, keep it
 * that way.
 * 
 * @author smelC
 */
public class DungeonBuilder {

	protected final Dungeon dungeon;

	DungeonBuilder(Dungeon dungeon) {
		this.dungeon = dungeon;
	}

	/**
	 * Adds a connection between {@code z1} and {@code z2} in {@code dungeon},
	 * taking care of reflexivity.
	 * 
	 * @param z1
	 * @param z2
	 */
	public void addConnection(Zone z1, Zone z2) {
		assert z1 != z2;
		if (z1 == z2)
			throw new IllegalStateException("A zone should not be connected to itself");
		assert Dungeons.hasRoomOrCorridor(dungeon, z1);
		assert Dungeons.hasRoomOrCorridor(dungeon, z2);
		Multimaps.addToListMultimapIfAbsent(dungeon.connections, z1, z2);
		Multimaps.addToListMultimapIfAbsent(dungeon.connections, z2, z1);
	}

	/**
	 * @param z
	 */
	public void addDisconnectedRoom(Zone z) {
		assert dungeon.getRooms().contains(z);
		if (dungeon.disconnectedRooms == null)
			dungeon.disconnectedRooms = new ArrayList<Zone>();
		dungeon.disconnectedRooms.add(z);
	}

	/**
	 * @param z
	 */
	public void addWaterIsland(Zone z) {
		assert dungeon.getRooms().contains(z);
		if (dungeon.waterIslands == null)
			dungeon.waterIslands = new ArrayList<Zone>();
		dungeon.waterIslands.add(z);
	}

	/**
	 * @param pool
	 *            The pool to add.
	 */
	public void addWaterPool(ListZone pool) {
		assert !Dungeons.hasZone(dungeon, pool);
		if (dungeon.waterPools == null)
			dungeon.waterPools = new ArrayList<ListZone>();
		else
			assert !dungeon.waterPools.contains(pool);
		dungeon.waterPools.add(pool);
	}

	/**
	 * @param z
	 *            The zone to add
	 * @param boundingBox
	 *            {@code z}'s bounding box, or null.
	 * @param roomOrCorridor
	 *            Whether {@code z} is a room or a corridor.
	 */
	public void addZone(Zone z, /* @Nullable */ Rectangle boundingBox, boolean roomOrCorridor) {
		/* Zone should not intersect with existing zones */
		assert Dungeons.findIntersectingZones(dungeon, z, true, true, true) == null : ("Cannot add zone " + z
				+ ". It overlaps with an existing zone: "
				+ Dungeons.findIntersectingZones(dungeon, z, true, true, true));
		/* Check that bounding box (if any) is correct */
		assert boundingBox == null || boundingBox.contains(z);
		// System.out.println("Adding zone: " + z);
		if (roomOrCorridor)
			dungeon.rooms.add(z);
		else
			dungeon.corridors.add(z);
		if (boundingBox != null) {
			final Rectangle prev = dungeon.boundingBoxes.put(z, boundingBox);
			if (prev != null)
				throw new IllegalStateException(z + " was recorded already");
		}
	}

	/**
	 * Remove all members of {@code z} from {@code dungeon}'s water pools.
	 * 
	 * @param z
	 * @param acc
	 *            Where to record removed cells, or null.
	 * @return Whether something was indeed removed.
	 */
	public boolean removeFromWaterPools(Zone z, /* @Nullable */ Collection<Coord> acc) {
		if (dungeon.waterPools == null)
			return false;
		boolean result = false;
		final int sz = dungeon.waterPools.size();
		nextCoord: for (Coord c : z) {
			for (int i = 0; i < sz; i++) {
				final boolean rmed = dungeon.waterPools.get(i).getState().remove(c);
				if (rmed) {
					result = true;
					if (acc != null)
						acc.add(c);
					continue nextCoord;
				}
			}
		}
		return result;
	}

	/**
	 * @param z
	 *            The zone to remove.
	 */
	public void removeZone(Zone z) {
		boolean done = dungeon.rooms.remove(z);
		if (!done)
			done = dungeon.corridors.remove(z);
		if (!done)
			throw new IllegalStateException("Cannot remove zone (not found): " + z);
		dungeon.boundingBoxes.remove(z);
		dungeon.connections.remove(z);
		for (List<Zone> destinations : dungeon.connections.values())
			destinations.remove(z);
	}

	/**
	 * Prefer this method over direct mutations, it eases debugging.
	 * 
	 * @param x
	 * @param y
	 * @param upOrDown
	 */
	public void setStair(int x, int y, boolean upOrDown) {
		final DungeonSymbol sym = upOrDown ? DungeonSymbol.STAIR_UP : DungeonSymbol.STAIR_DOWN;
		setSymbol(x, y, sym);
		final Coord c = Coord.get(x, y);
		if (upOrDown)
			dungeon.upwardStair = c;
		else
			dungeon.downwardStair = c;
	}

	/**
	 * Prefer this method over direct mutations, it eases debugging.
	 * 
	 * @param c
	 *            The cell to modify.
	 * @param sym
	 *            The symbol to set at {@code sym}.
	 */
	public void setSymbol(Coord c, DungeonSymbol sym) {
		setSymbol(c.x, c.y, sym);
	}

	/**
	 * Prefer this method over direct mutations, it eases debugging.
	 *
	 * @param x
	 * @param y
	 * @param sym
	 */
	public void setSymbol(int x, int y, DungeonSymbol sym) {
		dungeon.map[x][y] = sym;
	}

	/**
	 * Sets {@code sym} everywhere in {@code it}.
	 * 
	 * @param it
	 * @param sym
	 */
	public void setSymbols(Iterator<Coord> it, DungeonSymbol sym) {
		while (it.hasNext()) {
			final Coord c = it.next();
			dungeon.map[c.x][c.y] = sym;
		}
	}

	/**
	 * Sets {@code sym} for cells in {@code it} whose symbol is not in {@cod except}
	 * 
	 * @param it
	 * @param sym
	 * @param except
	 */
	public void setSymbolsExcept(Iterator<Coord> it, DungeonSymbol sym, EnumSet<DungeonSymbol> except) {
		while (it.hasNext()) {
			final Coord c = it.next();
			if (!except.contains(dungeon.getSymbol(c)))
				dungeon.map[c.x][c.y] = sym;
		}
	}

	/**
	 * Sets {@code sym} everywhere in {@code dungeon}.
	 * 
	 * @param sym
	 */
	public void setAllSymbols(DungeonSymbol sym) {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				dungeon.map[x][y] = sym;
			}
		}
	}

}
