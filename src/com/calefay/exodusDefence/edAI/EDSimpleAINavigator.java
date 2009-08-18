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
package com.calefay.exodusDefence.edAI;

import java.util.ArrayList;


import com.calefay.exodusDefence.EDEntityMap;
import com.calefay.exodusDefence.EDMap;
import com.calefay.exodusDefence.entities.EDAircraft;
import com.calefay.exodusDefence.entities.EDCombatAircraft;
import com.calefay.utils.DebugMethods;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameRemoveable;
import com.jme.math.Vector3f;

public class EDSimpleAINavigator implements GameRemoveable {
	public enum navigationType {FOLLOW_WAYPOINTS, FIXEDWING_STRAFE, HELICOPTER_MISSILE};
	
	private boolean active = false;
	
	private EDAircraft aircraft = null;
	private EDAircraftSimpleAIPilot pilot = null;
	private EDMap map;
	
	private Vector3f destination = null;	// TODO: Do we really need these? Could use waypoints[currentWP]
	private ArrayList<Vector3f> waypoints = null;
	private int currentWP = 0;
	private boolean firing = false;
	private navigationType navigationMode = navigationType.FOLLOW_WAYPOINTS;
	
	private float waypointAccuracy = 0f;
	private float waypointAccuracySquared = 0f;
	private float minSafeAltitude = 0f;
	
	private float attackAltitude = 90.0f;
	private float attackEndAltitude = 40.0f;
	private float strafeCoverDistance = 30.0f;	// The distance that a strafing run will sweep - 50% before and after the target point.
	private EDEntityMap targetMap = null;
	
	private GameEntity targetEntity = null;
	
	/* TODO: Add the ability for the EDMap to return the coordinates of the highest point on a line. 
	 * Then enhance the waypoint plotter to use that.*/
	public EDSimpleAINavigator( navigationType mode, 
								EDAircraft aircraft, EDAircraftSimpleAIPilot pilot, 
								EDMap map, EDEntityMap targetMap) {
		// TODO: Ideally this should not reference the pilot
		active = true;
		this.aircraft = aircraft;
		this.pilot = pilot;
		this.map = map;
		this.targetMap = targetMap;

		destination = new Vector3f();
		firing = false;
		
		this.navigationMode = mode;
		waypoints = new ArrayList<Vector3f>();
		/*
		waypoints.add(new Vector3f(230f, 70f, 400f));
		waypoints.add(new Vector3f(330f, 120f, 400f));	// INTERIM
		waypoints.add(new Vector3f(420f, 68f, 430f));
		waypoints.add(new Vector3f(410f, 120f, 350f));	// INTERIM
		waypoints.add(new Vector3f(400f, 71f, 300f));
		waypoints.add(new Vector3f(310f, 120f, 300f));	// INTERIM
		waypoints.add(new Vector3f(220f, 72f, 300f));
		waypoints.add(new Vector3f(225f, 120f, 350f));	// INTERIM
		*/

		/* TODO: Add a waypoint class that includes heading and possibly precision. 
		   Add to the AI pilot so it can work out an approach to hit the waypoint at the right heading, and the helicopter can hold the heading when using its 4 directional navigation.*/
		
		getNewTarget();
		
		switch(navigationMode) {
			case FOLLOW_WAYPOINTS: setWaypointAccuracy(30.0f); break;
			case FIXEDWING_STRAFE: setWaypointAccuracy(30.0f); planStrafingRun(); break;
			case HELICOPTER_MISSILE: setWaypointAccuracy(3.0f); planHelicopterMissileAttack(); break;
		}
		
		if( (waypoints != null) && (waypoints.size() > currentWP) ) setDestination(waypoints.get(currentWP));
	}
	
	public void update(float interpolation) {
		if(!aircraft.isActive()) deactivate();
		if(!active) return;
		if(pilot.isPlayerAssist()) return;
		switch(navigationMode) {
			case FOLLOW_WAYPOINTS: basicNavigation(); break;
			case FIXEDWING_STRAFE: strafingNavigation(); break;
			case HELICOPTER_MISSILE: helicopterMissileAttackNavigation(); break;
		}
	}
	
	public Vector3f getFallbackPoint() {	// TOD: Ideally this should look for a location on roughly the same heading that it exits its attack run
		return map.getRandomEdgeLocation();
	}
	
	private void getNewTarget() {
		targetEntity = targetMap.getRandomEntity(); 
	}
	
