package com.hgames.rhogue.game;

import com.hgames.rhogue.animate.IAnimate;

/**
 * Service giving information about players.
 * 
 * @author smelC
 * @param <I>
 *            The concrete instance of {@link IAnimate} used in the game.
 */
public interface IPlayerService<I extends IAnimate> {

	/**
	 * @return Whether it's a player's turn.
	 */
	public boolean isPlayerTurn();

	/**
	 * @return The player whose turn it is, or {@code null} if
	 *         {@link #isPlayerTurn()} doesn't hold.
	 */
	public /* @Nullable */ I getPlayerWhoseTurnItIs();

}
