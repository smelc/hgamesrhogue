package com.hgames.rhogue.generation.map.dungeon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
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
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.draw.ConsoleDungeonDrawer;
import com.hgames.rhogue.generation.map.draw.IDungeonDrawer;
import com.hgames.rhogue.generation.map.dungeon.corridor.CorridorBuilders;
import com.hgames.rhogue.generation.map.dungeon.corridor.ICorridorBuilder;
import com.hgames.rhogue.generation.map.dungeon.flood.IFloodObjective;
import com.hgames.rhogue.generation.map.dungeon.stair.SkeletalStairGenerator;
import com.hgames.rhogue.generation.map.lifetime.Lifetime;
import com.hgames.rhogue.generation.map.lifetime.SomeShots;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;
import com.hgames.rhogue.rng.ProbabilityTable;
import com.hgames.rhogue.zone.CachingZone;
import com.hgames.rhogue.zone.ListZone;
import com.hgames.rhogue.zone.Rectangle;
import com.hgames.rhogue.zone.SingleCellZone;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidgrid.Direction;
import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

/**
 * The dungeon generator I use in <a href="http://schplaf.org/hgames">Dungeon
 * Mercenary</a>. It builts a structured dungeon. This means you have accesses
 * to objects describing rooms, corridors, zones, etc.
 * 
 * This class' public API can be divided into two:
 * 
 * <ul>
 * <li>The "configure" part of the API which consists of all {@code set*}
 * methods.</li>
 * <li>The "generate" part of the API which is {@link #generate()}.</li>
 * </ul>
 * 
 * <p>
 * This class' protected API is designed to be called from
 * {@link GeneratorComponent components}.
 * </p>
 * 
 * <p>
 * Overall the API of this file has been Intentionally kept as little as
 * possible, trying to divide the generator into understandable and decoupled
 * pieces. Please maintain this spirit.
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
 * @see GeneratorComponent
 */
public class DungeonGenerator {

	protected final IRNG rng;
	protected final int width;
	protected final int height;

	protected /* @Nullable */ IDungeonDrawer drawer;
	protected /* @Nullable */ ILogger logger;
	protected /* @Nullable */ IDungeonGeneratorListener listener = new DungeonGeneratorListener();

	/** All elements of this table are in {@link #rgLifetimes} too */
	protected final ProbabilityTable<IRoomGenerator> roomGenerators;
	/** A map that keep tracks of rooms' generators */
	protected final Map<Zone, IRoomGenerator> roomToGenerator;
	/** The domain of this map is {@link #roomGenerators} */
	protected final Map<IRoomGenerator, Lifetime> rgLifetimes;

	protected int minRoomWidth = 3;
	protected int maxRoomWidth;
	protected int minRoomHeight = 3;
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

	/** Whether shallow water can be generated */
	protected boolean allowShallowWater = true;

	/** Whether to do water before rooms. This makes water more central. */
	// XXX false is not yet implemented
	protected boolean startWithWater = true;

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

	/** See {@link SkeletalStairGenerator} */
	protected /* @Nullable */ Direction stairUpValidDirection;
	/** See {@link SkeletalStairGenerator} */
	protected /* @Nullable */ Direction stairDownValidDirection;

