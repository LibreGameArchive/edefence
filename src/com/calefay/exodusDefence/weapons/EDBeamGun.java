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


import com.calefay.utils.GameRemoveable;
import com.calefay.utils.GameWorldInfo;
import com.jme.math.Vector3f;
import com.jme.scene.Node;


public class EDBeamGun extends Gun implements GameRemoveable {

	private LaserBeamWeapon laser = null;
	private float maxTemperature = 0;
	private float temperatureLockout = 0;
	/** Temp increase is the rate of temperature change when turned ON*/
	private float tempIncreasePerSecond = 0;
	/** Cooling means the rate it will cool at when turned OFF.*/
	private float coolingPerSecond = 0;
	
	private float currentTemperature = 0;
	private float currentLockout = 0;
	private String fireEventType = null;
	private Vector3f fireLoc = null;
	
	private boolean active = true;	// Active in the do not discard sense
	private boolean firing = false;	// Means trying to fire - it still could be temperature locked.
	
	public EDBeamGun(	String name, LaserBeamWeapon weapon, 
						float maxTemp, float tempIncreasePerSec, float coolingPerSec, float overHeatLockout, 
						Node parentNode) {
		super(name, parentNode);
		active = true;
		firing = false;
		maxTemperature = maxTemp;
		temperatureLockout = overHeatLockout;
		tempIncreasePerSecond = tempIncreasePerSec;
		coolingPerSecond = coolingPerSec;

		laser = weapon;
		laser.setRegisterHits(true);
		laser.setOffset(firePoint);
		stopFiring();
		
		fireEventType = null; fireLoc = new Vector3f();
	}
	
	public EDBeamGun(String name, LaserBeamWeapon weapon, float maxTemp, float tempIncreasePerSec, float coolingPerSec, Node parentNode) {
		this(name, weapon, maxTemp, tempIncreasePerSec, coolingPerSec, 1.0f, parentNode);
	}
	
	public EDBeamGun(String name,  LaserBeamWeapon weapon, float maxTemp, Node parentNode) {
		this(name, weapon, maxTemp, 1.0f, 1.0f, 1.0f, parentNode);
	}
	
	public void setBeamWidth(float w) {
		laser.setBeamWidth(w);
	}
	
	public void update(float interpolation) {
		if(!enabled) return; // Should it cool down when disabled?
		
		if(laser.isOn() && (!firing)) stopFiring();
		if(!laser.isOn() && firing) fire();
		
		laser.update(interpolation);	// TODO: Should be added to a manager in it's own right.
		if(laser.isOn()) {
			currentTemperature += tempIncreasePerSecond  * interpolation; 
			if(currentTemperature >= maxTemperature) {
				laser.setOff();
				currentLockout = temperatureLockout;
			}
		} else {
			if(currentLockout > 0) {
				currentLockout -= interpolation;
				if (currentLockout < 0) {currentLockout = 0;}
			}
			if(currentTemperature > 0) {
				currentTemperature -= coolingPerSecond * interpolation;
				if (currentTemperature < 0) {currentTemperature = 0;}
			}
		}
	}
	
	@Override
	public void fire() {
		if(!enabled) return;
	
		firing = true;
		if ( (currentTemperature < maxTemperature) && (currentLockout == 0) && !laser.isOn()) {
			laser.setOn();
			parentNode.localToWorld(firePoint, fireLoc);
			if(fireEventType != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(fireEventType,  "audio", fireLoc, null);
		}
	}
	
	public void stopFiring() {
		firing = false;
		
		if (laser.isOn()) {
			laser.setOff();
		}
	}
	
	public void disable() {
		super.disable();
		stopFiring();
	}
	
	public float getTemperature() {return currentTemperature;}
	public float getMaxTemperature() {return maxTemperature;}
	public boolean isLockedOut() {return (currentLockout > 0);}
	public boolean isActive(){return active;}
	public void setFireEventType(String type) {fireEventType = type;}
	
	public void deactivate() {
		laser.deactivate();
		active = false;
	}
	
	public void cleanup() {
		super.cleanup();
		laser.cleanup();
	}
	
	public String toString() {return name;}
}
