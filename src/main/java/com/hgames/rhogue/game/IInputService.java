package com.hgames.rhogue.game;

/**
 * @author smelC
 */
public interface IInputService {

	/**
	 * @param val
	 *            true to start ignoring input, false to consider it.
	 */
	public void setInputIgnoring(boolean val);

	/** A convenience method that does {@code setInputIgnoring(true)}. */
	public void ignoreInput();

	/** A convenience method that does {@code setInputIgnoring(false)}. */
	public void considerInput();

	/**
	 * @param val
	 *            The value to which to disjunct the value of input ignoring.
	 */
	public void orInputIgnoring(boolean val);

	/**
	 * @return Whether input is being ignored now.
	 */
	public boolean isInputIgnored();

	/**
	 * @author smelC
	 */
	public class Skeleton implements IInputService {

		protected boolean inputLocked = false;

		@Override
		public final void setInputIgnoring(boolean val) {
			inputLocked = val;
		}

		@Override
		public final void orInputIgnoring(boolean val) {
			inputLocked |= val;
		}

		@Override
		public final void ignoreInput() {
			setInputIgnoring(true);
		}

		@Override
		public final void considerInput() {
			setInputIgnoring(false);
		}

		@Override
		public final boolean isInputIgnored() {
			return inputLocked;
		}
	}

}
