package com.hgames.rhogue.game;

import java.io.Serializable;

import com.hgames.rhogue.animate.IAnimate;
import com.hgames.rhogue.level.ILevel;

/**
 * The game instance. It ties together levels and players. It is split into
 * different services, to avoid having a very large number of methods in this
 * interface.
 * 
 * @author smelC
 * 
 * @param <L>
 *            The concrete type of levels.
 * @param <I>
 *            The concrete instance of {@link IAnimate} used in the game.
 */
public interface IGame<L extends ILevel<?>, I extends IAnimate> {

	/**
	 * @return The instance of player service. Typically created once and then
	 *         stored.
	 */
	public IPlayerService<I> getPlayerService();

	/**
	 * @return The instance of level service. Typically created once and then
	 *         stored.
	 */
	public ILevelService<L> getLevelService();

	/**
	 * @return The instance of {@link IInputService}. Typically created once and
	 *         then stored.
	 */
	public IInputService getInputService();

	/**
	 * @return The instance of message service. Typically created once and then
	 *         stored.
	 */
	public IMessageService getMessageService();

	/**
	 * @return The save of the current game.
	 */
	public Serializable toSave();

}
