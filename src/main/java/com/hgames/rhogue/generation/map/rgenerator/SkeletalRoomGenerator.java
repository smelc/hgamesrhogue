package com.hgames.rhogue.generation.map.rgenerator;

/**
 * @author smelC
 */
public abstract class SkeletalRoomGenerator implements IRoomGenerator {

	protected boolean requiresPerfect = false;

	@Override
	public boolean requiresPerfect() {
		return requiresPerfect;
	}

	/**
	 * Sets the value to be returned by {@link #requiresPerfect()}.
	 * 
	 * @param val
	 * @return {@code this}
	 */
	public SkeletalRoomGenerator setRequiresPerfect(boolean val) {
		this.requiresPerfect = val;
		return this;
	}

}
