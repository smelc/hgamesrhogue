package com.hgames.rhogue.game;

import java.util.List;

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
	 * @param liveOnly
	 *            Whether to filter away dead player
	 * @return The first player, or null if none.
	 */
	public /* @Nullable */ I getFirstPlayer(boolean liveOnly);

	/**
	 * This method should likely be fast (no allocations).
	 * 
	 * @return Whether it's a player's turn.
	 */
	public boolean isPlayerTurn();

	/**
	 * This method should likely be fast (no allocations).
	 * 
	 * @param liveOnly
	 *            Whether to filter away dead players.
	 * @return The current players.
	 */
	public List<I> getPlayers(boolean liveOnly);

	/**
	 * This method should likely be fast (no allocations).
	 * 
	 * @return The player whose turn it is, or {@code null} if
	 *         {@link #isPlayerTurn()} doesn't hold.
	 */
	public /* @Nullable */ I getPlayerWhoseTurnItIs();

}
