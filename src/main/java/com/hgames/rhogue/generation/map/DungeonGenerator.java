package com.hgames.rhogue.generation.map;

import static com.hgames.lib.Strings.plural;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.hgames.lib.Arrays;
import com.hgames.lib.Exceptions;
import com.hgames.lib.Ints;
import com.hgames.lib.Stopwatch;
import com.hgames.lib.collection.list.Lists;
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.connection.IConnectionFinder;
import com.hgames.rhogue.generation.map.corridor.CorridorBuilders;
import com.hgames.rhogue.generation.map.corridor.ICorridorBuilder;
import com.hgames.rhogue.generation.map.draw.ConsoleDungeonDrawer;
import com.hgames.rhogue.generation.map.draw.IDungeonDrawer;
import com.hgames.rhogue.generation.map.flood.DungeonWaterStartFloodFill;
import com.hgames.rhogue.generation.map.flood.FloodFill;
import com.hgames.rhogue.generation.map.flood.IFloodObjective;
import com.hgames.rhogue.generation.map.lifetime.Lifetime;
import com.hgames.rhogue.generation.map.stair.IStairGenerator;
import com.hgames.rhogue.generation.map.stair.StairGenerator;
import com.hgames.rhogue.rng.ProbabilityTable;
import com.hgames.rhogue.zone.CachingZone;
import com.hgames.rhogue.zone.SingleCellZone;
import com.hgames.rhogue.zone.Zones;

import squidpony.squidgrid.Direction;
import squidpony.squidgrid.mapping.Rectangle;
import squidpony.squidgrid.zone.ListZone;
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
 * @see Dungeon
 * @see DungeonGenerators
 */
public class DungeonGenerator {

	protected final RNG rng;
	protected final int width;
	protected final int height;
	/**
	 * An upper bound of the number of corridors to and from a room (ignores doors
	 * punched because of rooms being adjacent).
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
	 * The percentage of walkable cells that will be turned into grass
	 * (approximately). In [0, 100].
	 */
	protected int grassPercentage = 15;
	/** The number of patches of grasses to generate (approximately) */
	protected int grassPatches = 5;

	/**
	 * Whether rooms whose width and height of size 1 are allowed. They look like
	 * corridors, so they are ruled out by default; but it can be fun to have them.
	 */
	protected boolean allowWidthOrHeightOneRooms = false;

	/** Whether to do water before rooms. This makes water more central. */
	protected boolean startWithWater;

	/**
	 * The percentage of the map that will be turned into deep water
	 * (approximately). In [0, 100].
	 */
	protected int waterPercentage = 15;

	/**
	 * The number of zones to fill with water. Ignored if {@link #waterPercentage}
	 * is 0.
	 */
	protected int waterPools = 2;

	protected int waterIslands = 0;

	/**
	 * The number of unconnected rooms (w.r.t. to the stairs) to aim for. Useful for
	 * secret rooms that require carving.
	 */
	protected int disconnectedRoomsObjective = 0;

