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
package com.calefay.exodusDefence.radar;

import java.util.ArrayList;

import com.calefay.exodusDefence.EDEntityMap;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameRemoveable;
import com.jme.math.Vector3f;
import com.jme.scene.Node;


public abstract class EDGenericRadar implements TargetAcquirer, GameRemoveable {

	public static float LOCKONTIME = 1.0f;
	
	private final float UPDATEFREQUENCY = 0.5f;
	private boolean active = false;	
	protected GameEntity currentTarget = null;
	
	protected EDEntityMap map = null;
	protected Node radarNode = null;
	protected ArrayList<GameEntity> potentialTargets = null;
	protected RadarMode mode = null;
	
	private float updateTimer = 0, lockonTimer = 0;
	
	private Vector3f currentTargetPos = null;
	
	public EDGenericRadar(Node detectorNode, EDEntityMap map) {
		this.radarNode = detectorNode;
		this.map = map;
		
		mode = RadarMode.SCANNING;
		lockonTimer = 0;
				
		currentTargetPos = new Vector3f();
		
		this.active = true;
	}
	
	/* Returns the closest EDAircraft that lies within the turret's radar cone.*/ 
	public GameEntity getTarget() {
		if(mode == RadarMode.LOCKED) return currentTarget; else return null;
	}
	
	public Vector3f getTrackingPosition() {
		if(currentTarget == null) return null;
		currentTargetPos.set(currentTarget.getPosition());
		return currentTargetPos;
	}
	
	public void findNewTarget() {
		if(map == null) return;
		
		updateTimer = 0;
		
		computeTrackingZone();
		
		if( (currentTarget != null) && (!currentTarget.isDead()) && targetInTrackingZone()){return;}
		
		potentialTargets = getValidTargets();

		if( (potentialTargets == null) || (potentialTargets.size() == 0) ) {
			mode = RadarMode.SCANNING;
			lockonTimer = 0;
			currentTarget = null; 
			return;
		}
		
		currentTarget = getClosestValidTarget();
		lockonTimer = LOCKONTIME; mode = RadarMode.LOCKING;
		
		potentialTargets.clear();
	}
	
	private GameEntity getClosestValidTarget() {
		float closestDistSq = Float.POSITIVE_INFINITY; float distSq = 0;
		GameEntity closest = null;
		
		for(GameEntity entity : potentialTargets) {
			if( !entity.isDead() ) {
				distSq = radarNode.getWorldTranslation().distanceSquared(entity.getPosition());
				if( distSq < closestDistSq) {
					closestDistSq = distSq;
					closest = entity;
				}
			}
		}
		
		return closest;
	}
	
	protected abstract void computeTrackingZone();
	
	protected abstract boolean targetInTrackingZone();
	
	protected abstract ArrayList<GameEntity> getValidTargets();
	
	public void update(float interpolation) {
		if(!active || (mode == RadarMode.INACTIVE) ) return;
		if( (currentTarget != null) && (currentTarget.isDead()) ) {
			currentTarget = null; mode = RadarMode.SCANNING;
		}
		
		updateTimer -= interpolation;
		if( (mode == RadarMode.LOCKING) && (lockonTimer > 0)) lockonTimer -= interpolation;
		if( (mode == RadarMode.LOCKING) && (lockonTimer <= 0) ) {
			lockonTimer = 0;
			mode = RadarMode.LOCKED;
		}
		
		if(updateTimer <= 0) {			
			findNewTarget(); // Should really have a simpler routine to check if the current target is still valid & locked.
			updateTimer = UPDATEFREQUENCY;
		}
		
	}

	public void stopScanning() {mode = RadarMode.INACTIVE;}
	public void startScanning() {if(mode == RadarMode.INACTIVE) mode = RadarMode.SCANNING;}
	public RadarMode getMode() {return mode;}
	public boolean isActive() {return active;}
	public void deactivate() {active = false; currentTarget = null;}
	
	public void cleanup() {
		active = false;
		
		map = null;
		radarNode = null;
		if(potentialTargets != null) potentialTargets.clear(); potentialTargets = null;
		
		currentTargetPos = null;
	}
}
