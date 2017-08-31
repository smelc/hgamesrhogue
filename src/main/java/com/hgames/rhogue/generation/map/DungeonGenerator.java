package com.hgames.rhogue.generation.map;

import static com.hgames.lib.Strings.plural;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import com.hgames.lib.Exceptions;
import com.hgames.lib.Ints;
import com.hgames.lib.Pair;
import com.hgames.lib.Stopwatch;
import com.hgames.lib.choice.DoublePriorityCell;
import com.hgames.lib.collection.Multimaps;
import com.hgames.lib.collection.multiset.EnumMultiset;
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.lifetime.Lifetime;
import com.hgames.rhogue.grid.GridIterators;
import com.hgames.rhogue.rng.ProbabilityTable;
import com.hgames.rhogue.tests.generation.map.ConsoleDungeonDrawer;
import com.hgames.rhogue.zone.CachingZone;
import com.hgames.rhogue.zone.SingleCellZone;
import com.hgames.rhogue.zone.Zones;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.Zone;
import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * The dungeon generator I use in <a href="http://schplaf.org/hgames">Dungeon
 * Mercenary</a>. It builts a structured dungeon. This means you have accesses
 * to objects describing rooms, corridors, zones, etc.
 * 
 * <p>
 * This class' API can be divided into two:
 * 
 * <ul>
 * <li>The "configure" part of the API which consists of all {@code set*}
 * methods.</li>
 * <li>The "generate" part of the API which is {@link #generate()}.</li>
 * </ul>
 * </p>
 * 
 * <p>
 * You can call {@link #generate()} multiple times on a single instance to
 * generate different dungeons. Beware, however, that instances of
 * {@link Lifetime} associated to {@link IRoomGenerator generators} may change
 * their inner state overtime; which will affect {@link #generate() results}.
 * </p>
 * 
 * <p>
 * If you wanna see how this class proceeds in console, you should give an
 * instance of {@link ConsoleDungeonDrawer} to
 * {@link DungeonGenerator#setDrawer(IDungeonDrawer)}. If you're in a UI context
 * with a main loop, you should give an instance of {@link DungeonProgression}
 * to {@link DungeonGenerator#setDrawer(IDungeonDrawer)} and draw the successive
 * states of the dungeon in successive calls of the main loop (see the
 * <a href="hdungeongen">https://github.com/smelc/hdungeongen</a> library for an
 * example on how to do that with libgdx).
 * </p>
 * 
 * <p>
 * See {@link DungeonGenerators} for examples of usage.
 * </p>
 * 
 * @author smelC
 */
public class DungeonGenerator {

	protected final RNG rng;
	protected final int width;
	protected final int height;
	/**
	 * An upper bound of the number of corridors to and from a room (ignores
	 * doors punched because of rooms being adjacent).
	 */
	protected int connectivity = 3;
	protected /* @Nullable */ IDungeonDrawer drawer;
	protected /* @Nullable */ ILogger logger;

	/** All elements of this table are in {@link #rgLifetimes} too */
	protected final ProbabilityTable<IRoomGenerator> roomGenerators;
	/** All elements of this map are in {@link #roomGenerators} too */
	protected final Map<IRoomGenerator, Lifetime> rgLifetimes;

	protected int minRoomWidth = 2;
	protected int maxRoomWidth;
	protected int minRoomHeight = 2;
	protected int maxRoomHeight;

	/** An int in [0, 100], which is used when a door can be punched */
	protected int doorProbability = 50;

	/**
	 * Whether rooms whose width and height of size 1 are allowed. They look
	 * like corridors, so they are ruled out by default; but it can be fun to
	 * have them.
	 */
	protected boolean allowWidthOrHeightOneRooms = false;

	/**
	 * The percentage of the map that will be turned into deep water
	 * (approximately). In [0, 100].
	 */
	protected int waterPercentage = 15;

	/**
	 * The number of unconnected rooms (w.r.t. to the stairs) to aim for. Useful
	 * for secret rooms that require carving.
	 */
	protected int disconnectedRoomsObjective = 0;

	/** Where to put the upward stair (approximately) */
	protected /* @Nullable */ Coord upStairObjective;
	/** Where to put the downward stair (approximately) */
	protected /* @Nullable */ Coord downStairObjective;

	private static final Zone[] ZONE_PAIR_BUF = new Zone[2];
	private static final List<Coord> COORD_LIST_BUF = new ArrayList<Coord>(4);
	private static final DoublePriorityCell<Coord> DP_CELL = DoublePriorityCell.createEmpty();

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
		this.rgLifetimes = new HashMap<IRoomGenerator, Lifetime>();
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
	 * @param logger
	 *            The logger to use, or null to turn logging OFF.
	 * @return {@code this}
	 */
	public DungeonGenerator setLogger(/* @Nullable */ ILogger logger) {
		this.logger = logger;
		return this;
	}

	/**
	 * @param value
	 *            Whether to allow rooms of width or height one (which allow
	 *            dungeons like this: <a href=
	 *            "https://twitter.com/hgamesdev/status/899609612554559489">
	 *            image </a>). False by default.
	 * @return {@code this}
	 */
	public DungeonGenerator setAllowWidthOrHeightOneRooms(boolean value) {
		this.allowWidthOrHeightOneRooms = value;
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
	 * @param minWidth
	 *            The minimum width of rooms. The default is 2. Give anything
	 *            negative to keep the existing value (useful to only change a
	 *            subset of the sizes).
	 * @param maxWidth
	 *            The maximum width of rooms (inclusive). The default is
	 *            {@link #width} / 5. Give anything negative to keep the
	 *            existing value (useful to only change a subset of the sizes).
	 * @param minHeight
	 *            The minimum width of rooms. The default is 2. Give anything
	 *            negative to keep the existing value (useful to only change a
	 *            subset of the sizes).
	 * @param maxHeight
	 *            The maximum height of rooms (inclusive). The default is
	 *            {@link #height} / 5. Give anything negative to keep the
	 *            existing value (useful to only change a subset of the sizes).
	 * @return {@code this}
	 */
	public DungeonGenerator setRoomsBounds(int minWidth, int maxWidth, int minHeight, int maxHeight) {
		if (0 <= minWidth)
			this.minRoomWidth = minWidth;
		if (0 <= maxWidth)
			this.maxRoomWidth = maxWidth;
		if (0 <= minHeight)
			this.minRoomHeight = minHeight;
		if (0 <= maxHeight)
			this.maxRoomHeight = maxHeight;
		return this;
	}

	/**
	 * Sets objectives to place the stairs. By default there are no objectives
	 * which means a first stair is placed randomly and the second stair is
	 * placed far away from the first one.
	 * 
	 * @param upStair
	 *            A coord or null.
	 * @param downStair
	 *            A coord or null.
	 * @return {@code this}.
	 */
	public DungeonGenerator setStairsObjectives(/* @Nullable */Coord upStair,
			/* @Nullable */Coord downStair) {
		this.upStairObjective = upStair == null ? null : clamp(upStair);
		this.downStairObjective = downStair == null ? null : clamp(downStair);
		return this;
	}

	/**
	 * @param objective
	 *            The objective. Must be >= 0.
	 * @return {@code this}
	 * @throws IllegalStateException
	 *             If {@code objective < 0}.
	 */
	public DungeonGenerator setDisconnectedRoomsObjective(int objective) {
		if (objective < 0)
			throw new IllegalStateException(
					"Disconnected rooms objective must be >= 0. Received: " + objective);
		this.disconnectedRoomsObjective = objective;
		return this;
	}

	/**
	 * @param percent
	 *            An int in [0, 100]
	 * @return {@code this}
	 * @throws IllegalStateException
	 *             If {@code percent} isn't in [0, 100].
	 */
	public DungeonGenerator setWaterPercentage(int percent) {
		if (!Ints.inInterval(0, percent, 100))
			throw new IllegalStateException("Excepted a value in [0, 100]. Received: " + percent);
		this.waterPercentage = percent;
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
	 * @param lifetime
	 * @return {@code this}.
	 */
	public DungeonGenerator installRoomGenerator(IRoomGenerator roomGenerator, int probability,
			Lifetime lifetime) {
		this.roomGenerators.add(roomGenerator, probability);
		this.rgLifetimes.put(roomGenerator, lifetime);
		return this;
	}

	/** @return A fresh dungeon or null if it could not be generated. */
	public Dungeon generate() {
		/*
		 * /!\ Don't forget to disable assertions when checking performances.
		 * Assertions are by far the longest thing! /!\.
		 */
		final Stopwatch watch = (logger != null && logger.isInfoEnabled()) ? new Stopwatch() : null;
		final DungeonSymbol[][] map = new DungeonSymbol[width][height];
		final Dungeon dungeon = new Dungeon(map);
		DungeonBuilder.setAllSymbols(dungeon, DungeonSymbol.WALL);
		if (width == 0 || height == 0)
			// Nothing to do
			return dungeon;
		final GenerationData gdata = new GenerationData(dungeon, watch);
		gdata.startStage(Stage.ROOMS);
		generateRooms(gdata);
		gdata.startStage(Stage.PASSAGES_IN_ALMOST_ADJACENT_ROOMS);
		/* XXX use RectangleRoomFinder to generate rooms in a second pass ? */
		generatePassagesInAlmostAdjacentRooms(gdata);
		draw(gdata.dungeon);
		gdata.startStage(Stage.CORRIDORS);
		generateCorridors(gdata, dungeon.rooms, dungeon.rooms, true);
		/* Must be called before 'generateWater' */
		gdata.startStage(Stage.STAIRS);
		final boolean good = generateStairs(gdata);
		if (!good)
			return null;
		gdata.startStage(Stage.ENSURE_DENSITY);
		ensureDensity(gdata);
		gdata.startStage(Stage.WATER);
		generateWater(gdata);
		gdata.startStage(null);
		gdata.logTimings(logger);
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
	 * Generate doors/floors on cells that are {@link DungeonSymbol#WALL} and
	 * which are between rooms.
	 * 
	 * @param gdata
	 */
	protected void generatePassagesInAlmostAdjacentRooms(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		/* Keys in this map are ordered according to GenerationData.zOrder */
		/* The Lists do not contain doublons */
		final Map<Pair<Zone, Zone>, List<Coord>> connectedsToCandidates = new HashMap<Pair<Zone, Zone>, List<Coord>>(
				16);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (!isDoorCandidate(gdata, x, y, true) && !isDoorCandidate(gdata, x, y, false))
					continue;
				final Zone z0 = ZONE_PAIR_BUF[0];
				assert z0 != null;
				final Zone z1 = ZONE_PAIR_BUF[1];
				assert z1 != null;
				assert z0 != z1;
				Multimaps.addToListMultimap(connectedsToCandidates, orderedPair(gdata, z0, z1),
						Coord.get(x, y));
			}
		}
		/*
		 * Look for the door closest to the mean of the zones' centers. That's
		 * the ideal door.
		 */
		final DoublePriorityCell<Coord> cell = DoublePriorityCell.createEmpty();
		for (Map.Entry<Pair<Zone, Zone>, List<Coord>> entry : connectedsToCandidates.entrySet()) {
			cell.clear();
			final Pair<Zone, Zone> connecteds = entry.getKey();
			final Zone z0 = connecteds.getFst();
			final Zone z1 = connecteds.getSnd();
			assert !DungeonBuilder.areConnected(dungeon, z0, z1, 1);
			final Coord ideal;
			{
				final Coord center0 = z0.getCenter();
				final Coord center1 = z1.getCenter();
				ideal = Coord.get((center0.x + center1.x) / 2, (center0.y + center1.y / 2));
			}
			final List<Coord> candidates = entry.getValue();
			final int nbc = candidates.size();
			assert 0 < nbc;
			for (int i = 0; i < nbc; i++) {
				final Coord candidate = candidates.get(i);
				cell.union(candidate, candidate.distance(ideal));
			}
			final Coord door = cell.get();
			assert door != null;
			final DungeonSymbol sym = rng.next(101) <= doorProbability ? DungeonSymbol.DOOR
					: DungeonSymbol.FLOOR;
			final Zone zdoor = new SingleCellZone(door);
			addZone(gdata, zdoor, null, false);
			DungeonBuilder.addConnection(dungeon, z0, zdoor);
			DungeonBuilder.addConnection(dungeon, z1, zdoor);
			DungeonBuilder.setSymbol(dungeon, door.x, door.y, sym);
			draw(dungeon);
		}
	}

	/**
	 * @param rooms
	 *            The rooms to build corridors from.
	 * @param dests
	 *            The possible destinations (should be rooms too).
	 * @return The number of corridors built
	 */
	protected int generateCorridors(GenerationData gdata, List<Zone> rooms, List<Zone> dests,
			boolean onlyPerfectCarving) {
		final Dungeon dungeon = gdata.dungeon;
		final int nbr = rooms.size();
		/* A Zone, to the other zones; ordered by the distance of the centers */
		final Map<Zone, List<Pair<Double, Zone>>> zoneToOtherZones = new LinkedHashMap<Zone, List<Pair<Double, Zone>>>(
				nbr);
		final int nbd = dests.size();
		final double maxDist = (width + height) / 6;
		for (int i = 0; i < nbr; i++) {
			final Zone z = rooms.get(i);
			assert DungeonBuilder.isRoom(dungeon, z);
			final Coord zc = z.getCenter();
			assert !zoneToOtherZones.keySet().contains(z);
			final List<Pair<Double, Zone>> otherZones = new ArrayList<Pair<Double, Zone>>(
					Math.max(0, nbr - 1));
			for (int j = 0; j < nbd; j++) {
				final Zone other = dests.get(j);
				assert DungeonBuilder.isRoom(dungeon, other);
				if (other == z)
					continue;
				final Coord oc = other.getCenter();
				final double dist = zc.distance(oc);
				if (maxDist < dist)
					/* Too far away */
					continue;
				otherZones.add(Pair.of(dist, other));
			}
			if (!otherZones.isEmpty())
				zoneToOtherZones.put(z, otherZones);
		}
		final CorridorBuilder cc = new CorridorBuilder(gdata.dungeon, true, onlyPerfectCarving);
		final Coord[] startEndBuffer = new Coord[2];
		final Coord[] connection = new Coord[2];
		int result = 0;
		for (Zone z : zoneToOtherZones.keySet()) {
			final List<Pair<Double, Zone>> candidateDests = zoneToOtherZones.get(z);
			if (candidateDests == null)
				continue;
			final int nbcd = candidateDests.size();
			Collections.sort(candidateDests, ORDERER);
			for (int j = 0; j < connectivity && j < nbcd; j++) {
				final Zone dest = candidateDests.get(j).getSnd();
				if (DungeonBuilder.areConnected(dungeon, z, dest, 3))
					continue;
				final boolean found = getZonesConnectionEndpoints(gdata, z, dest, connection);
				if (!found)
					continue;
				final Coord zEndpoint = connection[0];
				final Coord destEndpoint = connection[1];
				final Zone built = cc.build(rng, zEndpoint, destEndpoint, startEndBuffer);
				if (built != null) {
					// (NO_CORRIDOR_BBOX). This doesn't trigger if 'built'
					// is a Rectangle, but it may if it a ZoneUnion.
					assert !built.contains(zEndpoint);
					assert !built.contains(destEndpoint);
					final Zone recorded = addZone(gdata, built, null, false);
					DungeonBuilder.addConnection(dungeon, z, recorded);
					DungeonBuilder.addConnection(dungeon, dest, recorded);
					// Punch corridor
					DungeonBuilder.setSymbols(dungeon, built.iterator(), DungeonSymbol.FLOOR);
					draw(dungeon);
					result++;
				}
			}
		}
		return result;
	}

	/** @return Whether the stairs could be placed */
	private boolean generateStairs(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		final Coord up = generateStair(gdata, true);
		if (up == null) {
			warnLog("Cannot place upward stair");
			return false;
		} else if (logger != null && logger.isInfoEnabled())
			infoLog("Placed upward stair at " + up);
		assert dungeon.getSymbol(dungeon.upwardStair) == DungeonSymbol.STAIR_UP;
		draw(dungeon);
		final Coord down = generateStair(gdata, false);
		if (down == null) {
			warnLog("Cannot place downward stair");
			return false;
		} else if (logger != null && logger.isInfoEnabled())
			infoLog("Placed downward stair at " + down);
		assert dungeon.getSymbol(dungeon.downwardStair) == DungeonSymbol.STAIR_DOWN;
		draw(dungeon);
		return true;
	}

	/** @return Where it got generated, if it did (otherwise null) */
	protected /* @Nullable */ Coord generateStair(GenerationData gdata, boolean upOrDown) {
		final Dungeon dungeon = gdata.dungeon;
		/* @Nullable */ Coord objective = upOrDown ? upStairObjective : downStairObjective;
		assert objective == null || dungeon.isValid(objective);
		final /* @Nullable */ Coord other = upOrDown ? dungeon.downwardStair : dungeon.upwardStair;
		if (objective == null) {
			if (other == null) {
				objective = getRandomCell(null);
				if (logger != null && logger.isInfoEnabled())
					infoLog((upOrDown ? "upward" : "downward") + " stair objective chosen randomly");
			} else {
				final int random = rng.nextInt(4);
				switch (random) {
				case 0:
				case 1:
				case 2:
					final Direction otherDir = getDirection(other);
					if (logger != null && logger.isInfoEnabled())
						infoLog("other stair is in direction: " + otherDir);
					final int disturb = rng.nextInt(2);
					Direction chosenDir = otherDir.opposite();
					if (1 == disturb)
						chosenDir = chosenDir.clockwise();
					else if (2 == disturb)
						chosenDir = chosenDir.counterClockwise();
					objective = getRandomCell(chosenDir);
					if (logger != null && logger.isInfoEnabled())
						infoLog((upOrDown ? "upward" : "downward") + " stair objective chosen in direction: "
								+ chosenDir);
					break;
				case 3:
					if (logger != null && logger.isInfoEnabled())
						infoLog((upOrDown ? "upward" : "downward") + " stair objective chosen randomly");
					objective = getRandomCell(null);
					break;
				default:
					throw new IllegalStateException(
							"Rng is incorrect. Received " + random + " when calling nextInt(4)");
				}
			}
		}
		assert objective != null;
		if (logger != null && logger.isInfoEnabled())
			infoLog("Stair objective: " + objective);

		final int rSize = ((width + height) / 6) + 1;
		final Iterator<Coord> it = new GridIterators.GrowingRectangle(objective, rSize);
		final PriorityQueue<Coord> queue = new PriorityQueue<Coord>(rSize * 4,
				newDistanceComparatorFrom(objective));
		while (it.hasNext()) {
			final Coord next = it.next();
			if (isStairCandidate(dungeon, next))
				queue.add(next);
		}
		if (queue.isEmpty())
			return null;
		if (logger != null && logger.isInfoEnabled())
			infoLog(queue.size() + " stair candidate" + (queue.size() == 1 ? "" : "s"));
		while (!queue.isEmpty()) {
			final Coord candidate = queue.remove();
			if (other == null
					|| (!other.equals(candidate) && gdata.pathExists(other, candidate, false, false))) {
				if (punchStair(gdata, candidate, upOrDown))
					return candidate;
			}
		}
		return null;
	}

	/** @return Whether punching was done */
	protected boolean punchStair(GenerationData gdata, Coord c, boolean upOrDown) {
		final Dungeon dungeon = gdata.dungeon;
		DungeonBuilder.setStair(dungeon, c.x, c.y, upOrDown);
		return true;
	}

	/**
	 * Make sure that at least 1/5th of the map is accessible. For that, find
	 * disconnected rooms. For every disconnected component whose size is >
	 * {@link #getWallificationBound()} of the map, try very hard to connect it
	 * to the stairs. At the end check if 1/6th of the map is accessible.
	 * 
	 * <p>
	 * This method must be called before generating impassable terrain (deep
	 * water, lava, etc.)
	 * </p>
	 * 
	 * @return Whether the dungeon is valid.
	 */
	protected boolean ensureDensity(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		if (!DungeonBuilder.hasStairs(dungeon))
			throw new IllegalStateException("ensureDensity method requires stairs to be set");
		final List<Zone> disconnectedZones = gdata.zonesDisconnectedFrom(true, true, dungeon.upwardStair,
				dungeon.downwardStair);
		final List<List<Zone>> disconnectedComponents = DungeonBuilder.connectedComponents(dungeon,
				disconnectedZones);
		final int nbdc = disconnectedComponents.size();
		int reachable = Zones.size(dungeon.rooms);
		reachable += Zones.size(dungeon.corridors);
		final int msz = mapSize();
		if (0 < nbdc) {
			infoLog("Found " + nbdc + " disconnected component" + plural(nbdc));
			final List<Zone> connectedRooms = new ArrayList<Zone>(dungeon.rooms);
			for (int i = 0; i < nbdc; i++)
				connectedRooms.removeAll(disconnectedComponents.get(i));
			for (int i = 0; i < nbdc; i++) {
				/* Contains both rooms and corridors */
				final List<Zone> disconnectedComponent = disconnectedComponents.get(i);
				final int sz = Zones.size(disconnectedComponent);
				/*
				 * /!\ This call mutes 'connectedRooms' and trashes
				 * 'disconnectedComponent' /!\
				 */
				final int extension = treatDisconnectedComponent(gdata, connectedRooms,
						disconnectedComponent);
				if (0 < extension) {
					reachable += extension;
					infoLog("Connected component (consisting of " + nbdc + " zone" + plural(sz) + ") of size "
							+ extension);
				}
			}
		}
		return (msz / 6) < reachable;
	}

	/**
	 * @param connectedRooms
	 *            Rooms connected to the stair (which are possible corridors
	 *            destinations). Can be extended by this call.
	 * @param component
	 *            The component. It should not be used anymore after this call.
	 * @return The number of cells that got connected to the stairs.
	 */
	private int treatDisconnectedComponent(GenerationData gdata, List<Zone> connectedRooms,
			List<Zone> component) {
		final Dungeon dungeon = gdata.dungeon;
		final int sz = Zones.size(component);
		final int csz = component.size();
		final int bound = getWallificationBound();
		if (sz < bound) {
			/* Component is small, replace it with walls (hereby removing it) */
			if (csz == 1) {
				/*
				 * Component is a single room. Can it be used to honor
				 * #disconnectedRoomsObjective ?
				 */
				if (disconnectedRoomsObjective < dungeon.getDisconnectedRooms().size()) {
					DungeonBuilder.addDisconnectedRoom(dungeon, component.get(0));
					return 0;
				}
			}

			for (int i = 0; i < csz; i++) {
				final Zone z = component.get(i);
				wallify(gdata, z);
				if (logger != null && logger.isInfoEnabled())
					infoLog("Wallified a zone of size " + z.size());
				// FIXME CH Record this zone's center as a candidate for water
				// fill
			}
			infoLog("Total of wallification: " + sz + " (bound is " + bound + " cells)");
			return 0;
		}

		final int nbCellsInComponent = Zones.size(component);
		/*
		 * To ensure 'generateCorridors' precondition that we give it only rooms
		 */
		component.removeAll(dungeon.corridors);
		final int nbc = generateCorridors(gdata, component, connectedRooms, true);
		final boolean connected = 0 < nbc;
		if (connected) {
			connectedRooms.addAll(component);
			return nbCellsInComponent;
		} else
			return 0;
	}

	/** @return The size under which a disconnected component is wallified */
	protected int getWallificationBound() {
		return mapSize() / 24;
	}

	/** Turns a zone into walls, hereby removing it */
	protected final void wallify(GenerationData gdata, Zone z) {
		final Dungeon dungeon = gdata.dungeon;
		assert DungeonBuilder.hasZone(dungeon, z);
		removeZone(gdata, z);
		DungeonBuilder.setSymbols(dungeon, z.iterator(), DungeonSymbol.WALL);
		draw(dungeon);
	}

	/**
	 * This method should be called before making all rooms connected, as it'll
	 * recycle them into water areas.
	 * 
	 * @param gdata
	 * @throws IllegalStateException
	 *             If the stairs haven't been set yet.
	 */
	protected void generateWater(GenerationData gdata) {
		if (waterPercentage == 0)
			/* Nothing to do */
			return;
		final Dungeon dungeon = gdata.dungeon;
		if (!DungeonBuilder.hasStairs(dungeon))
			throw new IllegalStateException("Stairs must be set for generateWater to be called");
		// FIXME CH Change all that
		/* Try to garbage collect disconnected rooms */
		final List<Zone> disconnectedZones = gdata.zonesDisconnectedFrom(true, true, dungeon.upwardStair,
				dungeon.downwardStair);
		final int nbdz = disconnectedZones.size();
		final List<Zone> disconnectedRooms = new ArrayList<Zone>(nbdz / 4);
		if (!disconnectedRooms.isEmpty() && logger != null && logger.isInfoEnabled())
			infoLog("Found " + disconnectedRooms.size() + " disconnected room"
					+ (disconnectedRooms.size() == 1 ? "" : "s"));
		for (int i = 0; i < nbdz; i++) {
			final Zone z = disconnectedZones.get(i);
			if (DungeonBuilder.isRoom(dungeon, z))
				disconnectedRooms.add(z);
		}
		final int waterObjective = (int) (mapSize() * (waterPercentage / 100f));
		int waterCells = 0;
		while (!disconnectedRooms.isEmpty() && disconnectedRoomsObjective < disconnectedRooms.size()
				&& waterCells < waterObjective) {
			final int wdiff = waterObjective - waterCells;
			assert 0 < wdiff;
			/* Pick room whose size is closest to wdiff */
			Collections.sort(disconnectedRooms, new Comparator<Zone>() {
				@Override
				public int compare(Zone o1, Zone o2) {
					final int diff1 = Math.abs(o1.size() - wdiff);
					final int diff2 = Math.abs(o2.size() - wdiff);
					return Integer.compare(diff1, diff2);
				}
			});
			final Zone toFlood = disconnectedRooms.remove(0);
			DungeonBuilder.setSymbols(dungeon, toFlood.iterator(), DungeonSymbol.DEEP_WATER);
			waterCells += toFlood.size();
			removeZone(gdata, toFlood);
			draw(dungeon);
		}
	}

	protected int getMaxRoomSideSize(boolean widthOrHeight, boolean spiceItUp) {
		/*
		 * +1, because #maxRoomWidth and #maxRoomHeight are inclusive, where
		 * RNG#between isn't.
		 */
		final int result = widthOrHeight ? rng.between(minRoomWidth, maxRoomWidth + 1)
				: rng.between(minRoomHeight, maxRoomHeight + 1);
		return result * (spiceItUp ? 2 : 1);
	}

	protected int mapSize() {
		return width * height;
	}

	protected void draw(Dungeon dungeon) {
		if (drawer != null) {
			assert dungeon.invariant();
			drawer.draw(dungeon.getMap());
		}
	}

	/**
	 * @param connection
	 * @return Whether something was found.
	 */
	private boolean getZonesConnectionEndpoints(GenerationData gdata, Zone z1, Zone z2, Coord[] connection) {
		assert connection.length == 2;
		if (z1 == z2) {
			assert false;
			return false;
		}
		final Dungeon dungeon = gdata.dungeon;
		assert DungeonBuilder.hasZone(dungeon, z1);
		assert DungeonBuilder.hasZone(dungeon, z2);
		final Direction fromz1toz2 = Direction.toGoTo(z1.getCenter(), z2.getCenter());
		assert fromz1toz2 != Direction.NONE;
		getConnectionCandidates(gdata, z1, fromz1toz2);
		if (COORD_LIST_BUF.isEmpty())
			return false;
		final Coord z1Endpoint = rng.getRandomElement(COORD_LIST_BUF);
		assert z1.contains(z1Endpoint);
		getConnectionCandidates(gdata, z2, fromz1toz2.opposite());
		if (COORD_LIST_BUF.isEmpty())
			return false;
		final Coord z2Endpoint = rng.getRandomElement(COORD_LIST_BUF);
		assert z2.contains(z2Endpoint);
		connection[0] = z1Endpoint;
		connection[1] = z2Endpoint;
		return true;
	}

	private boolean generateRoom(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		int frustration = 0;
		/*
		 * This bound is quite important. Increasing it makes dungeon generation
		 * slower, but creates more packed dungeons (more small rooms).
		 */
		outer: while (frustration < 8) {
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
				if (mw == 0 || mh == 0)
					/* Cannot do */
					continue;
				if (!allowWidthOrHeightOneRooms && (mw == 1 || mh == 1))
					/* Should not do */
					continue;
				assert 2 <= mw && 2 <= mh;
				/* Bottom-right cell */
				final Coord brCandidate = Coord.get(tlCandidate.x + mw, tlCandidate.y + mh);
				final Coord blCandidate = Coord.get(tlCandidate.x, brCandidate.y);
				/*
				 * .extend() to avoid generating adjacent rooms. This is a smart
				 * trick (as opposed to extending the rooms already created).
				 */
				if (!isOnly(dungeon, Rectangle.Utils.cells(new Rectangle.Impl(blCandidate, mw, mh).extend()),
						DungeonSymbol.WALL))
					continue;
				assert dungeon.isValid(brCandidate);
				assert !Dungeons.isOnEdge(dungeon, brCandidate);
				final Zone zone = generateRoomAt(blCandidate, mw, mh);
				/*
				 * 'zone' must be used now, since the generator's usage has been
				 * recorded in 'generateRoomAt'.
				 */
				if (zone != null) {
					assert !DungeonBuilder.anyOnEdge(dungeon, zone.iterator());
					// infoLog("Generated room: " + zone);
					/* Record the zone */
					addZone(gdata, zone, new Rectangle.Impl(blCandidate, mw, mh), true);
					/* Punch it */
					DungeonBuilder.setSymbols(dungeon, zone.iterator(), DungeonSymbol.FLOOR);
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
		assert 1 <= maxWidth;
		assert 1 <= maxHeight;
		final IRoomGenerator rg = roomGenerators.get(rng);
		if (rg == null)
			return null;
		// infoLog("Trying " + maxWidth + "x" + maxHeight + " room at " +
		// bottomLeft);
		final Zone zeroZeroZone = rg.generate(maxWidth, maxHeight);
		if (zeroZeroZone == null)
			return null;
		final Zone zone = zeroZeroZone.translate(bottomLeft);
		{
			/* Remember that generator is getting used */
			final Lifetime lifetime = rgLifetimes.get(rg);
			if (lifetime == null)
				throw new IllegalStateException(IRoomGenerator.class.getSimpleName() + " has no "
						+ Lifetime.class.getSimpleName() + " instance attached");
			lifetime.recordUsage();
			if (lifetime.shouldBeRemoved()) {
				/* Remove generator */
				roomGenerators.remove(rg);
				rgLifetimes.remove(lifetime);
				lifetime.removeCallback();
			}
		}
		return zone;
	}

	/**
	 * @param gdata
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
	private boolean isDoorCandidate(GenerationData gdata, int x, int y, boolean southNorthOrEastWest) {
		final Dungeon dungeon = gdata.dungeon;
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
		// Using gdata's cellToEncloser cache isn't necessary for performances
		final Zone z1 = gdata.findZoneContaining(x1, y1);
		if (z1 == null) {
			assert false;
			throw new IllegalStateException("Cannot find zone containing " + x1 + "," + y1);
		}
		final Zone z2 = gdata.findZoneContaining(x2, y2);
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

	private Zone addZone(GenerationData gdata, Zone z, /* @Nullable */ Rectangle boundingBox,
			boolean roomOrCorridor) {
		final Zone recorded = needCaching(z) ? new CachingZone(z) : z;
		DungeonBuilder.addZone(gdata.dungeon, recorded, boundingBox, roomOrCorridor);
		for (Coord c : recorded) {
			final Zone prev = gdata.cellToEncloser[c.x][c.y];
			if (prev != null)
				throw new IllegalStateException(
						"Cell " + c + " belongs to zone " + prev + " already. Cannot map it to " + recorded);
			gdata.cellToEncloser[c.x][c.y] = recorded;
		}
		gdata.recordRoomOrdering(recorded);
		return recorded;
	}

	private void removeZone(GenerationData gdata, Zone z) {
		DungeonBuilder.removeZone(gdata.dungeon, z);
		for (Coord c : z) {
			gdata.cellToEncloser[c.x][c.y] = null;
		}
	}

	/**
	 * Fills {@link #COORD_LIST_BUF} with the candidates for building a
	 * connection towards {@code dir} in {@code z}. They should all belong to
	 * {@code z} {@link Zone#getInternalBorder() internal border}.
	 * 
	 * @param gdata
	 * @param z
	 * @param dir
	 * @return Whether something was found.
	 */
	private boolean getConnectionCandidates(GenerationData gdata, Zone z, Direction dir) {
		if (dir == Direction.NONE)
			throw new IllegalStateException();
		COORD_LIST_BUF.clear();
		final boolean zIsARectangle = z instanceof Rectangle;
		if (zIsARectangle && dir.isCardinal()) {
			// (NO-BBOX)
			// Rectangle.Utils.getBorder requires a cardinal direction
			Rectangle.Utils.getBorder((Rectangle) z, dir, COORD_LIST_BUF);
		} else if (z instanceof SingleCellZone) {
			// (NO-BBOX)
			COORD_LIST_BUF.add(z.getCenter());
		} else {
			final Rectangle bbox = zIsARectangle ? (Rectangle) z : gdata.dungeon.boundingBoxes.get(z);
			assert bbox != null;
			final Coord corner = Rectangle.Utils.getCorner(bbox, dir);
			DP_CELL.clear();
			final List<Coord> all = z.getAll();
			final int sz = all.size();
			for (int i = 0; i < sz; i++) {
				final Coord c = all.get(i);
				DP_CELL.union(c, c.distance(corner));
			}
			final Coord coord = DP_CELL.get();
			if (coord != null)
				COORD_LIST_BUF.add(coord);
		}
		return !COORD_LIST_BUF.isEmpty();
	}

	/*
	 * This method makes the assumption that grass hasn't been generated yet It
	 * could be changed by giving an EnumSet<DungeonSymbol> to
	 * nbNeighborsOfType.
	 */
	protected boolean isStairCandidate(Dungeon dungeon, Coord c) {
		final DungeonSymbol sym = dungeon.getSymbol(c);
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
		/* Cardinal neighbors */
		final EnumMultiset<DungeonSymbol> cneighbors = Dungeons.getNeighbors(dungeon, c.x, c.y, false);
		boolean reachable = false;
		/* This pattern to avoid missing a case */
		for (DungeonSymbol dsym : DungeonSymbol.values()) {
			switch (dsym) {
			case CHASM:
			case DEEP_WATER:
				/* No constraint */
				continue;
			case GRASS:
			case SHALLOW_WATER:
			case FLOOR:
				if (cneighbors.contains(dsym))
					reachable |= true;
				continue;
			case DOOR:
			case HIGH_GRASS:
			case STAIR_DOWN:
			case STAIR_UP:
				if (cneighbors.contains(dsym))
					/* Stair should not be cardinally adjacent to those */
					return false;
				continue;
			case WALL:
				/* Constraint checked on diagonal neighbors (see below) */
				continue;
			}
			throw Exceptions.newUnmatchedISE(dsym);
		}
		if (!reachable)
			/* Not cardinally accessible from a safe cell */
			return false;
		/* Diagonal neighbors */
		COORD_LIST_BUF.clear();
		int nbw = 0;
		for (Direction dir : Direction.OUTWARDS) {
			final Coord neighbor = c.translate(dir);
			final DungeonSymbol dsym = dungeon.getSymbol(neighbor);
			if (dsym == null)
				continue;
			switch (dsym) {
			case CHASM:
			case DEEP_WATER:
			case HIGH_GRASS:
				continue;
			case FLOOR:
			case GRASS:
			case SHALLOW_WATER:
				/* Can safely go from such cells to the candidate stair */
				COORD_LIST_BUF.add(neighbor);
				continue;
			case DOOR:
			case STAIR_DOWN:
			case STAIR_UP:
				/* Stair should not be adjacent to a door or stair */
				return false;
			case WALL:
				nbw++;
				continue;
			}
			throw Exceptions.newUnmatchedISE(dsym);
		}
		/**
		 * Because we want stairs in such positions:
		 * 
		 * <pre>
		 * ###
		 * #>#
		 * ...
		 * </pre>
		 * 
		 * because we wanna avoid
		 * 
		 * <pre>
		 * .##
		 * #>#
		 * ...
		 * </pre>
		 * 
		 * which would force cause possible placement weirdness upon arriving
		 * into the level (since placement in different rooms would be
		 * possible).
		 */
		if (nbw < 5)
			return false;
		/**
		 * Because we wanna forbid stairs in such positions:
		 * 
		 * <pre>
		 * ####
		 * #>##
		 * #..#
		 * </pre>
		 */
		final int sources = COORD_LIST_BUF.size();
		if (sources < 3)
			return false;
		/**
		 * Check that all sources are adjacent to each other. To avoid:
		 * 
		 * <pre>
		 * ..#
		 * #>#
		 * .##
		 * </pre>
		 */
		if (!haveACrossRoad(COORD_LIST_BUF))
			return false;
		COORD_LIST_BUF.clear();
		return true;
	}

	/**
	 * @param dir
	 * @return A random cell. In the direction {@code dir} (think about the map
	 *         being split in 8 parts) if {@code dir} is not null.
	 */
	protected final Coord getRandomCell(/* @Nullable */ Direction dir) {
		if (dir == null)
			return Coord.get(rng.nextInt(width), rng.nextInt(height));
		else {
			final boolean hasup = dir.hasUp();
			final boolean hasdown = dir.hasDown();
			assert !(hasup && hasdown);
			final boolean hasleft = dir.hasLeft();
			final boolean hasright = dir.hasRight();
			assert !(hasleft && hasright);
			final int w3 = width / 3;
			final int h3 = height / 3;
			int x = rng.nextInt(w3);
			if (!hasleft) {
				x += w3;
				/* Can be centered or to the right */
				if (hasright)
					/* To the right */
					x += w3;
			}
			int y = rng.nextInt(h3);
			if (!hasup) {
				/* Can be centered or downward */
				y += h3;
				if (hasdown)
					/* Is downward */
					y += h3;
			}
			assert x < width;
			assert y < width;
			return Coord.get(x, y);
		}
	}

	protected final Direction getDirection(Coord c) {
		final Coord center = Coord.get(width / 2, height / 2);
		return Direction.getCardinalDirection(c.x - center.x, c.y - center.y);
	}

	/**
	 * @param xsym
	 *            Whether to do symmetry according to x
	 * @param ysym
	 *            Whether to do symmetry according to y
	 * @return The symetric of {@code c} in {code this}.
	 */
	protected final Coord getSymmetric(Coord c, boolean xsym, boolean ysym) {
		final int x = xsym ? getSymmetric(c.x, true) : c.x;
		final int y = ysym ? getSymmetric(c.y, false) : c.y;
		final Coord result = Coord.get(x, y);
		return result;
	}

	protected final int getSymmetric(int v, boolean xOrY) {
		final int middle = (xOrY ? width : height) / 2;
		final int diff = middle - v;
		return diff < 0 ? middle + diff : middle - diff;
	}

	/**
	 * @return A variant of {@code c} (or {@code c} itself) clamped to be valid
	 *         in {@code this}.
	 */
	protected final Coord clamp(Coord c) {
		final int x = Ints.clamp(0, c.x, width - 1);
		final int y = Ints.clamp(0, c.y, height - 1);
		return x == c.y && y == c.y ? c : Coord.get(x, y);
	}

	protected final boolean isSquare() {
		return width == height;
	}

	protected final boolean isWide() {
		return height < width;
	}

	/**
	 * You should avoid calling this method too muchif {@code logger} is null or
	 * if info isn't enabled, because building {@code log} can be costly if it's
	 * not a constant.
	 * 
	 * @param log
	 */
	protected final void infoLog(String log) {
		if (logger != null)
			logger.infoLog(Tags.GENERATION, log);
	}

	protected final void warnLog(String log) {
		if (logger != null)
			logger.warnLog(Tags.GENERATION, log);
	}

	protected final void errLog(String log) {
		if (logger != null)
			logger.errLog(Tags.GENERATION, log);
	}

	private static Pair<Zone, Zone> orderedPair(GenerationData gdata, Zone z1, Zone z2) {
		final Integer i1 = gdata.zOrder.get(z1);
		if (i1 == null)
			throw new IllegalStateException("Unknown zone: " + z1);
		final Integer i2 = gdata.zOrder.get(z2);
		if (i2 == null)
			throw new IllegalStateException("Unknown zone: " + z2);
		return i1 < i2 ? Pair.of(z1, z2) : Pair.of(z2, z1);
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
	 * @param list
	 * @return {@code true} if there's a member of {@code list} to which all
	 *         other members are adjacent.
	 */
	private static boolean haveACrossRoad(List<Coord> list) {
		final int size = list.size();
		outer: for (int i = 0; i < size; i++) {
			final Coord c = list.get(i);
			for (int j = 0; j < size; j++) {
				if (i == j)
					continue;
				final Coord other = list.get(i);
				if (!c.isAdjacent(other))
					continue outer;
			}
			return true;
		}
		return true;
	}

	private static boolean needCaching(Zone z) {
		if (z instanceof Rectangle)
			// (NO-BBOX)
			return false;
		else if (z instanceof SingleCellZone)
			// (NO-BBOX)
			return false;
		else
			return true;
	}

	private static Comparator<Coord> newDistanceComparatorFrom(final Coord c) {
		return new Comparator<Coord>() {
			@Override
			public int compare(Coord o1, Coord o2) {
				return Double.compare(o1.distance(c), o2.distance(c));
			}
		};
	}

	private static final Comparator<Pair<Double, Zone>> ORDERER = new Comparator<Pair<Double, Zone>>() {
		@Override
		public int compare(Pair<Double, Zone> o1, Pair<Double, Zone> o2) {
			return Double.compare(o1.getFst(), o2.getFst());
		}
	};

	/**
	 * Data carried on during generation of a single dungeon.
	 * 
	 * @author smelC
	 */
	private static class GenerationData {

		protected final Dungeon dungeon;
		/**
		 * An array that keeps track of the zone to which a cell belongs. A cell
		 * belongs to at most one zone, because all zones are exclusive. All
		 * zones in this array belong to {@link #dungeon}.
		 */
		protected final Zone[][] cellToEncloser;
		/**
		 * A map that keep tracks in the order in which {@link Dungeon#rooms}
		 * and {@link Dungeon#corridors} have been generated, hereby providing
		 * an ordering on rooms.
		 */
		protected final Map<Zone, Integer> zOrder = new HashMap<Zone, Integer>();

		/** A buffer of size {@link Dungeon#width} and{@link Dungeon#height} */
		private boolean buf[][];

		private int nextRoomIndex = 0;

		/** Current stage is the stage whose value is -1 */
		private final /* @Nullable */ Stopwatch watch;
		private final EnumMap<Stage, Long> timings;

		protected GenerationData(Dungeon dungeon, /* @Nullable */ Stopwatch watch) {
			this.dungeon = dungeon;
			this.cellToEncloser = new Zone[dungeon.width][dungeon.height];
			this.timings = new EnumMap<Stage, Long>(Stage.class);
			this.timings.put(Stage.INIT, -1l);
			this.watch = watch;
		}

		protected void startStage(/* @Nullable */ Stage next) {
			if (watch == null)
				return;
			Stage current = null;
			for (Stage s : Stage.values()) {
				if (timings.get(s) == -1l) {
					current = s;
					break;
				}
			}
			if (current == null)
				throw new IllegalStateException("Stage not found: " + current);
			timings.put(current, watch.getDuration());
			if (next != null) {
				watch.reset();
				timings.put(next, -1l);
			}
		}

		protected void recordRoomOrdering(Zone z) {
			assert DungeonBuilder.hasZone(dungeon, z);
			final Integer prev = this.zOrder.put(z, nextRoomIndex);
			nextRoomIndex++;
			if (prev != null)
				throw new IllegalStateException("Zone " + z + " is being recorded twice");
		}

		/**
		 * Note that this method is designed to return {@code true} when
		 * {@code from} or {@code to} is a wall accessible from a floor. That's
		 * because this method is used to check stairs-accessibility.
		 * 
		 * @param from
		 * @param to
		 * @param considerDiagonals
		 *            Whether to allow diagonal moves.
		 * @param unsafe
		 *            Whether to consider unsafe moves (through deep water).
		 * @return true if there exists a path from {@code from} to {@code to}.
		 */
		protected boolean pathExists(Coord from, Coord to, boolean considerDiagonals, boolean unsafe) {
			if (from.equals(to))
				return true;
			/* #buf acts as a Set<Coord> dones */
			prepareBuffer();
			/* Cells in 'todo' are cells reachable from 'from' */
			final Queue<Coord> todo = new LinkedList<Coord>();
			todo.add(from);
			final Direction[] moves = considerDiagonals ? Direction.OUTWARDS : Direction.CARDINALS;
			while (!todo.isEmpty()) {
				final Coord next = todo.remove();
				if (buf[next.x][next.y])
					continue;
				assert !to.equals(next);
				for (Direction dir : moves) {
					final Coord neighbor = next.translate(dir);
					if (buf[neighbor.x][neighbor.y]) {
						assert !to.equals(neighbor);
						continue;
					}
					if (to.equals(neighbor))
						return true;
					final DungeonSymbol sym = dungeon.getSymbol(neighbor);
					if (sym == null)
						continue;
					switch (sym) {
					case CHASM:
					case STAIR_DOWN:
					case STAIR_UP:
					case WALL:
						continue;
					case DEEP_WATER:
						if (!unsafe)
							continue;
						//$FALL-THROUGH$
					case DOOR:
					case FLOOR:
					case GRASS:
					case HIGH_GRASS:
					case SHALLOW_WATER:
						todo.add(neighbor);
						continue;
					}
					throw Exceptions.newUnmatchedISE(sym);
				}
				assert !buf[next.x][next.y];
				// Record it was done
				buf[next.x][next.y] = true;
			}
			return false;
		}

		protected Set<Zone> zonesConnectedTo(Coord... starts) {
			prepareBuffer();
			final Set<Zone> result = new LinkedHashSet<Zone>(DungeonBuilder.getNumberOfZones(dungeon) / 2);
			/* Cells in 'todo' are cells reachable from 'from' */
			final Queue<Coord> todo = new LinkedList<Coord>();
			for (Coord start : starts)
				todo.add(start);
			final Direction[] moves = Direction.CARDINALS;
			while (!todo.isEmpty()) {
				final Coord next = todo.remove();
				if (buf[next.x][next.y])
					continue;
				for (Direction dir : moves) {
					final Coord neighbor = next.translate(dir);
					if (buf[neighbor.x][neighbor.y]) {
						/* Done already */
						continue;
					}
					final DungeonSymbol sym = dungeon.getSymbol(neighbor);
					if (sym == null)
						continue;
					switch (sym) {
					case CHASM:
					case DEEP_WATER:
					case STAIR_DOWN:
					case STAIR_UP:
					case WALL:
						continue;
					case DOOR:
					case FLOOR:
					case GRASS:
					case HIGH_GRASS:
					case SHALLOW_WATER:
						final Zone z = cellToEncloser[neighbor.x][neighbor.y];
						result.add(z);
						todo.add(neighbor);
						continue;
					}
					throw Exceptions.newUnmatchedISE(sym);
				}
				assert !buf[next.x][next.y];
				// Record it was done
				buf[next.x][next.y] = true;
			}
			return result;
		}

		/**
		 * @param considerRooms
		 *            Whether to consider rooms for inclusion in the result.
		 * @param considerCorridors
		 *            Whether to consider corridors for inclusion in the result.
		 * @param starts
		 * @return Zones that are not reachable from {@code starts}.
		 */
		protected List<Zone> zonesDisconnectedFrom(boolean considerRooms, boolean considerCorridors,
				Coord... starts) {
			final int sz = (considerRooms ? dungeon.rooms.size() : 0)
					+ (considerCorridors ? dungeon.corridors.size() : 0);
			final List<Zone> result = new ArrayList<Zone>(sz / 8);
			if (considerRooms)
				result.addAll(dungeon.rooms);
			if (considerCorridors)
				result.addAll(dungeon.corridors);
			result.removeAll(zonesConnectedTo(starts));
			return result;
		}

		protected Zone findZoneContaining(int x, int y) {
			if (cellToEncloser != null)
				return cellToEncloser[x][y];
			return DungeonBuilder.findZoneContaining(dungeon, x, y);
		}

		protected void logTimings(ILogger logger) {
			if (watch == null || logger == null || !logger.isInfoEnabled())
				return;
			long total = 0;
			final String tag = Tags.GENERATION;
			for (Stage stage : Stage.values()) {
				final long duration = timings.get(stage);
				if (duration < 0)
					logger.warnLog(tag, "Duration of stage " + stage + " is unexpectedly " + duration);
				total += timings.get(stage);
			}
			final int width = dungeon.width;
			final int height = dungeon.height;
			final int mapSize = width * height;
			logger.infoLog(tag, "Generated " + width + "x" + height + " dungeon (" + mapSize + " cells) in "
					+ total + "ms.");
			if (1000 < mapSize)
				logger.infoLog(tag,
						"That's approximately " + (int) ((1000f / mapSize) * total) + "ms per 1K cells.");
			for (Stage stage : Stage.values())
				logger.infoLog(tag, "Stage " + stage + " took " + timings.get(stage) + "ms");
		}

		private void prepareBuffer() {
			final int width = dungeon.width;
			final int height = dungeon.height;
			if (buf == null)
				buf = new boolean[width][height];
			else {
				for (int x = 0; x < width; x++) {
					for (int y = 0; y < height; y++)
						buf[x][y] &= false;
				}
			}
		}

	}

	/**
	 * The stages of generation. Used for logging performances.
	 * 
	 * @author smelC
	 */
	private static enum Stage {
		/* In the order in which they are executed */
		INIT, ROOMS, PASSAGES_IN_ALMOST_ADJACENT_ROOMS, CORRIDORS, STAIRS, ENSURE_DENSITY, WATER
	}
}
