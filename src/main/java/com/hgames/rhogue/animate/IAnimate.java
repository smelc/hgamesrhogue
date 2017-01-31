package com.hgames.rhogue.animate;

import com.hgames.rhogue.grid.Positioned;
import com.hgames.rhogue.level.ILevel;
import com.hgames.rhogue.team.Team;

/**
 * A monster, a player, something like that.
 * 
 * @author smelC
 */
public interface IAnimate extends Positioned {

	/**
	 * @return Whether this element is controlled by a player.
	 */
	public boolean isPlayer();

	/**
	 * @return Whether this animate is dead.
	 */
	public boolean isDead();

	/**
	 * @return {@code this}' team.
	 */
	/* Override if you have a stronger return type */
	public Team getTeam();

	/**
	 * @return {@code this}' current level, or null if none.
	 */
	/* Override if you have a stronger return type */
	public /* @Nullable */ ILevel<?> getLevel();

}
