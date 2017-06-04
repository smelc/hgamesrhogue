package com.hgames.rhogue.grid;

/**
 * @author smelC
 */
public interface Positioned {

	/**
	 * @return This element's x coordinate.
	 */
	public int getX();

	/**
	 * @return This element's y coordinate.
	 */
	public int getY();

	/**
	 * Changes this element's {@code x} coordinate.
	 * 
	 * @param x
	 * @return Whether the change could be honored.
	 */
	public boolean setX(int x);

	/**
	 * Changes this element's {@code y} coordinate.
	 * 
	 * @param y
	 * @return Whether the change could be honored.
	 */
	public boolean setY(int y);
}
