package com.hgames.rhogue.generation.map.dungeon;

import static com.hgames.lib.Strings.plural;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import com.hgames.lib.collection.list.Lists;
import com.hgames.lib.log.ILogger;
import com.hgames.rhogue.Tags;
import com.hgames.rhogue.generation.map.dungeon.DungeonGenerator.GenerationData;
import com.hgames.rhogue.generation.map.dungeon.DungeonGenerator.ICorridorControl;
import com.hgames.rhogue.zone.Zone;
import com.hgames.rhogue.zone.Zones;

/**
 * Component that makes sure that the dungeon is connected enough.
 * 
 * <p>
 * Make sure that at least 1/5th of the map is accessible. For that, find
 * disconnected rooms. For every disconnected component whose size is greater
 * than {@link #getWallificationBound(Dungeon)} of the map, try very hard to
 * connect it to the stairs. At the end check if 1/6th of the map is accessible.
 * </p>
 * 
 * <p>
 * This component must be called after having generated the stairs.
 * </p>
 * 
 * @author smelC
 */
public class DensityComponent implements GeneratorComponent {

	@Override
	public boolean generate(DungeonGenerator gen, GenerationData gdata) {
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
		final ILogger logger = gen.logger;
		if (0 < nbdc) {
			if (logger != null && logger.isInfoEnabled())
				logger.infoLog(Tags.GENERATION, "Found " + nbdc + " disconnected component" + plural(nbdc));
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
				final int extension = treatDisconnectedComponent(gen, gdata, connectedRooms, disconnectedComponent);
				assert 0 <= extension;
				if (0 < extension) {
					reachable += extension;
					if (logger != null && logger.isInfoEnabled())
						logger.infoLog(Tags.GENERATION, "Connected component (consisting of " + nbdc + " zone"
								+ plural(sz) + ") of size " + extension);
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
	private int treatDisconnectedComponent(DungeonGenerator gen, GenerationData gdata, List<Zone> connectedRooms,
			List<Zone> component) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		final int sz = Zones.size(component);
		final int csz = component.size();
		final ILogger logger = gen.logger;
		if (csz == 1) {
			/* Component is a single room */
			final Zone z = component.get(0);
			/* Can it be used to honor #disconnectedRoomsObjective ? */
			if (dungeon.getDisconnectedRooms().size() < gen.disconnectedRoomsObjective) {
				if (logger != null && logger.isInfoEnabled())
					logger.infoLog(Tags.GENERATION,
							"Used a size " + sz + " disconnected room to fulfill the disconnected rooms objective.");
				builder.addDisconnectedRoom(z);
				return 0;
			}
			/* Can it be used to honor #waterIslands ? */
			if (dungeon.getWaterIslands().size() < gen.waterIslands
					&& Dungeons.isSurroundedBy(dungeon, z, EnumSet.of(DungeonSymbol.DEEP_WATER))) {
				if (logger != null && logger.isInfoEnabled())
					logger.infoLog(Tags.GENERATION,
							"Used a size " + sz + " disconnected room to fulfill the water islands objective.");
				builder.addWaterIsland(z);
				return 0;
			}
		}

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
				final boolean built = gen.generateCorridors(gdata, component, connectedRooms, new ICorridorControl.Impl(
						dungeon, false, true, true, Math.max(z.getWidth(), z.getHeight()) * 2, false));
				if (built) {
					if (logger != null && logger.isInfoEnabled())
						logger.infoLog(Tags.GENERATION, "Connected a water island of size " + csz);
					return csz;
				}
			}
		}

		final int bound = getWallificationBound(dungeon);
		if (sz < bound) {
			/* Component is small */
			/* Replace it with walls (hereby removing it) */
			wallifyAll(gen, gdata, component);
			if (logger != null && logger.isInfoEnabled())
				logger.infoLog(Tags.GENERATION, "Total of wallification: " + sz + " (bound is " + bound + " cells)");
			return 0;
		}

		final int nbCellsInComponent = Zones.size(component);
		/* To ensure 'generateCorridors' precondition that it gets only rooms */
		component.removeAll(dungeon.corridors);
		final boolean connected = gen.generateCorridors(gdata, component, connectedRooms,
				new ICorridorControl.Impl(dungeon, false, true, true, nbCellsInComponent / 2, true));
		if (connected) {
			connectedRooms.addAll(component);
			return nbCellsInComponent;
		} else {
			if (logger != null && logger.isInfoEnabled())
				logger.infoLog(Tags.GENERATION, "Could not treat a disconnected component of size " + sz);
			final int discNow = dungeon.getDisconnectedRooms().size();
			final int discObj = gen.disconnectedRoomsObjective;
			if (discNow + csz <= discObj) {
				logger.infoLog(Tags.GENERATION, "Using it to fill the disconnected rooms objective");
				for (int i = 0; i < csz; i++)
					builder.addDisconnectedRoom(component.get(i));
			} else {
				logger.infoLog(Tags.GENERATION,
						"Wallifying it, despite above the wallification bound; as it's the only option");
				wallifyAll(gen, gdata, component);
			}
			return 0;
		}
	}

	/**
	 * @return The size under which a disconnected component can be wallified
	 */
	protected int getWallificationBound(Dungeon dungeon) {
		final int width = dungeon.getWidth();
		final int height = dungeon.getHeight();
		return (width * height) / 128;
	}

	/** Turns zones into walls, hereby removing them */
	protected void wallifyAll(DungeonGenerator gen, GenerationData gdata, List<Zone> component) {
		final int csz = component.size();
		final ILogger logger = gen.logger;
		for (int i = 0; i < csz; i++) {
			final Zone z = component.get(i);
			wallify(gen, gdata, z);
			if (logger != null && logger.isInfoEnabled())
				logger.infoLog(Tags.GENERATION, "Wallified a zone of size " + z.size());
			gdata.addWaterFillStartCandidate(z.getCenter());
		}
	}

	/** Turns a zone into walls, hereby removing it */
	protected final void wallify(DungeonGenerator gen, GenerationData gdata, Zone z) {
		final Dungeon dungeon = gdata.dungeon;
		final DungeonBuilder builder = dungeon.getBuilder();
		assert Dungeons.hasZone(dungeon, z);
		gen.removeRoomOrCorridor(gdata, z);
		builder.setSymbols(z.iterator(), DungeonSymbol.WALL);
		gen.draw(dungeon);
	}

}