	/** See {@link Complexity} */
	protected Complexity complexity = Complexity.NORMAL;

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
	public DungeonGenerator(IRNG rng, int width, int height) {
		if (width < 0)
			throw new IllegalStateException("Invalid width for dungeon generator: " + width);
		if (height < 0)
			throw new IllegalStateException("Invalid height for dungeon generator: " + height);
		this.rng = rng;
		this.width = width;
		this.height = height;
		this.roomGenerators = ProbabilityTable.create();
		this.rgLifetimes = new HashMap<IRoomGenerator, Lifetime>();
		this.roomToGenerator = new HashMap<Zone, IRoomGenerator>();
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
	 * This method is designed to plug a game-specific listener that will keep track
	 * of extra information.
	 * 
	 * @param listener
	 *            The listener to use, or null
	 */
	public void setListener(IDungeonGeneratorListener listener) {
		this.listener = listener;
	}

	/**
	 * @param logger
	 *            The logger to use, or null to turn logging OFF.
	 */
	public void setLogger(/* @Nullable */ ILogger logger) {
		this.logger = logger;
	}

	/**
	 * @param value
	 *            Whether to generate shallow water
	 */
	public void setAllowShallowWater(boolean value) {
		this.allowShallowWater = value;
	}

	/**
	 * @param value
	 *            Whether to allow rooms of width or height one (which allow
	 *            dungeons like this: <a href=
	 *            "https://twitter.com/hgamesdev/status/899609612554559489"> image
	 *            </a>). False by default.
	 */
	public void setAllowWidthOrHeightOneRooms(boolean value) {
		this.allowWidthOrHeightOneRooms = value;
	}

	/**
	 * The complexity controls the maximum width/height of rooms. More complex
	 * dungeons (many rooms, many corridors) use a smaller maximum size.
	 * 
	 * @author smelC
	 */
	public static enum Complexity {
		/**
		 * The complexity to have simple dungeons with a few big rooms. Fits for a
		 * tutorial level
		 */
		BABY,
		/** Complexity between {@link #BABY} and {@link #NORMAL} */
		KID,
		/** The usual complexity, that will classical dungeons */
		NORMAL,
		/** Complexity between {@link #NORMAL} and {@link #WIZARD} */
		COMPLEX,
		/**
		 * Circonvoluted dungeons, with many small rooms and complex corridors'
		 * connections
		 */
		WIZARD;
	}

	/**
	 * @param complexity
	 *            Set the complexity of this dungeon.
	 */
	public void setComplexity(Complexity complexity) {
		this.complexity = complexity;
	}

	/**
	 * @param proba
	 *            An int in [0, 100]
	 * @throws IllegalStateException
	 *             If {@code proba} isn't in [0, 100].
	 */
	public void setDoorProbability(int proba) {
		if (!Ints.inInterval(0, proba, 100))
			throw new IllegalStateException("Excepted a value in [0, 100]. Received: " + proba);
		this.doorProbability = proba;
	}

	/**
	 * @param percent
	 *            The percentage of walkable cells to turn into grass. If negative,
	 *            unchanged; otherwise must be in [0, 100].
	 * @param nbPools
	 *            The number of pools of grasses to generate or something negative
	 *            not to change anything.
	 * @throws IllegalStateException
	 *             If {@code 100 < percent}
	 */
	public void setGrassObjectives(int percent, int nbPools) {
		if (100 < percent)
			throw new IllegalStateException(
					"Percentage of grass must be negative or in [0, 100]. Received: " + percent);
		if (0 <= percent)
			this.grassPercentage = percent;
		if (0 <= nbPools)
			this.grassPatches = nbPools;
	}

	/**
	 * @param minWidth
	 *            The minimum width of rooms. The default is 3.
	 * @param minHeight
	 *            The minimum width of rooms. The default is 3.
	 */
	public void setMinRoomSizes(int minWidth, int minHeight) {
		this.minRoomWidth = minWidth;
		this.minRoomHeight = minHeight;
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
	 */
	public void setStairsObjectives(/* @Nullable */Coord upStair, /* @Nullable */Coord downStair) {
		this.upStairObjective = upStair == null ? null : clamp(upStair);
		this.downStairObjective = downStair == null ? null : clamp(downStair);
	}

	/**
	 * @param dir
	 *            The direction in which the stair must be in its enclosing room or
	 *            null for no constraint (the default)
	 * @param upOrDown
	 *            Whether the stair up or stair down is being concerned
	 */
	public void setStairValidDirection(Direction dir, boolean upOrDown) {
		if (upOrDown)
			stairUpValidDirection = dir;
		else
			stairDownValidDirection = dir;
	}

	/**
	 * @param objective
	 *            The objective. Must be {@code >= 0}.
	 * @throws IllegalStateException
	 *             If {@code objective < 0}.
	 */
	public void setDisconnectedRoomsObjective(int objective) {
		if (objective < 0)
			throw new IllegalStateException("Disconnected rooms objective must be >= 0. Received: " + objective);
		this.disconnectedRoomsObjective = objective;
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
	 * @throws IllegalStateException
	 *             If {@code percent} is greater than 100.
	 */
	public void setWaterObjective(boolean startWithWater, int percent, int pools, int islands) {
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
	 */
	public void installRoomGenerator(IRoomGenerator roomGenerator, int probability, Lifetime lifetime) {
		this.roomGenerators.add(roomGenerator, probability);
		this.rgLifetimes.put(roomGenerator, lifetime);
	}

	/** @return A fresh dungeon or null if it could not be generated. */
	public Dungeon generate() {
		if (roomGenerators.isEmpty()) {
			final String msg = "You need to install at least one room generator (using method installRoomGenerator). Cannot generate dungeons.";
			if (logger != null && logger.isErrEnabled())
				logger.infoLog(Tags.GENERATION, msg);
			else
				/* We usually don't do that, but it's friendly for newcomers */
				System.out.println(msg);
			return null;
		}
		roomToGenerator.clear();
		computeMaxRoomSizes();
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
		final GenerationData gdata = new GenerationData(this, dungeon, watch);

		if (startWithWater)
			doStage(Stage.WATER_START, new WaterComponent(), gdata);

		doStage(Stage.ROOMS, new RoomComponent(this, gdata), gdata);

		doStage(Stage.PASSAGES_IN_ALMOST_ADJACENT_ROOMS, new PassagesComponent(), gdata);
		// Done once here instead of at every passage
		draw(gdata.dungeon);

		gdata.startStage(Stage.CORRIDORS);
		generateCorridors(gdata, dungeon.rooms, dungeon.rooms,
				new ICorridorControl.Impl(dungeon, true, false, false, false));

		gdata.startStage(Stage.STAIRS);
		/* Must be called before 'generateWater' */
		final boolean good = doStage(Stage.STAIRS, new StairsComponent(), gdata);
		if (!good) {
			if (logger != null && logger.isDebugEnabled())
				logger.infoLog(Tags.GENERATION, dungeon.dirtyPrint("\n"));
			return null;
		}

		doStage(Stage.ENSURE_DENSITY, new DensityComponent(), gdata);

		if (!startWithWater)
			doStage(Stage.WATER, new WaterComponent(), gdata);

		if (!doStage(Stage.GRASS, new GrassComponent(), gdata))
			return null;

		if (logger != null) {
			for (IRoomGenerator generator : rgLifetimes.keySet()) {
				final Lifetime lifetime = rgLifetimes.get(generator);
				if (lifetime == null) {
					assert false;
					continue;
				}
				if (lifetime instanceof SomeShots) {
					final SomeShots shots = (SomeShots) lifetime;
					final int remainingShots = shots.getRemainingShots();
					if (0 < remainingShots) {
						logger.warnLog(Tags.GENERATION, "Some room generating objective could not be honored.");
						logger.warnLog(Tags.GENERATION, remainingShots + " shot" + (remainingShots == 1 ? "" : "s")
								+ " remain" + (remainingShots == 1 ? "s" : "") + " for generator: " + generator);
					}
				}
			}
		}
		assert dungeon.invariant();
		gdata.startStage(null); // Record end of last stage
		gdata.logTimings(logger);
		return dungeon;
	}

	/**
	 * This method should only be called after {@link #generate()}.
	 * 
	 * @param room
	 * @return The generator that generated {@code zone} or null if not found
	 *         (should not happen if {@code room} is indeed a room obtained with
	 *         {@link Dungeon#getRooms()} in a dungeon generated by {@code this}).
	 * 
	 *         <p>
	 *         Receiving null on corridors is normal.
	 *         </p>
	 */
	public /* @Nullable */ IRoomGenerator getRoomGenerator(Zone room) {
		return roomToGenerator.get(room);
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
		if (dungeon.waterPools == null || dungeon.waterPools.isEmpty())
			/* Nothing to do */
			return;
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

	/**
	 * @author smelC
	 */
	public static enum ZoneType {
		/** The type of rooms */
		ROOM,
		/** The type associated to chasms */
		CHASM,
		/** The type associated to corridors */
		CORRIDOR,
		/** The type associated to deep water */
		DEEP_WATER;

		boolean needsCaching() {
			switch (this) {
			case CHASM:
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

	protected IRoomGenerator getRoomGenerator(Dungeon dungeon, Zone room) {
		assert dungeon.rooms.contains(room);
		final IRoomGenerator rg = roomToGenerator.get(room);
		assert rg != null : IRoomGenerator.class.getSimpleName() + " for zone " + room + " is missing";
		return rg;
	}

	/**
	 * @param gdata
	 * @param z
	 *            An instance of {@link ListZone} if ztype is
	 *            {@link ZoneType#DEEP_WATER}.
	 * @param boundingBox
	 *            {@code z}'s bounding box. Not required if a {@link ZoneType#CHASM}
	 *            or {@link ZoneType#DEEP_WATER}.
	 * @param rg
	 *            The generator that generated {@code z}, if any.
	 * @param ztype
	 * @return The zone added (might be a cache over {@code z}).
	 */
	protected Zone addZone(GenerationData gdata, Zone z, /* @Nullable */ Rectangle boundingBox,
			/* @Nullable */ IRoomGenerator rg, ZoneType ztype) {
		assert z != null;
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		final Zone recorded = needCaching(z, ztype) ? new CachingZone(z) : z;
		switch (ztype) {
		case CHASM:
			builder.addChasm(recorded);
			break;
		case ROOM:
			if (rg == null) {
				assert false;
				if (logger != null && logger.isErrEnabled())
					logger.errLog(Tags.GENERATION,
							"No " + IRoomGenerator.class.getSimpleName() + " given when adding a " + ztype
									+ " zone. This is a bug. Continuing but hoping for the best...");
			}
			//$FALL-THROUGH$
		case CORRIDOR:
			builder.addZone(recorded, boundingBox, ztype == ZoneType.ROOM);
			break;
		case DEEP_WATER:
			builder.addWaterPool((ListZone) z);
			break;
		}
		if (rg != null) {
			assert !roomToGenerator.containsKey(recorded);
			roomToGenerator.put(recorded, rg);
		}
		if (logger != null && logger.isDebugEnabled())
			logger.debugLog(Tags.GENERATION,
					"Recording " + ztype + " zone: " + z + (rg == null ? "" : " with generator: " + rg));
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

	protected void removeRoomOrCorridor(GenerationData gdata, Zone z) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		final boolean room = dungeon.getRooms().contains(z);
		final IRoomGenerator rg = room ? getRoomGenerator(gdata.dungeon, z) : null;
		builder.removeRoomOrCorridor(z);
		for (Coord c : z) {
			gdata.cellToEncloser[c.x][c.y] = null;
		}
		if (room) {
			if (listener != null)
				listener.removedRoom(dungeon, rg, z);
			final boolean rmed = roomToGenerator.remove(z) != null;
			assert rmed;
		}
	}

	protected void draw(Dungeon dungeon) {
		if (drawer != null) {
			drawer.draw(dungeon.getMap());
		}
	}

	protected void debugDraw(Dungeon dungeon) {
		new ConsoleDungeonDrawer(new DungeonSymbolDrawer()).draw(dungeon.map);
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

	private void computeMaxRoomSizes() {
		int div = 5;
		switch (complexity) {
		case BABY:
			div -= 3;
			break;
		case KID:
			div -= 2;
			break;
		case NORMAL:
			div -= 1;
			break;
		case COMPLEX:
			break;
		case WIZARD:
			div += 2;
			break;
		}
		this.maxRoomWidth = Math.max(2, width / div);
		this.maxRoomHeight = Math.max(2, height / div);
	}

	private boolean doStage(Stage stage, GeneratorComponent component, GenerationData gdata) {
		gdata.startStage(stage);
		return component.generate(this, gdata);
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
	protected static class GenerationData implements ICellToZone {

		@Deprecated
		protected final DungeonGenerator dgen;
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

		protected GenerationData(DungeonGenerator dgen, Dungeon dungeon, /* @Nullable */ Stopwatch watch) {
			this.dgen = dgen;
			this.dungeon = dungeon;
			this.cellToEncloser = new Zone[dungeon.width][dungeon.height];
			this.timings = new EnumMap<Stage, Long>(Stage.class);
			this.timings.put(Stage.INIT, Long.valueOf(-1l));
			this.watch = watch;
		}

		protected void startStage(/* @Nullable */ Stage next) {
			assert invariant();

			if (watch == null)
				return;
			Stage current = null;
			for (Stage s : Stage.values()) {
				final Long duration = timings.get(s);
				if (duration != null && duration.longValue() == -1l) {
					current = s;
					break;
				}
			}
			if (current == null)
				throw new IllegalStateException("Stage not found: " + current);
			timings.put(current, Long.valueOf(watch.getDuration()));
			if (next != null) {
				watch.reset();
				timings.put(next, Long.valueOf(-1l));
			}
		}

		protected void recordRoomOrdering(Zone z) {
			assert Dungeons.hasZone(dungeon, z);
			final Integer prev = this.zOrder.put(z, Integer.valueOf(nextRoomIndex));
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

		protected Zone findZoneContaining(int x, int y) {
			if (cellToEncloser != null)
				return cellToEncloser[x][y];
			return Dungeons.findRoomOrCorridorContaining(dungeon, x, y);
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
				final Long duration = timings.get(stage);
				if (duration == null)
					/* Stage wasn't done. It's okay. */
					continue;
				if (duration.intValue() < 0)
					logger.warnLog(tag, "Duration of stage " + stage + " is unexpectedly " + duration);
				total += timings.get(stage).longValue();
			}
			final int width = dungeon.width;
			final int height = dungeon.height;
			final int mapSize = width * height;
			logger.infoLog(tag,
					"Generated " + width + "x" + height + " dungeon (" + mapSize + " cells) in " + total + "ms.");
			if (1000 < mapSize)
				logger.infoLog(tag, "That's approximately " + (int) ((1000f / mapSize) * total) + "ms per 1K cells.");
			for (Stage stage : Stage.values()) {
				final Long duration = timings.get(stage);
				if (duration != null)
					logger.infoLog(tag, "Stage " + stage + " took " + duration + "ms");
			}
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

		private boolean invariant() {
			for (int x = 0; x < cellToEncloser.length; x++) {
				final Zone[] ys = cellToEncloser[x];
				for (int y = 0; y < ys.length; y++) {
					final Zone zone = cellToEncloser[x][y];
					if (zone != null && !(Dungeons.hasRoomOrCorridor(dungeon, zone) || Dungeons.hasChasm(dungeon, zone)
							|| Dungeons.hasDisconnectedRoom(dungeon, zone) || Dungeons.hasWaterPool(dungeon, zone))) {
						assert false;
						return false;
					}
				}
			}
			return true;
		}

		@Override
		// Implementation of ICellToZone which gives the Zone containing a
		// coord.
		public /* @Nullable */ Zone get(Coord c) {
			return cellToEncloser[c.x][c.y];
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