	/** Where to put the upward stair (approximately) */
	protected /* @Nullable */ Coord upStairObjective;
	/** Where to put the downward stair (approximately) */
	protected /* @Nullable */ Coord downStairObjective;

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
	 *            "https://twitter.com/hgamesdev/status/899609612554559489"> image
	 *            </a>). False by default.
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
	 * @param percent
	 *            The percentage of walkable cells to turn into grass. If negative,
	 *            unchanged; otherwise must be in [0, 100].
	 * @param nbPools
	 *            The number of pools of grasses to generate or something negative
	 *            not to change anything.
	 * @return {@code this}
	 * @throws IllegalStateException
	 *             If {@code 100 < percent}
	 */
	public DungeonGenerator setGrassObjectives(int percent, int nbPools) {
		if (100 < percent)
			throw new IllegalStateException(
					"Percentage of grass must be negative or in [0, 100]. Received: " + percent);
		if (0 <= percent)
			this.grassPercentage = percent;
		if (0 <= grassPatches)
			this.grassPatches = nbPools;
		return this;
	}

	/**
	 * @param minWidth
	 *            The minimum width of rooms. The default is 2. Give anything
	 *            negative to keep the existing value (useful to only change a
	 *            subset of the sizes).
	 * @param maxWidth
	 *            The maximum width of rooms (inclusive). The default is
	 *            {@link #width} / 5. Give anything negative to keep the existing
	 *            value (useful to only change a subset of the sizes).
	 * @param minHeight
	 *            The minimum width of rooms. The default is 2. Give anything
	 *            negative to keep the existing value (useful to only change a
	 *            subset of the sizes).
	 * @param maxHeight
	 *            The maximum height of rooms (inclusive). The default is
	 *            {@link #height} / 5. Give anything negative to keep the existing
	 *            value (useful to only change a subset of the sizes).
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
	 * Sets objectives to place the stairs. By default there are no objectives which
	 * means a first stair is placed randomly and the second stair is placed far
	 * away from the first one.
	 * 
	 * @param upStair
	 *            A coord or null.
	 * @param downStair
	 *            A coord or null.
	 * @return {@code this}.
	 */
	public DungeonGenerator setStairsObjectives(/* @Nullable */Coord upStair, /* @Nullable */Coord downStair) {
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
			throw new IllegalStateException("Disconnected rooms objective must be >= 0. Received: " + objective);
		this.disconnectedRoomsObjective = objective;
		return this;
	}

	/**
	 * @param startWithWater
	 *            Whether to do water before rooms. This makes water more central.
	 * @param percent
	 *            An int in [0, 100]. The percentage of the map to turn into water.
	 *            Or anything negative not to change it.
	 * @param pools
	 *            An int in [0, Integer.MAX_VALUE]. The number of pools to create.
	 *            Or anything negative not to change it.
	 * @param islands
	 *            An int in [0, Integer.MAX_VALUE]. The number of islands to
	 *            generate. An island is a room solely surrounded by deep water. Or
	 *            anything negative not to change it.
	 * @return {@code this}
	 * @throws IllegalStateException
	 *             If {@code percent} is greater than 100.
	 */
	public DungeonGenerator setWaterObjective(boolean startWithWater, int percent, int pools, int islands) {
		this.startWithWater = startWithWater;
		if (0 <= percent) {
			if (100 < percent)
				throw new IllegalStateException("Excepted a value in [0, 100]. Received: " + percent);
			this.waterPercentage = percent;
		}
		/* else do not change it */
		if (0 <= pools)
			this.waterPools = pools;
		/* else do not change it */
		if (0 <= islands)
			this.waterIslands = islands;
		return this;
	}

	/**
	 * Record {@code roomGenerator} as a generator used by this dungeon generator.
	 * 
	 * @param roomGenerator
	 *            The generator to record.
	 * @param probability
	 *            The probability of using {@code roomGenerator} among all room
	 *            generators installed.
	 * @param lifetime
	 * @return {@code this}.
	 */
	public DungeonGenerator installRoomGenerator(IRoomGenerator roomGenerator, int probability, Lifetime lifetime) {
		this.roomGenerators.add(roomGenerator, probability);
		this.rgLifetimes.put(roomGenerator, lifetime);
		return this;
	}

	/** @return A fresh dungeon or null if it could not be generated. */
	public Dungeon generate() {
		if (roomGenerators.isEmpty()) {
			final String msg = "You need to install at least one room generator (using method installRoomGenerator). Cannot generate dungeons.";
			if (logger != null && logger.isErrEnabled())
				infoLog(msg);
			else
				/* We usually don't do that, but it's friendly for newcomers */
				System.out.println(msg);
			return null;
		}
		/*
		 * /!\ Don't forget to disable assertions when checking performances. Assertions
		 * are by far the longest thing! /!\.
		 */
		final Stopwatch watch = (logger != null && logger.isInfoEnabled()) ? new Stopwatch() : null;
		final DungeonSymbol[][] map = new DungeonSymbol[width][height];
		final Dungeon dungeon = new Dungeon(map);
		dungeon.getBuilder().setAllSymbols(DungeonSymbol.WALL);
		if (width == 0 || height == 0)
			// Nothing to do
			return dungeon;
		final GenerationData gdata = new GenerationData(dungeon, watch);
		gdata.startStage(Stage.WATER_START);
		if (startWithWater)
			generateWater(gdata);

		if (!doStage(Stage.ROOMS, new RoomComponent(), gdata))
			return null;

		if (!doStage(Stage.PASSAGES_IN_ALMOST_ADJACENT_ROOMS, new PassagesComponent(), gdata))
			return null;

		draw(gdata.dungeon);
		gdata.startStage(Stage.CORRIDORS);
		generateCorridors(gdata, dungeon.rooms, dungeon.rooms,
				new ICorridorControl.Impl(dungeon, true, false, false, false));
		/* Must be called before 'generateWater' */
		gdata.startStage(Stage.STAIRS);
		final boolean good = generateStairs(gdata);
		if (!good) {
			if (logger != null && logger.isDebugEnabled())
				logger.infoLog(Tags.GENERATION, dungeon.dirtyPrint());
			return null;
		}
		gdata.startStage(Stage.ENSURE_DENSITY);
		ensureDensity(gdata);

		gdata.startStage(Stage.WATER);
		if (!startWithWater)
			generateWater(gdata);

		if (!doStage(Stage.GRASS, new GrassGenerator(), gdata))
			return null;

		gdata.startStage(null); // Record end of last stage
		gdata.logTimings(logger);
		return dungeon;
	}

	private boolean doStage(Stage stage, GeneratorComponent component, GenerationData gdata) {
		gdata.startStage(stage);
		return component.generate(this, gdata);
	}

	/**
	 * @author smelC
	 */
	interface ICorridorControl {

		/**
		 * @return Whether {@link #getBuilder()} is a perfect corridor builder (see
		 *         {@link CorridorBuilders})
		 * 
		 *         <p>
		 *         Roughly: returns true if carving can only go through through walls
		 *         and through cells next to walls. Otherwise loosen the leash.
		 *         </p>
		 */
		public boolean getPerfect();

		/**
		 * @return The maximum length of corridors.
		 */
		public int getLengthLimit();

		/** @return true to try a little harder */
		public boolean force();

		/** @return The corridor builder to use. */
		public ICorridorBuilder getBuilder();

		/**
		 * @author smelC
		 */
		static class Impl implements ICorridorControl {

			private final boolean perfect;
			private final ICorridorBuilder builder;
			private final int limit;
			private final boolean force;

			protected Impl(Dungeon dungeon, boolean perfect, boolean bresenhamFirst, boolean useSnd, int limit,
					boolean force) {
				this.perfect = perfect;
				this.builder = useSnd ? CorridorBuilders.createCombination(dungeon, perfect, bresenhamFirst)
						: CorridorBuilders.create(dungeon, perfect, bresenhamFirst);
				if (limit < 0)
					throw new IllegalStateException("Limit of length of corridors must be >= 0. Received: " + limit);
				this.limit = limit;
				this.force = force;
			}

			protected Impl(Dungeon dungeon, boolean perfect, boolean bresenham, boolean useSnd, boolean force) {
				this(dungeon, perfect, bresenham, useSnd, (dungeon.width + dungeon.height) / 6, force);
			}

			@Override
			public boolean getPerfect() {
				return perfect;
			}

			@Override
			public ICorridorBuilder getBuilder() {
				return builder;
			}

			@Override
			public int getLengthLimit() {
				return limit;
			}

			@Override
			public boolean force() {
				return force;
			}
		}

	}

	/**
	 * @param rooms
	 *            The rooms to build corridors from.
	 * @param dests
	 *            The possible destinations (should be rooms too).
	 * @return Whether at least one corridor was built.
	 */
	protected boolean generateCorridors(GenerationData gdata, Collection<Zone> rooms, List<Zone> dests,
			ICorridorControl control) {
		return new CorridorsComponent(rooms, dests, control).generate(this, gdata);
	}

	/**
	 * Cleanup {@link Dungeon#waterPools}: remove lonelies and then delete empty
	 * pools.
	 */
	protected void cleanWaterPools(final GenerationData gdata, /* @Nullable */ Collection<? extends Zone> needCleanUp) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		final Iterator<ListZone> it = dungeon.waterPools.iterator();
		while (it.hasNext()) {
			final ListZone next = it.next();
			if (needCleanUp != null && !needCleanUp.contains(next))
				continue;
			final List<Coord> all = next.getState();
			/* /!\ Mutes 'all' */
			DungeonGeneratorHelper.replaceLonelies(next, new CellDoer() {
				@Override
				public void doOnCell(Coord c) {
					/* Remove from the pool */
					assert !all.contains(c) : c + " should have been removed already";
					builder.setSymbol(c, DungeonSymbol.WALL);
					/* Adapt cell->Zone cache */
					gdata.cellToEncloser[c.x][c.y] = null;
					draw(dungeon);
				}
			});
			if (next.isEmpty())
				it.remove();
		}
	}

	/** @return Whether the stairs could be placed */
	private boolean generateStairs(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		final Coord up = generateStair(gdata, true, null, false);
		if (up == null) {
			warnLog("Cannot place upward stair");
			return false;
		} else if (logger != null && logger.isInfoEnabled())
			infoLog("Placed upward stair at " + up);
		assert dungeon.getSymbol(dungeon.upwardStair) == DungeonSymbol.STAIR_UP;
		draw(dungeon);
		final Coord down = generateStair(gdata, false, null, false);
		if (down == null) {
			warnLog("Cannot place downward stair");
			return false;
		} else if (logger != null && logger.isInfoEnabled())
			infoLog("Placed downward stair at " + down);
		assert dungeon.getSymbol(dungeon.downwardStair) == DungeonSymbol.STAIR_DOWN;
		draw(dungeon);
		return true;
	}

	/**
	 * @param objective
	 *            Where to place the stair, or null to let this method choose.
	 * @param lastHope
	 *            true if failing will doom generation for sure.
	 * @return Where it got generated, if it did (otherwise null)
	 */
	protected /* @Nullable */ Coord generateStair(GenerationData gdata, boolean upOrDown,
			/* @Nullable */ Coord objective, boolean lastHope) {
		final Dungeon dungeon = gdata.dungeon;
		final IStairGenerator generator = getStairGenerator(gdata, objective, upOrDown);
		final Iterator<Coord> candidates = generator.candidates();
		if (candidates == null || !candidates.hasNext()) {
			infoLog("No candidate for stair " + (upOrDown ? "up" : "down"));
			return null;
		}

		/* 'lastHope' => 'trieds' won't be used */
		final List<Coord> trieds = lastHope ? null : new ArrayList<Coord>(32);
		final /* @Nullable */ Coord other = dungeon.getStair(!upOrDown);
		while (candidates.hasNext()) {
			final Coord candidate = candidates.next();
			if (other == null || (!other.equals(candidate) && gdata.pathExists(other, candidate, false, false))) {
				if (punchStair(gdata, candidate, upOrDown))
					return candidate;
			}
			if (trieds != null)
				trieds.add(candidate);
		}
		if (lastHope)
			return null;
		if (other == null)
			return null;
		/*
		 * It may be a connectivity problem, if the stair objective is only close to
		 * rooms that aren't connected to the other stair. Let's try to fix that.
		 */
		final List<Zone> dests = new ArrayList<Zone>(
				gdata.zonesConnectedTo(true, false, Collections.singletonList(other)));
		assert !dests.isEmpty();
		final Collection<Zone> sources = gdata.zonesConnectedTo(true, false, trieds);
		infoLog("Could not generate " + (upOrDown ? "upward" : "downward")
				+ " stair, trying to fix connectivity issue (around " + objective + ") if any (" + sources.size()
				+ " sources and " + dests.size() + " destinations).");
		final boolean built = generateCorridors(gdata, sources, dests,
				new ICorridorControl.Impl(dungeon, false, true, false, Integer.MAX_VALUE, true));
		if (!built) {
			infoLog("Could not fix connectivity issue. Failing.");
			return null;
		} else
			infoLog("Fixed connectivity issue by creating corridor(s)");
		return generateStair(gdata, upOrDown, objective, true);
	}

	protected IStairGenerator getStairGenerator(GenerationData gdata, /* @Nullable */ Coord objective,
			boolean upOrDown) {
		final Dungeon dungeon = gdata.dungeon;
		final IConnectionFinder connections = new IConnectionFinder() {
			@Override
			public boolean areConnected(Zone z0, Zone z1, int intermediates) {
				return Dungeons.areConnected(dungeon, z0, z1, intermediates);
			}
		};
		return new StairGenerator(logger, rng, dungeon, objective, upOrDown, gdata, connections);
	}

	/** @return Whether punching was done */
	protected boolean punchStair(GenerationData gdata, Coord c, boolean upOrDown) {
		final Dungeon dungeon = gdata.dungeon;
		dungeon.getBuilder().setStair(c.x, c.y, upOrDown);
		return true;
	}

	/**
	 * Make sure that at least 1/5th of the map is accessible. For that, find
	 * disconnected rooms. For every disconnected component whose size is >
	 * {@link #getWallificationBound()} of the map, try very hard to connect it to
	 * the stairs. At the end check if 1/6th of the map is accessible.
	 * 
	 * <p>
	 * This method must be called before generating impassable terrain (deep water,
	 * lava, etc.)
	 * </p>
	 * 
	 * @return Whether the dungeon is valid.
	 */
	protected boolean ensureDensity(GenerationData gdata) {
		final Dungeon dungeon = gdata.dungeon;
		if (!Dungeons.hasStairs(dungeon))
			throw new IllegalStateException("ensureDensity method requires stairs to be set");
		final List<Zone> disconnectedZones = gdata.zonesDisconnectedFrom(true, true,
				Lists.newArrayList(dungeon.upwardStair, dungeon.downwardStair));
		final List<List<Zone>> disconnectedComponents = Dungeons.connectedComponents(dungeon, disconnectedZones);
		final int nbdc = disconnectedComponents.size();
		int reachable = Zones.size(dungeon.rooms);
		reachable += Zones.size(dungeon.corridors);
		final int msz = dungeon.size();
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
				 * /!\ This call mutes 'connectedRooms' and trashes 'disconnectedComponent' /!\
				 */
				final int extension = treatDisconnectedComponent(gdata, connectedRooms, disconnectedComponent);
				assert 0 <= extension;
				if (0 < extension) {
					reachable += extension;
					if (logger != null && logger.isInfoEnabled())
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
	private int treatDisconnectedComponent(GenerationData gdata, List<Zone> connectedRooms, List<Zone> component) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		final int sz = Zones.size(component);
		final int csz = component.size();
		if (csz == 1) {
			/* Component is a single room */
			final Zone z = component.get(0);
			/* Can it be used to honor #disconnectedRoomsObjective ? */
			if (dungeon.getDisconnectedRooms().size() < disconnectedRoomsObjective) {
				infoLog("Used a size " + sz + " disconnected room to fulfill the disconnected rooms objective.");
				builder.addDisconnectedRoom(z);
				return 0;
			}
			/* Can it be used to honor #waterIslands ? */
			if (dungeon.getWaterIslands().size() < waterIslands
					&& Dungeons.isSurroundedBy(dungeon, z, EnumSet.of(DungeonSymbol.DEEP_WATER))) {
				infoLog("Used a size " + sz + " disconnected room to fulfill the water islands objective.");
				builder.addWaterIsland(z);
				return 0;
			}
		}

		final int bound = getWallificationBound();
		if (csz == 1) {
			/* Component is a single room */
			/* Is it kindof a water island ? */
			if (Dungeons.isSurroundedBy(dungeon, component.get(0),
					EnumSet.of(DungeonSymbol.DEEP_WATER, DungeonSymbol.WALL))) {
				/*
				 * Yes it's a water island. Try to connect it with shallow water; coz such
				 * islands can be fun.
				 */
				final Zone z = component.get(0);
				final boolean built = generateCorridors(gdata, component, connectedRooms, new ICorridorControl.Impl(
						dungeon, false, true, true, Math.max(z.getWidth(), z.getHeight()) * 2, false));
				// FIXME CH Generate doors on these corridors ?
				if (built) {
					if (logger != null && logger.isInfoEnabled())
						infoLog("Connected a water island of size " + csz);
					return csz;
				}
			}
		}

		if (sz < bound && sz < getWallificationBound()) {
			/* Component is small */
			/* Replace it with walls (hereby removing it) */
			for (int i = 0; i < csz; i++) {
				final Zone z = component.get(i);
				wallify(gdata, z);
				if (logger != null && logger.isInfoEnabled())
					infoLog("Wallified a zone of size " + z.size());
				gdata.addWaterFillStartCandidate(z.getCenter());
			}
			infoLog("Total of wallification: " + sz + " (bound is " + bound + " cells)");
			return 0;
		}

		final int nbCellsInComponent = Zones.size(component);
		/* To ensure 'generateCorridors' precondition that it gets only rooms */
		component.removeAll(dungeon.corridors);
		final boolean connected = generateCorridors(gdata, component, connectedRooms,
				new ICorridorControl.Impl(dungeon, false, true, true, nbCellsInComponent / 2, true));
		if (connected) {
			connectedRooms.addAll(component);
			return nbCellsInComponent;
		} else {
			if (logger != null && logger.isInfoEnabled())
				infoLog("Could not treat a disconnected component of size " + sz);
			return 0;
		}
	}

	/**
	 * @return The size under which a disconnected component can be wallified
	 */
	protected int getWallificationBound() {
		return (width * height) / 128;
	}

	/** Turns a zone into walls, hereby removing it */
	protected final void wallify(GenerationData gdata, Zone z) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		assert Dungeons.hasZone(dungeon, z);
		removeZone(gdata, z);
		builder.setSymbols(z.iterator(), DungeonSymbol.WALL);
		draw(dungeon);
	}

	/**
	 * This method behaves differently as to whether {@link #startWithWater} is set.
	 * If not set, it'll build pools that are connected to existing walkable areas.
	 * 
	 * @param gdata
	 */
	protected void generateWater(GenerationData gdata) {
		if (waterPercentage == 0 || waterPools == 0)
			/* Nothing to do */
			return;
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		Set<Coord> candidates = gdata.getWaterFillStartCandidates();
		if (candidates.isEmpty())
			candidates = new LinkedHashSet<Coord>();
		gdata.removeWaterFillStartCandidates();
		if (candidates.isEmpty()) {
			if (startWithWater) {
				for (int i = 0; i < waterPools; i++) {
					/* So that's it's not too much on the edge */
					final int x = rng.between(width / 4, width - (width / 4));
					final int y = rng.between(height / 4, height - (height / 4));
					candidates.add(Coord.get(x, y));
				}
			} else
				throw new IllegalStateException("Implement me");
		}
		/* The number of cells filled */
		int filled = 0;
		final FloodFill fill = new DungeonWaterStartFloodFill(gdata.dungeon.map, width, height);
		final FloodFillObjective objective = new FloodFillObjective(dungeon, startWithWater);
		final int msz = dungeon.size();
		final int totalObjective = (msz / 100) * waterPercentage;
		final int poolObjective = totalObjective / waterPools;
		int poolsDone = 0;
		final Iterator<Coord> it = candidates.iterator();
		final LinkedHashSet<Coord> spill = new LinkedHashSet<Coord>();
		while (it.hasNext() && poolsDone < waterPools && filled < totalObjective) {
			/* Prepare iteration */
			objective.prepare(poolObjective);
			spill.clear();
			/* Go */
			final Coord candidate = it.next();
			fill.flood(rng, candidate.x, candidate.y, objective, startWithWater, spill);
			if (spill.isEmpty())
				continue;
			final int sz = spill.size();
			for (Coord spilt : spill) {
				assert Dungeons.findZoneContaining(dungeon, spilt.x,
						spilt.y) == null : ("Cells spilt on should not belong to a zone. You should fix 'impassable'. Cell spilt on: "
								+ spilt + " belonging to zone: "
								+ Dungeons.findZoneContaining(dungeon, spilt.x, spilt.y));
				builder.setSymbol(spilt, DungeonSymbol.DEEP_WATER);
				filled++;
			}
			addZone(gdata, new ListZone(new ArrayList<Coord>(spill)), null, ZoneType.DEEP_WATER);
			if (logger != null && logger.isInfoEnabled())
				infoLog("Created water pool of size " + sz); // + ": " + spill);
			draw(dungeon);
			poolsDone++;
		}
	}

	protected int getMaxRoomSideSize(boolean widthOrHeight, boolean spiceItUp) {
		/*
		 * +1, because #maxRoomWidth and #maxRoomHeight are inclusive, where RNG#between
		 * isn't.
		 */
		final int result = widthOrHeight ? rng.between(minRoomWidth, maxRoomWidth + 1)
				: rng.between(minRoomHeight, maxRoomHeight + 1);
		return result * (spiceItUp ? 2 : 1);
	}

	protected void draw(Dungeon dungeon) {
		if (drawer != null) {
			assert dungeon.invariant();
			drawer.draw(dungeon.getMap());
		}
	}

	protected void debugDraw(Dungeon dungeon) {
		new ConsoleDungeonDrawer(new DungeonSymbolDrawer()).draw(dungeon.map);
	}

	/**
	 * @param gdata
	 * @param z
	 *            An instance of {@link ListZone} if ztype is
	 *            {@link ZoneType#DEEP_WATER}.
	 * @param boundingBox
	 * @param ztype
	 */
	protected Zone addZone(GenerationData gdata, Zone z, /* @Nullable */ Rectangle boundingBox, ZoneType ztype) {
		assert z != null;
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		final Zone recorded = needCaching(z, ztype) ? new CachingZone(z) : z;
		switch (ztype) {
		case CORRIDOR:
		case ROOM:
			builder.addZone(recorded, boundingBox, ztype == ZoneType.ROOM);
			break;
		case DEEP_WATER:
			builder.addWaterPool((ListZone) z);
			break;
		}
		if (logger != null && logger.isDebugEnabled())
			debugLog("Recording " + ztype + " zone: " + z);
		for (Coord c : recorded) {
			final Zone prev = gdata.cellToEncloser[c.x][c.y];
			if (prev != null)
				throw new IllegalStateException("Cell " + c + " (" + dungeon.map[c.x][c.y] + ") belongs to zone " + prev
						+ " already. Cannot map it to " + recorded);
			gdata.cellToEncloser[c.x][c.y] = recorded;
		}
		gdata.recordRoomOrdering(recorded);
		return recorded;
	}

	private void removeZone(GenerationData gdata, Zone z) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		builder.removeZone(z);
		for (Coord c : z) {
			gdata.cellToEncloser[c.x][c.y] = null;
		}
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

	/**
	 * @return A variant of {@code c} (or {@code c} itself) clamped to be valid in
	 *         {@code this}.
	 */
	protected final Coord clamp(Coord c) {
		final int x = Ints.clamp(0, c.x, width - 1);
		final int y = Ints.clamp(0, c.y, height - 1);
		return x == c.y && y == c.y ? c : Coord.get(x, y);
	}

	protected final void debugLog(String log) {
		if (logger != null)
			logger.debugLog(Tags.GENERATION, log);
	}

	/**
	 * You should avoid calling this method too much if {@code logger} is null or if
	 * info isn't enabled, because building {@code log} can be costly if it's not a
	 * constant.
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

	private static boolean needCaching(Zone z, ZoneType ztype) {
		if (!ztype.needsCaching())
			return false;
		if (z instanceof Rectangle)
			// (NO-BBOX)
			return false;
		else if (z instanceof SingleCellZone)
			// (NO-BBOX)
			return false;
		else
			return true;
	}

	/**
	 * @author smelC
	 */
	protected static class FloodFillObjective implements IFloodObjective {

		/** The underlying dungeon */
		protected final Dungeon dungeon;
		/**
		 * true if water is generated before rooms. In this case, no need to look for
		 * nearby walkable cells.
		 */
		protected final boolean startWithWater;

		/** See {@link #isMet()} for the relation between all these fields */

		protected Collection<Coord> dones;
		/** The minimum size that the floodfill should reach */
		protected int sizeObjective;

		protected int walkableNeighbors = 0;

		public FloodFillObjective(Dungeon dungeon, boolean startWithWater) {
			this.dungeon = dungeon;
			this.startWithWater = startWithWater;
		}

		protected void prepare(int szObjective) {
			if (dones != null)
				dones.clear();
			this.sizeObjective = szObjective;
			walkableNeighbors = 0;
		}

		@Override
		public void record(Coord c) {
			if (dones == null)
				dones = new HashSet<Coord>();
			dones.add(c);
			if (!startWithWater && isCardinallyAdjacentToWalkable(c))
				walkableNeighbors++;
		}

		@Override
		public boolean isMet() {
			final int sz = dones == null ? 0 : dones.size();
			if (sz < sizeObjective)
				/* Size objective isn't met */
				return false;
			if (!startWithWater && walkableNeighbors < 3)
				/* It must be connected to a walkable area by at least */
				return false;
			return true;
		}

		private boolean isCardinallyAdjacentToWalkable(Coord c) {
			for (Direction dir : Direction.CARDINALS) {
				final Coord neighbor = c.translate(dir);
				final DungeonSymbol sym = Arrays.getIfValid(dungeon.map, neighbor.x, neighbor.y);
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
					return true;
				}
				throw Exceptions.newUnmatchedISE(sym);
			}
			return false;
		}

	}

	/**
	 * Data carried on during generation of a single dungeon.
	 * 
	 * @author smelC
	 */
	static class GenerationData implements ICellToZone {

		protected final Dungeon dungeon;
		/**
		 * An array that keeps track of the zone to which a cell belongs. A cell belongs
		 * to at most one zone, because all zones are exclusive. All zones in this array
		 * belong to {@link #dungeon}.
		 */
		protected final Zone[][] cellToEncloser;
		/**
		 * A map that keep tracks in the order in which {@link Dungeon#rooms} and
		 * {@link Dungeon#corridors} have been generated, hereby providing an ordering
		 * on rooms.
		 */
		protected final Map<Zone, Integer> zOrder = new HashMap<Zone, Integer>();

		/** A buffer of size {@link Dungeon#width} and{@link Dungeon#height} */
		private boolean buf[][];

		private /* @Nullable */ Set<Coord> waterFillStartCandidates;

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
			assert Dungeons.hasZone(dungeon, z);
			final Integer prev = this.zOrder.put(z, nextRoomIndex);
			nextRoomIndex++;
			if (prev != null)
				throw new IllegalStateException("Zone " + z + " is being recorded twice");
		}

		/**
		 * Note that this method is designed to return {@code true} when {@code from} or
		 * {@code to} is a wall accessible from a floor. That's because this method is
		 * used to check stairs-accessibility.
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

		protected List<Zone> zonesConnectedTo(boolean considerRooms, boolean considerCorridors, List<Coord> starts) {
			final List<Zone> result = new ArrayList<Zone>(zonesConnectedTo(starts));
			if (!considerRooms || !considerCorridors) {
				final Iterator<Zone> it = result.iterator();
				while (it.hasNext()) {
					final Zone next = it.next();
					assert Dungeons.hasRoomOrCorridor(dungeon, next);
					final boolean room = Dungeons.isRoom(dungeon, next);
					if (room) {
						if (!considerRooms)
							it.remove();
					} else {
						/* A corridor */
						if (!considerCorridors)
							it.remove();
					}
				}
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
				List<Coord> starts) {
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

		@SuppressWarnings("unused")
		protected Zone findZoneAdjacentToStairCandidate(Coord candidate) {
			/*
			 * This works because stair candidates should be cardinally adjacent to at most
			 * one zone.
			 */
			Zone result = null;
			for (Direction dir : Direction.CARDINALS) {
				final Coord neighbor = candidate.translate(dir);
				final Zone z = findZoneContaining(neighbor.x, neighbor.y);
				if (z != null) {
					if (result != null)
						assert false;
					result = z;
				}
			}
			return result;
		}

		protected Zone findZoneContaining(int x, int y) {
			if (cellToEncloser != null)
				return cellToEncloser[x][y];
			return Dungeons.findZoneContaining(dungeon, x, y);
		}

		protected void addWaterFillStartCandidate(Coord c) {
			if (waterFillStartCandidates == null)
				waterFillStartCandidates = new LinkedHashSet<Coord>();
			waterFillStartCandidates.add(c);
		}

		/** @return A mutable set if not empty. */
		protected Set<Coord> getWaterFillStartCandidates() {
			if (waterFillStartCandidates == null)
				return Collections.emptySet();
			else
				return waterFillStartCandidates;
		}

		protected void removeWaterFillStartCandidates() {
			waterFillStartCandidates = null;
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
			logger.infoLog(tag,
					"Generated " + width + "x" + height + " dungeon (" + mapSize + " cells) in " + total + "ms.");
			if (1000 < mapSize)
				logger.infoLog(tag, "That's approximately " + (int) ((1000f / mapSize) * total) + "ms per 1K cells.");
			for (Stage stage : Stage.values())
				logger.infoLog(tag, "Stage " + stage + " took " + timings.get(stage) + "ms");
		}

		private Set<Zone> zonesConnectedTo(List<Coord> starts) {
			prepareBuffer();
			final Set<Zone> result = new LinkedHashSet<Zone>(Dungeons.getNumberOfZones(dungeon) / 2);
			/* Cells in 'todo' are cells reachable from 'from' */
			final Queue<Coord> todo = new LinkedList<Coord>();
			final int nbs = starts.size();
			for (int i = 0; i < nbs; i++)
				todo.add(starts.get(i));
			final Direction[] moves = Direction.CARDINALS;
			while (!todo.isEmpty()) {
				final Coord next = todo.remove();
				if (buf[next.x][next.y])
					continue;
				assert dungeon.isValid(next);
				for (Direction dir : moves) {
					final Coord neighbor = next.translate(dir);
					final DungeonSymbol sym = dungeon.getSymbol(neighbor);
					if (sym == null)
						/* Out of bounds */
						continue;
					if (buf[neighbor.x][neighbor.y]) {
						/* Done already */
						continue;
					}
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

		@Override
		// Implementation of ICellToZone which gives the Zone containing a
		// coord.
		public /* @Nullable */ Zone get(Coord c) {
			return cellToEncloser[c.x][c.y];
		}

	}

	/**
	 * @author smelC
	 */
	static enum ZoneType {
		ROOM, CORRIDOR, DEEP_WATER;

		boolean needsCaching() {
			switch (this) {
			case CORRIDOR:
			case ROOM:
				return true;
			case DEEP_WATER:
				/* No caching, since these zones are muted */
				return false;
			}
			throw Exceptions.newUnmatchedISE(this);
		}
	}

	/**
	 * The stages of generation. Used for logging performances.
	 * 
	 * @author smelC
	 */
	private static enum Stage {
		/* In the order in which they are executed */
		INIT, WATER_START, ROOMS, PASSAGES_IN_ALMOST_ADJACENT_ROOMS, CORRIDORS, STAIRS, ENSURE_DENSITY, WATER, GRASS
	}
}
