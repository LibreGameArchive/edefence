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

import java.util.HashMap;

import com.calefay.exodusDefence.edAI.EDAIController;
import com.calefay.exodusDefence.edAI.EDAircraftSimpleAIPilot;
import com.calefay.exodusDefence.edAI.EDSimpleAINavigator;
import com.calefay.exodusDefence.edAI.EDTurretSimpleAI;
import com.calefay.exodusDefence.edAI.EDSimpleAINavigator.navigationType;
import com.calefay.exodusDefence.entities.EDCombatAircraft;
import com.calefay.exodusDefence.entities.EDTurret;
import com.calefay.exodusDefence.radar.EDCircleZoneRadar;
import com.calefay.utils.RemoveableEntityManager;

public class EDControllerHandler {
	private HashMap<PlayableEntity, EDAIController> entityAI = null;
	private RemoveableEntityManager controllerManager = null;
	
	public EDControllerHandler() {
		entityAI = new HashMap<PlayableEntity, EDAIController>();
		controllerManager = new RemoveableEntityManager();
	}
	
	public void update(float interpolation) {
		controllerManager.updateEntities(interpolation);
	}
	
	public void addTurretAIController(EDTurret turret, EDEntityMap targetMap, float turretRange) {
		EDCircleZoneRadar radar = new EDCircleZoneRadar(turret.getMountingNode(), targetMap, turretRange);
		EDTurretSimpleAI tAI = new EDTurretSimpleAI(turret, radar);
		tAI.setWeaponRange(turretRange);
		entityAI.put(turret, tAI);
		controllerManager.add(radar); controllerManager.add(tAI);
	}
	
	public void addAircraftAIController(EDCombatAircraft aircraft, boolean isHelicopter, EDMap map, EDEntityMap targetMap) {
		EDAircraftSimpleAIPilot pilot = new EDAircraftSimpleAIPilot(aircraft, map);
		EDSimpleAINavigator navigator = null;
		
		if(isHelicopter) {
			navigator = new EDSimpleAINavigator(navigationType.HELICOPTER_MISSILE, aircraft, pilot, map, targetMap);
			pilot.setMinSafeAltitude(5f); navigator.setMinSafeAltitude(5f);
			pilot.setHelicopterMode(true);
		} else {
			navigator = new EDSimpleAINavigator(navigationType.FIXEDWING_STRAFE, aircraft, pilot, map, targetMap);
			pilot.setMinSafeAltitude(15f); navigator.setMinSafeAltitude(15f);	// FIXME: These should be optional and scripted.
		}
		
		entityAI.put(aircraft, pilot);
		controllerManager.add(pilot); controllerManager.add(navigator);
	}
	
	public EDAIController getEntityController(PlayableEntity entity) {return entityAI.get(entity);}
	
	public void removeTurretAIController(PlayableEntity turret) {
		entityAI.remove(turret);
	}
	
	public void cleanupLevel() {
		if(entityAI != null) entityAI.clear();
		if(controllerManager != null) controllerManager.clearEntities();
	}
	
	public void cleanup() {
		if(entityAI != null) entityAI.clear(); entityAI = null;
		if(controllerManager != null) controllerManager.cleanup(); controllerManager = null;
	}
}
