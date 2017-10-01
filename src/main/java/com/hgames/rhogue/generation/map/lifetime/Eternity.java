package com.hgames.rhogue.generation.map.lifetime;

import com.hgames.rhogue.generation.map.rgenerator.IRoomGenerator;

/**
 * An instance of {@link Lifetime} that lasts forever. Typically,
 * you should attach it to a {@link IRoomGenerator} that you wanna use as the
 * default when special rooms have been created.
 * 
 * @author smelC
 */
public class Eternity implements Lifetime {

	/** Some instance */
	public static final Lifetime INSTANCE = new Eternity();

	@Override
	public void recordUsage() {
		/* Nothing done */
	}

	@Override
	public final boolean shouldBeRemoved() {
		return false;
	}

	@Override
	public final boolean mayBeRemoved() {
		return false;
	}

	@Override
	/* Subclassers may override */
	public void removeCallback() {
		/* Nothing done */
	}

}
