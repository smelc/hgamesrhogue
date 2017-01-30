package com.hgames.rhogue.animate;

import com.hgames.rhogue.grid.Positioned;
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

	/* Override if you have a stronger return type */
	public Team getTeam();

}
