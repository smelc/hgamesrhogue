package com.hgames.rhogue.rng;

import java.io.Serializable;
import java.util.Random;

import squidpony.squidmath.IRNG;

/**
 * A {@link IRNG} based on Java's {@link Random}. If using libgdx, prefer an
 * implementation based on libgdx's RNG (RandomXS128) (see implementation in
 * <a href="https://github.com/smelc/hgamesgdx">hgamesgdx</a>).
 * 
 * <p>
 * Methods are final, because really there's no reason to change them.
 * </p>
 * 
 * @author smelC
 */
public class DefaultRNG extends AbstractRNG {

	private final Random delegate;

	private static final long serialVersionUID = 7354326781521340284L;

	/** A fresh instance. */
	public DefaultRNG() {
		this.delegate = new Random();
	}

	/**
	 * A fresh instance that uses the given seed.
	 * 
	 * @param seed
	 */
	public DefaultRNG(long seed) {
		this.delegate = new Random(seed);
	}

	@Override
	public final boolean nextBoolean() {
		return delegate.nextBoolean();
	}

	@Override
	public final float nextFloat() {
		return delegate.nextFloat();
	}

	@Override
	public final int nextInt() {
		return delegate.nextInt();
	}

	@Override
	public final int nextInt(int bound) {
		return delegate.nextInt(bound);
	}

	@Override
	public final long nextLong() {
		return delegate.nextLong();
	}

	@Override
	public Serializable toSerializable() {
		/* Works because java.util.Random (#delegate) is Serializable */
		return this;
	}
}
