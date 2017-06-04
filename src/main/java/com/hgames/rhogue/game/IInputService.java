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
		public void ignoreInput() {
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

		@Override
		public String toString() {
			return "inputLocked=" + inputLocked;
		}
	}

}