	public void planStrafingRun() {
		if( (targetEntity == null) || (targetEntity.isDead())) return;
		Vector3f target = targetEntity.getPosition();
		
		waypoints.clear();	// FIXME: Do this through an internal method don't directly access the ArrayList.
		Vector3f startPoint = new Vector3f(aircraft.getAircraftNode().getWorldTranslation());	// FIXME: Prefer not having to interact with the node also this assumes you start in a suitable place.
		Vector3f approachVector = target.subtract(startPoint);
		Vector3f oneSecondsTravel = approachVector.normalize().mult(aircraft.getSpeed());
		Vector3f setForAttack = target.subtract( oneSecondsTravel.mult(10f)); // TODO: This will need changing but the idea is it's a waypoint 10 seconds before arrival at the target
		setForAttack.y = target.y + attackAltitude + 10.0f;	// FIXME: Hardcoded attack height.
		ensureMinimumAltitude(setForAttack, minSafeAltitude);
		
		approachVector = target.subtract(setForAttack);
		oneSecondsTravel = approachVector.normalize().mult(aircraft.getSpeed());
		Vector3f startAttack = target.subtract( oneSecondsTravel.mult(7f)); // TODO: This will need changing but the idea is it's a waypoint 5 seconds before arrival at the target
		startAttack.y = target.y + attackAltitude;
		ensureMinimumAltitude(startAttack, minSafeAltitude);
		
		Vector3f diveRecovery = target.add( oneSecondsTravel.mult(5f));
		diveRecovery.y = startAttack.y;
		ensureMinimumAltitude(diveRecovery, minSafeAltitude);
		//Vector3f fallbackPoint = getFallbackPoint();
		Vector3f strafePoint = new Vector3f(target);
		
		//addWaypoint(startPoint);
		addWaypoint(setForAttack, minSafeAltitude);
		addWaypoint(startAttack, minSafeAltitude);
		addWaypoint(strafePoint, 0);
		addWaypoint(diveRecovery, minSafeAltitude);
		//addWaypoint(fallbackPoint);
	}
	
	public void planHelicopterMissileAttack() {
		if( (targetEntity == null) || (targetEntity.isDead())) return;
		Vector3f target = targetEntity.getPosition();
		
		waypoints.clear();	// FIXME: Do this through an internal method don't directly access the ArrayList.
		Vector3f startPoint = new Vector3f(aircraft.getAircraftNode().getWorldTranslation());	// FIXME: Prefer not having to interact with the node also this assumes you start in a suitable place.
		Vector3f approachVector = target.subtract(startPoint);
		Vector3f oneSecondsTravel = approachVector.normalize().mult(aircraft.getSpeed() / 2);	// tacky
		Vector3f startAttack = target.subtract( oneSecondsTravel.mult(5f)); // TODO: This will need changing but the idea is it's a waypoint 10 seconds before arrival at the target
		startAttack.y = target.y + 20.0f;	// FIXME: Hardcoded attack height.
		
		Vector3f endAttack = startAttack.add(0, 10f, 0);
		
		Vector3f fallbackPoint = ensureMinimumAltitude(getFallbackPoint(), minSafeAltitude);
		
		addWaypoint(startPoint, minSafeAltitude);
		addWaypoint(startAttack, minSafeAltitude);
		addWaypoint(endAttack, minSafeAltitude);
		addWaypoint(fallbackPoint, minSafeAltitude);
	}
	
	private void strafingNavigation() {
		if( (targetEntity == null) || (targetEntity.isDead())) {
			getNewTarget();
			if( (targetEntity == null) || (targetEntity.isDead())) return;
			planStrafingRun();
		}
		
		Vector3f currentPos = aircraft.getAircraftNode().getWorldTranslation();
		float distTodestSquared = currentPos.distanceSquared(destination);
		if(currentWP == 2) {	// FIXME: Hardcoded waypoint - assumes the 6 point attack run plan.		
			float attackCompletion = ((currentPos.y - destination.y) - attackEndAltitude) / (attackAltitude - attackEndAltitude);
			Vector3f targetGroundVector = targetEntity.getPosition().subtract(currentPos); targetGroundVector.y = 0;
			Vector3f strafeOffset = targetGroundVector.normalize().mult((attackCompletion - 0.5f) * strafeCoverDistance);
			pilot.setDestination( targetEntity.getPosition().subtract(strafeOffset));
			
			if( pilot.isOnCourse(0.03f, 0.03f) && !firing) {
				firing = true;
				EDCombatAircraft fighter = (EDCombatAircraft)aircraft; // FIXME: Obviously, don't do this!
				if(fighter.getPrimaryGunBarrel1() != null) fighter.getPrimaryGunBarrel1().fire(); 
				if(fighter.getPrimaryGunBarrel2() != null) fighter.getPrimaryGunBarrel2().fire();
			}
			if( (currentPos.y - destination.y) < attackEndAltitude) {
				currentWP++;
				if(currentWP >= waypoints.size()) currentWP = 0;
				setDestination(waypoints.get(currentWP));
				pilot.setPointAt(false);
				pilot.setMinSafeAltitude(minSafeAltitude);
				EDCombatAircraft fighter = (EDCombatAircraft)aircraft; // FIXME: Obviously, don't do this!
				if(fighter.getPrimaryGunBarrel1() != null) fighter.getPrimaryGunBarrel1().stopFiring(); 
				if(fighter.getPrimaryGunBarrel2() != null) fighter.getPrimaryGunBarrel2().stopFiring();
				firing = false;
			}
		} else if(distTodestSquared < waypointAccuracySquared){
			currentWP++;
			if(currentWP >= waypoints.size()) {
				currentWP = 0;
				planStrafingRun();
			}
			setDestination(waypoints.get(currentWP));
			if(currentWP == 2) {
				pilot.setPointAt(true);
				pilot.setMinSafeAltitude(0);
			} else {
				pilot.setPointAt(false);
				pilot.setMinSafeAltitude(minSafeAltitude);
			}
		}
	}
	
