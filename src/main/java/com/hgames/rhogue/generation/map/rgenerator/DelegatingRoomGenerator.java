package com.hgames.rhogue.generation.map.rgenerator;

import com.hgames.rhogue.generation.map.DungeonGenerator;
import com.hgames.rhogue.generation.map.RoomComponent;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.RNG;

/**
 * A generator that delegates to another generator and has an identifier. The
 * identifier is used in Dungeon Mercenary, so that quests implementors can find
 * their rooms back, after having configured a {@link DungeonGenerator}.
 * 
 * @author smelC
 */
public class DelegatingRoomGenerator implements IRoomGenerator {

	protected final IRoomGenerator delegate;
	protected final int identifier;

	/**
	 * @param delegate
	 * @param identifier
	 *            An identifier for {@code this}.
	 */
	public DelegatingRoomGenerator(IRoomGenerator delegate, int identifier) {
		this.delegate = delegate;
		this.identifier = identifier;
	}

	/**
	 * @return The identifier given at creation time.
	 */
	public int getIdentifier() {
		return identifier;
	}

	@Override
	public void setRequiresPerfect(boolean val) {
		delegate.setRequiresPerfect(val);
	}

	@Override
	public void setMaxConnections(int val) {
		delegate.setMaxConnections(val);
	}

	@Override
	public void setForceDoors(boolean val) {
		delegate.setForceDoors(val);
	}

	@Override
	public Zone generate(RNG rng, RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		return delegate.generate(rng, component, translation, maxWidth, maxHeight);
	}

	@Override
	public boolean requiresPerfect() {
		return delegate.requiresPerfect();
	}

	@Override
	public int getMaxConnections() {
		return delegate.getMaxConnections();
	}

	@Override
	public boolean getForceDoors() {
		return delegate.getForceDoors();
	}

	@Override
	public String toString() {
		return String.valueOf(identifier) + "->" + delegate.toString();
	}
}
