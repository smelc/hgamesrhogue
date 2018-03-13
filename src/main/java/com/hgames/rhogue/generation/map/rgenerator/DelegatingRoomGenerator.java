package com.hgames.rhogue.generation.map.rgenerator;

import java.util.EnumSet;

import com.hgames.rhogue.generation.map.DungeonGenerator;
import com.hgames.rhogue.generation.map.DungeonSymbol;
import com.hgames.rhogue.generation.map.RoomComponent;
import com.hgames.rhogue.zone.Zone;

import squidpony.squidmath.Coord;
import squidpony.squidmath.IRNG;

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

	@Override
	public EnumSet<DungeonSymbol> getAcceptedNeighbors() {
		return delegate.getAcceptedNeighbors();
	}

	@Override
	public void setAcceptedNeighbors(EnumSet<DungeonSymbol> neighbors) {
		delegate.setAcceptedNeighbors(neighbors);
	}

	/**
	 * @return The identifier given at creation time.
	 */
	public int getIdentifier() {
		return identifier;
	}

	@Override
	public boolean isAcceptingStairs() {
		return delegate.isAcceptingStairs();
	}

	@Override
	public void setAcceptsStairs(boolean val) {
		delegate.setAcceptsStairs(val);
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
	public Zone generate(IRNG rng, RoomComponent component, Coord translation, int maxWidth, int maxHeight) {
		return delegate.generate(rng, component, translation, maxWidth, maxHeight);
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
	public int getMinSideSize(boolean widthOrHeight) {
		return delegate.getMinSideSize(widthOrHeight);
	}

	@Override
	public void setMinSideSize(int val, boolean widthOrHeight) {
		delegate.setMinSideSize(val, widthOrHeight);
	}

	@Override
	public void setMinSideSizes(int val) {
		delegate.setMinSideSizes(val);
	}

	@Override
	public int getMaxSideSize(boolean widthOrHeight) {
		return delegate.getMaxSideSize(widthOrHeight);
	}

	@Override
	public void setMaxSideSize(int val, boolean widthOrHeight) {
		delegate.setMaxSideSize(val, widthOrHeight);
	}

	@Override
	public void setMaxSideSizes(int val) {
		delegate.setMaxSideSizes(val);
	}

	@Override
	public void setMinMaxSideSizes(int val, boolean widthOrHeight) {
		delegate.setMinSideSize(val, widthOrHeight);
		delegate.setMaxSideSize(val, widthOrHeight);
	}

	@Override
	public String toString() {
		return String.valueOf(identifier) + "->" + delegate.toString();
	}
}
