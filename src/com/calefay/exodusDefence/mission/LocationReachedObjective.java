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
package com.calefay.exodusDefence.mission;


import com.calefay.utils.GameEntity;
import com.jme.math.Vector3f;

public class LocationReachedObjective extends EDBaseObjective {
	private GameEntity entity = null;
	private String entityName = null; private String entityFaction = null;
	private Vector3f targetLocation = null;
	private float requiredProximitySquared = 0;
	
	public LocationReachedObjective(String entityFaction, String entityName, Vector3f targetLocation, float requiredProximity) {
		super();
		
		this.targetLocation = new Vector3f(targetLocation);
		requiredProximitySquared = requiredProximity * requiredProximity;
	}
	
	public String getLabel() {return "Reach location";}
	
	public boolean refreshStatus() {
		if(isComplete()) return true;	// This condition does not let a condition be "un-met".
		if(targetLocation == null) return isComplete();
		if(entity == null) {
			
		} else {
			if(entity.isDead()) {entity = null; return isComplete();}
			if(targetLocation.distanceSquared(entity.getPosition()) <= requiredProximitySquared) {
				progress();
			}
		}
		
		return isComplete();
	}
	
	public void cleanup() {
		super.cleanup();
		entity = null;
		targetLocation = null;
		requiredProximitySquared = 0;
	}
}