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
package com.calefay.exodusDefence.weapons;


import com.calefay.exodusDefence.EntityRegister;
import com.calefay.exodusDefence.MissileTrackingModule;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameWorldInfo;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

public class EDSeekingMissile extends MissileProjectile implements GameEntity {
	
	private float hitPoints = 0;
	private float trackingActivation = 0;
	private MissileTrackingModule trackingModule;
	private GameEntity target;	
	
	private String faction = "default";
	private String onDestroyedEventType = null;
	
	@Deprecated
	public EDSeekingMissile(String newName, Vector3f pos, Quaternion orientation, 
			GameEntity targ, MissileTrackingModule tm,
			float speed, float acceleration, float maxSpeed, float lifeTime) {
		super(newName, pos, orientation, speed, acceleration, maxSpeed, lifeTime, 25.0f, true);	// FIXME: Deprecated - hardcoded damage
		
		initializeMissile(targ, tm);
	}

	/* 
	 * @param trackingActivation - Time delay before the missile starts to track it's target (eg. fly straight to clear the launcher)*/
	public EDSeekingMissile(String newName, 
			GameEntity targ, MissileTrackingModule tm,
			float speed, float acceleration, float maxSpeed, float lifeTime, float damage, boolean launch, float trackingDelay) {
		super(newName, speed, acceleration, maxSpeed, lifeTime, damage, launch);
		initializeMissile(targ, tm);	// FIXME: No reason to set the target until the missile is fired.
		trackingActivation = trackingDelay;
	}
	
	private void initializeMissile(GameEntity targ, MissileTrackingModule tm) {
		hitPoints = 1;	// FIXME: No harcoded hitpoints
		target = targ;
		trackingModule = tm;
		trackingActivation = 0;
		projectileNode.updateModelBound();
		
		onDestroyedEventType = "MissileDestroyed";
	}
	
	public boolean handleAction(GameEvent action) {
		// TODO: replace the hardcoded string with an appropriate constant.
		if(action.getEventType() == "damage") {
			// TODO: This should be in a try..catch with error handling.
			float damageAmount = (Float)action.getEventTarget();
			hitPoints -= damageAmount;
			if(hitPoints <= 0) {
				hitPoints = 0;
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(onDestroyedEventType,"default", this, null);
				deactivate();
			}
			return true;
			} else {return false;}
		}
	
	public void deactivate() {
		trackingModule = null;
		super.deactivate();
	}
	
	public void update(float interpolation) {
		if(active) {
			if( launched && (trackingModule != null) && (target != null) && !target.isDead()) {
				if(trackingActivation > 0) {	// TODO: Probably best to put activation timer in the tracking module eventually.
					trackingActivation -= interpolation;
					if(trackingActivation < 0) trackingActivation = 0;
				} else {
					trackingModule.trackTarget(this, target);
				}
			}
			super.update(interpolation);
		}
	}
	
	@Override
	public void launch() {
		super.launch();
		armingDelay = 0.1f;	// FIXME: Hardcoded missile arming delay. 
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("EDMissile",  "audio", this, null);	// TODO: Should have a better system than triggering audio events directly from each weapon.
	}
	
	public void launch(GameEntity target) {
		this.target = target;
		launch();
		EntityRegister.getEntityRegister().deRegisterGeometryEntity(getProjectileNode());
		EntityRegister.getEntityRegister().registerGeometryEntity(getProjectileNode(), this);
	}
	
	public void setDestroyedEventType(String type) {onDestroyedEventType = type;}
	
	// GameEntity implemented methods begin:
	public Vector3f getPosition() {return getWorldTranslation();}
	public Quaternion getRotation() {return projectileNode.getWorldRotation();}
	public Vector3f getVelocity() {return getProjectileVelocity();}
	public boolean isDead() {return !isActive();}
	public String getFaction() {return faction;}
	public void setFaction(String faction) {this.faction = faction;}
	// GameEntity implemented methods end 
	
	public void cleanup() {
		EntityRegister.getEntityRegister().deRegisterGeometryEntity(getProjectileNode());
		
		super.cleanup();
		
		target = null;
		trackingModule = null;
	}
	
}
