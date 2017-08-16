package com.hgames.rhogue.generation.map;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.hgames.lib.Exceptions;
import com.hgames.lib.Ints;
import com.hgames.lib.Pair;
import com.hgames.rhogue.grid.GridIterators;
import com.hgames.rhogue.rng.ProbabilityTable;
import com.hgames.rhogue.zone.CachingZone;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * @author smelC
 */
public class DungeonGenerator {

	protected final RNG rng;
	protected final int width;
	protected final int height;
	/** An upper bound of the number of corridors to and from a room */
	protected int connectivity;
	protected /* @Nullable */ IDungeonDrawer drawer;

	protected final ProbabilityTable<IRoomGenerator> roomGenerators;

	protected int minRoomWidth = 2;
	protected int maxRoomWidth;
	protected int minRoomHeight = 2;
	protected int maxRoomHeight;

	/** An int in [0, 100], which is used when a door can be punched */
	protected int doorProbability = 50;

	private static final Zone[] ZONE_PAIR_BUF = new Zone[2];

	/**
	 * A fresh generator.
	 * 
	 * @param rng
	 *            The seed to use.
	 * @param width
	 *            The desired map's width.
	 * @param height
	 *            The desired map's height.
	 */
	public DungeonGenerator(RNG rng, int width, int height) {
		if (width < 0)
			throw new IllegalStateException("Invalid width for dungeon generator: " + width);
		if (height < 0)
			throw new IllegalStateException("Invalid height for dungeon generator: " + height);
		this.rng = rng;
		this.width = width;
		this.height = height;
		this.roomGenerators = ProbabilityTable.create();
		this.maxRoomWidth = width / 5;
		this.maxRoomHeight = height / 5;
	}

	/**
	 * @param drawer
	 *            The drawer to use, or null to turn it OFF.
	 * @return {@code this}
	 */
	public DungeonGenerator setDrawer(/* @Nullable */ IDungeonDrawer drawer) {
		this.drawer = drawer;
		return this;
	}

	/**
	 * Sets the upper bound of the number of connections of a room.
	 * 
	 * @param c
	 * @return {@code this}
	 */
	public DungeonGenerator setConnectivity(int c) {
		if (c <= 0)
			throw new IllegalStateException("Connectivy must be greater than zero. Received: " + c);
		this.connectivity = c;
		return this;
	}

	/**
	 * @param minWidth
	 *            The minimum width of rooms. The default is 2.
	 * @param maxWidth
	 *            The maximum width of rooms. The default is {@link #width} / 5.
	 * @param minHeight
	 *            The minimum width of rooms. The default is 2.
	 * @param maxHeight
	 *            The maximum height of rooms. The default is {@link #height} /
	 *            5.
	 * @return {@code this}
	 */
	public DungeonGenerator setRoomsBounds(int minWidth, int maxWidth, int minHeight, int maxHeight) {
		this.minRoomWidth = minWidth;
		this.maxRoomWidth = maxWidth;
		this.minRoomHeight = minHeight;
		this.maxRoomHeight = maxHeight;
		return this;
	}

	/**
	 * @param proba
	 *            An int in [0, 100]
	 * @return {@code this}
	 * @throws IllegalStateException
	 *             If {@code proba} isn't in [0, 100].
	 */
	public DungeonGenerator setDoorProbability(int proba) {
		if (!Ints.inInterval(0, proba, 100))
			throw new IllegalStateException("Excepted a value in [0, 100]. Received: " + proba);
		this.doorProbability = proba;
		return this;
	}

	/**
	 * Record {@code roomGenerator} as a generator used by this dungeon
	 * generator.
	 * 
	 * @param roomGenerator
	 *            The generator to record.
	 * @param probability
	 *            The probability of using {@code roomGenerator} among all room
	 *            generators installed.
	 * @return {@code this}.
	 */
	public DungeonGenerator installRoomGenerator(IRoomGenerator roomGenerator, int probability) {
		this.roomGenerators.add(roomGenerator, probability);
		return this;
	}

	/**
	 * @return A fresh dungeon.
	 */
	public Dungeon generate() {
		final DungeonSymbol[][] map = new DungeonSymbol[width][height];
		final Dungeon dungeon = new Dungeon(map);
		DungeonBuilder.setAllSymbols(dungeon, DungeonSymbol.WALL);
		if (width == 0 || height == 0)
			// Nothing to do
			return dungeon;
		final GenerationData gdata = new GenerationData(dungeon);
		generateRooms(gdata);
		/* TODO use RectangleRoomFinder to generate rooms in a second pass */
		generateDoorsInAlmostAdjacentRooms(gdata);
		generateCorridors(gdata);
		return dungeon;
	}

