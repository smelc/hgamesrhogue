package com.hgames.rhogue.generation.map.lifetime;

import com.hgames.rhogue.generation.map.IRoomGenerator;

/**
 * An implementation of {@link Lifetime} that will unplug the associated
 * {@link IRoomGenerator} after the given number of callbacks to
 * {@link #recordUsage()}.
 * 
 * @author smelC
 */
public class SomeShots implements Lifetime {

	private int remainingShots;

	/**
	 * @param nbShots
	 *            The number of time that the associated {@link IRoomGenerator}
	 *            can be used. Must be >= 0.
	 * @throws IllegalStateException
	 *             If {@code nbShots < 0}.
	 */
	public SomeShots(int nbShots) {
		if (nbShots < 0)
			throw new IllegalStateException("nbShots should be > 0. Received: " + nbShots);
		this.remainingShots = nbShots;
	}

	@Override
	public void recordUsage() {
		remainingShots--;
	}

	@Override
	public boolean shouldBeRemoved() {
		return remainingShots < 1;
	}

	@Override
	public boolean mayBeRemoved() {
		return true;
	}

	@Override
	/* Subclassers may override */
	public void removeCallback() {
		/* Nothing done */
	}

}
