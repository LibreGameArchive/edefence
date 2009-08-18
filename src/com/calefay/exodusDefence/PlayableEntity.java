/*
    Exodus Defence
    contact@calefay.com
    Copyright (C) 2009 James Waddington

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.calefay.exodusDefence;


import com.calefay.exodusDefence.radar.TargetAcquirer;
import com.calefay.exodusDefence.weapons.Gun;
import com.calefay.utils.AttributeSet;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.RemoveableEntityManager;
import com.jme.scene.Node;


/** Consider getting rid of this altogether and segmenting the main game logic.
 *  Probably, each major type of playable entity should be handle separately (things like controls, HUD, upgrades)
 *  rather than have their function constrained by a common interface.*/
public interface PlayableEntity extends GameEntity {

	public enum ControlsType {TURRET, FIXEDWING, HELICOPTER}
	
	public void repair(float hitpoints);
	
	public void upgradeDouble();	// Get rid of this. Perhaps an arbitrary upgrade method instead.
	
	public void stripUpgrades();
	public void setPrimaryGun(	AttributeSet barrel1Template, AttributeSet barrel2Template,
			GameResourcePack gameResources, RemoveableEntityManager entityManager);
	public void setSecondaryGun(AttributeSet template, GameResourcePack gameResources, RemoveableEntityManager entityManager);
	
	/* Signals if the entity is still in use. If false then the object should be considered invalid awaiting garbage collection, and references to it should be cleared.*/
	public boolean isActive();
	
	public String getName();
	public Node getCameraTrackNode();
	public Gun getPrimaryGunBarrel1();
	public Gun getPrimaryGunBarrel2();
	public Gun getSecondaryGun();
	public float getHitpoints();
	public float getMaxHitpoints();
	public TargetAcquirer getRadar();
	public ControlsType getControlsType();
	
	public void cleanup();
}