	protected void generateRooms(GenerationData gdata) {
		while (true) {
			final boolean done = generateRoom(gdata);
			if (!done)
				/* Cannot place any more room */
				break;
		}
	}

	/**
	 * Generate doors on cells that are {@link DungeonSymbol#WALL} and which are
	 * between rooms.
	 * 
	 * @param gdata
	 */
	protected void generateDoorsInAlmostAdjacentRooms(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (!isDoorCandidate(dungeon, x, y, true) && !isDoorCandidate(dungeon, x, y, false))
					continue;
				final Zone z0 = ZONE_PAIR_BUF[0];
				assert z0 != null;
				final Zone z1 = ZONE_PAIR_BUF[1];
				assert z1 != null;
				assert z0 != z1;
				dungeon.map[x][y] = DungeonSymbol.FLOOR;
				DungeonBuilder.addConnection(dungeon, z0, z1);
			}
		}
	}

	protected void generateCorridors(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		/* A Zone, to the other zones; ordered by the distance of the centers */
		final Map<Zone, List<Pair<Double, Zone>>> zoneToOtherZones = new LinkedHashMap<Zone, List<Pair<Double, Zone>>>(
				dungeon.rooms.size());
		final int nbr = dungeon.rooms.size();
		for (int i = 0; i < nbr; i++) {
			final Zone z = dungeon.rooms.get(i);
			final Coord zc = z.getCenter();
			assert !zoneToOtherZones.keySet().contains(z);
			final List<Pair<Double, Zone>> otherZones = new ArrayList<Pair<Double, Zone>>(
					Math.max(0, nbr - 1));
			for (int j = 0; j < nbr; j++) {
				final Zone other = dungeon.rooms.get(j);
				if (other == z)
					continue;
				final Coord oc = other.getCenter();
				final double dist = zc.distance(oc);
				otherZones.add(Pair.of(dist, other));
			}
			assert otherZones.size() == nbr - 1;
			zoneToOtherZones.put(z, otherZones);
		}
		assert zoneToOtherZones.size() == nbr;
	}

	protected int getMaxRoomSideSize(boolean widthOrHeight, boolean spiceItUp) {
		final int result = widthOrHeight ? rng.between(minRoomWidth, maxRoomWidth)
				: rng.between(minRoomHeight, maxRoomHeight);
		return result * (spiceItUp ? 2 : 1);
	}

	protected int mapSize() {
		return width * height;
	}

	protected void draw(Dungeon dungeon) {
		if (drawer != null) {
			drawer.draw(dungeon.getMap());
		}
	}

	private boolean generateRoom(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		int frustration = 0;
		outer: while (frustration < 32) {
			frustration++;
			/* +1 to account for the surrounding wall */
			final int maxWidth = getMaxRoomSideSize(true, rng.nextInt(10) == 0) + 1;
			final int maxHeight = getMaxRoomSideSize(false, rng.nextInt(10) == 0) + 1;
			/* Top-left coordinate */
			final Iterator<Coord> tlPlacer = new GridIterators.RectangleRandomStartAndDirection(width, height,
					rng.nextInt(width), rng.nextInt(height), rng.getRandomElement(Direction.CARDINALS));
			while (true) {
				if (!tlPlacer.hasNext())
					continue outer;
				final Coord tlCandidate = tlPlacer.next();
				if (Dungeons.isOnEdge(dungeon, tlCandidate))
					continue;
				/* To avoid the room to be on the edge */
				final int mw = Math.min(maxWidth, width - (tlCandidate.x + 2));
				final int mh = Math.min(maxHeight, height - (tlCandidate.y + 2));
				/* Bottom-right cell */
				final Coord brCandidate = Coord.get(tlCandidate.x + mw, tlCandidate.y + mh);
				final Coord blCandidate = Coord.get(tlCandidate.x, brCandidate.y);
				/*
				 * .extend() to avoid generating adjacent rooms. This is a smart
				 * trick (as opposed to extending the rooms already created).
				 */
				if (!isOnly(dungeon,
						Rectangle.Utils.cells(new Rectangle.Impl(blCandidate, maxWidth, maxHeight).extend()),
						DungeonSymbol.WALL))
					continue;
				assert dungeon.isValid(brCandidate);
				assert !Dungeons.isOnEdge(dungeon, brCandidate);
				final Zone zone = generateRoomAt(tlCandidate, mw, mh);
				if (zone != null) {
					assert !DungeonBuilder.anyOnEdge(dungeon, zone.iterator());
					/* Punch the zone */
					DungeonBuilder.setSymbols(dungeon, zone.iterator(), DungeonSymbol.FLOOR);
					final Zone recorded = new CachingZone(zone);
					dungeon.rooms.add(recorded);
					dungeon.boundingBoxes.put(recorded, new Rectangle.Impl(blCandidate, maxWidth, maxHeight));
					gdata.extensions.put(recorded, new CachingZone(zone.extend()));
					draw(dungeon);
					return true;
				}
			}
			/* Unreachable */
			// assert false;
		}
		return false;
	}

	private /* @Nullable */ Zone generateRoomAt(Coord bottomLeft, int maxWidth, int maxHeight) {
		final IRoomGenerator rg = roomGenerators.get(rng);
		if (rg == null)
			return null;
		final Zone zeroZeroZone = rg.generate(maxWidth, maxHeight);
		if (zeroZeroZone == null)
			return null;
		final Zone zone = zeroZeroZone.translate(bottomLeft);
		return zone;
	}

	/**
	 * @param dungeon
	 * @param x
	 * @param y
	 * @param southNorthOrEastWest
	 * @return Whether {@code (x,y)} is a valid door candidate, i.e. it has a
	 *         valid walkable cell (according to
	 *         {@link #isDoorNeighborCandidate(DungeonSymbol)}) to its left and
	 *         right (if {@code southNorthOrEastWest} is set, otherwise north
	 *         and south are checked).
	 * 
	 *         <p>
	 *         If it returns {@code true}, {@link #ZONE_PAIR_BUF} is filled with
	 *         the zones that could be connected by the door.
	 *         </p>
	 */
	private boolean isDoorCandidate(Dungeon dungeon, int x, int y, boolean southNorthOrEastWest) {
		final DungeonSymbol sym = dungeon.getSymbol(x, y);
		if (sym == null)
			return false;
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case FLOOR:
		case GRASS:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
			return false;
		case WALL:
			break;
		}
		final int x1 = x + (southNorthOrEastWest ? Direction.DOWN.deltaX : Direction.LEFT.deltaX);
		final int y1 = y + (southNorthOrEastWest ? Direction.DOWN.deltaY : Direction.LEFT.deltaY);
		if (!isDoorNeighborCandidate(dungeon.getSymbol(x1, y1)))
			return false;
		final int x2 = x + (southNorthOrEastWest ? Direction.UP.deltaX : Direction.RIGHT.deltaX);
		final int y2 = y + (southNorthOrEastWest ? Direction.UP.deltaY : Direction.RIGHT.deltaY);
		if (!isDoorNeighborCandidate(dungeon.getSymbol(x2, y2)))
			return false;
		final Zone z1 = Dungeons.findZoneContaining(dungeon, x1, y1);
		if (z1 == null) {
			assert false;
			throw new IllegalStateException("Cannot find zone containing " + x1 + "," + y1);
		}
		final Zone z2 = Dungeons.findZoneContaining(dungeon, x2, y2);
		if (z2 == null) {
			assert false;
			throw new IllegalStateException("Cannot find zone containing " + x1 + "," + y1);
		}
		ZONE_PAIR_BUF[0] = z1;
		ZONE_PAIR_BUF[1] = z2;
		return true;
	}

	/**
	 * @return Whether zone is valid in dungeon and only contains {@code sym}
	 */
	private boolean isOnly(Dungeon dungeon, Iterator<Coord> zone, DungeonSymbol sym) {
		while (zone.hasNext()) {
			final Coord c = zone.next();
			final DungeonSymbol dsym = dungeon.getSymbol(c.x, c.y);
			if (dsym == null)
				/* Out of bounds */
				return false;
			if (dsym != sym)
				/* Not the expected symbol */
				return false;
		}
		return true;
	}

	private static boolean isDoorNeighborCandidate(/* @Nullable */ DungeonSymbol sym) {
		if (sym == null)
			return false;
		switch (sym) {
		case CHASM:
		case DEEP_WATER:
		case DOOR:
		case HIGH_GRASS:
		case SHALLOW_WATER:
		case STAIR_DOWN:
		case STAIR_UP:
		case WALL:
			return false;
		case GRASS:
		case FLOOR:
			return true;
		}
		throw Exceptions.newUnmatchedISE(sym);
	}

	/**
	 * Data carried on during generation of a single dungeon.
	 * 
	 * @author smelC
	 */
	private static class GenerationData {

		protected final Dungeon dungeon;
		/**
		 * A map from {@link Dungeon#rooms} to the same zones {@link Zone#extend
		 * extended once}.
		 */
		@Deprecated
		protected final Map<Zone, CachingZone> extensions = new HashMap<Zone, CachingZone>();

		protected GenerationData(Dungeon dungeon) {
			this.dungeon = dungeon;
		}

	}
}
