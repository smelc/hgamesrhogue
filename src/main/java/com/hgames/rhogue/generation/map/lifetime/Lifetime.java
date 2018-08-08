package com.hgames.rhogue.generation.map.lifetime;

import com.hgames.rhogue.generation.map.dungeon.DungeonGenerator;
import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;

/**
 * Controls the duration of {@link IRoomGenerator}s during
 * {@link DungeonGenerator}'s run. For example, if you want your dungeon to very
 * likely contain a single special room, you should write a dedicated
 * {@link IRoomGenerator} for this room, give it a very high probability at
 * {@link DungeonGenerator#installRoomGenerator(IRoomGenerator, int, Lifetime)}
 * , and attach it the {@link OneShot} instance; which would remove this
 * generator after its first usage (which would very likely be for the first
 * room created, because its probability is very high).
 * 
 * @author smelC
 */
public interface Lifetime {

	/**
	 * Call done by {@link DungeonGenerator} when the associated
	 * {@link IRoomGenerator} got used once.
	 */
	public void recordUsage();

	/**
	 * Typically this method returns {@code true} initially, and {@code false}
	 * after some calls to {@link #recordUsage()}.
	 * 
	 * @return Whether the associated {@link IRoomGenerator} should be removed.
	 */
	public boolean shouldBeRemoved();

	/**
	 * {@link DungeonGenerator} checks that at least one of the
	 * {@link DungeonGenerator#installRoomGenerator(IRoomGenerator, int, Lifetime)
	 * installed} {@link IRoomGenerator} returns {@code true} for this method.
	 * 
	 * <p>
	 * This method should typicall return a constant value.
	 * </p>
	 * 
	 * @return Whether {@link #shouldBeRemoved()} may return {@code true} at
	 *         some point.
	 */
	public boolean mayBeRemoved();

	/**
	 * Method called by {@link DungeonGenerator} when removing the associated
	 * {@link IRoomGenerator}. Use {@code this} to modify
	 * {@link DungeonGenerator}'s configuration.
	 */
	public void removeCallback();
}