	private void helicopterMissileAttackNavigation() {
		if( (targetEntity == null) || (targetEntity.isDead())) {
			getNewTarget();
			if( (targetEntity == null) || (targetEntity.isDead())) return;
			planHelicopterMissileAttack();
		}
		
		Vector3f currentPos = aircraft.getAircraftNode().getWorldTranslation();
		float distTodestSquared = currentPos.distanceSquared(destination);
		if(currentWP == 2) {	// FIXME: Hardcoded waypoint - assumes the 4 point attack run plan.					
			if( !firing) {
				firing = true;
				EDCombatAircraft fighter = (EDCombatAircraft)aircraft; // FIXME: Obviously, don't do this!
				fighter.getPrimaryGunBarrel1().fire(); fighter.getPrimaryGunBarrel2().fire();
			}
			if(distTodestSquared < waypointAccuracySquared) {
				currentWP++;
				if(currentWP >= waypoints.size()) currentWP = 0;
				setDestination(waypoints.get(currentWP));
				EDCombatAircraft fighter = (EDCombatAircraft)aircraft; // FIXME: Obviously, don't do this!
				fighter.getPrimaryGunBarrel1().stopFiring(); fighter.getPrimaryGunBarrel2().stopFiring();
				firing = false;
			}
		} else if(distTodestSquared < waypointAccuracySquared) {
			currentWP++;
			if(currentWP >= waypoints.size()) {
				currentWP = 0;
				planHelicopterMissileAttack();
			}
			setDestination(waypoints.get(currentWP));
		}
	}
	
	private void basicNavigation() {
		Vector3f currentPos = aircraft.getAircraftNode().getWorldTranslation();
		float distTodestSquared = new Vector3f(currentPos.x, 0, currentPos.z).distanceSquared( new Vector3f(destination.x, 0, destination.z));
		if(distTodestSquared < waypointAccuracySquared){
			currentWP++;
			if(currentWP >= waypoints.size()) currentWP = 0;
			setDestination(waypoints.get(currentWP));
		}
		DebugMethods.text.get(3).print("Heading to waypoint " + currentWP + ", distance: " + Math.round(Math.sqrt(distTodestSquared)) );
	}
	
	
	private void setDestination(Vector3f dest) {
		destination.set(dest);
		pilot.setDestination(dest);
	}
	
	public void setWaypointAccuracy(float accuracy) {
		this.waypointAccuracy = accuracy;
		this.waypointAccuracySquared = waypointAccuracy * waypointAccuracy;
	}

	public void setMinSafeAltitude(float alt) {minSafeAltitude = alt;}
	public void deactivate() {active = false;}
	public boolean isActive() {return active;}
	
	/* If minAltitude > 0 then the waypoint Vector will be adjusted if necessary to ensure the waypoint is at least minAltitude above the terrain.*/
	public void addWaypoint(Vector3f waypoint, float minAltitude) {
		if(waypoint == null) return;
		if(minAltitude > 0) ensureMinimumAltitude(waypoint, minAltitude);
		waypoints.add(waypoint);
	}
	
	
	/* Sets the y value of the given Vector to height above the terrain. Does nothing if no map is available.*/
	public void setAltitude(Vector3f waypoint, float altitude) {
		if(waypoint == null) return;
		if(map != null) {waypoint.setY(map.getHeightFromWorld(waypoint) + altitude);}
	}
	
	/* If the waypoint is less than height above the terrain then it is set to height above the terrain.*/
	public Vector3f ensureMinimumAltitude(Vector3f waypoint, float height) {
		if(waypoint == null) return null;
		if(map != null) {
			float safeAltitude = map.getHeightFromWorld(waypoint) + height;
			if(waypoint.y  < safeAltitude) waypoint.setY(safeAltitude);  
		}
		return waypoint;
	}
	
	public void cleanup() {
		active = false;
		aircraft = null;
		pilot = null;
		map = null;
		destination = null;
		waypoints.clear(); waypoints = null;
		currentWP = 0;
	}
}
