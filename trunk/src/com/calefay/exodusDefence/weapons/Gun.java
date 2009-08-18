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

import com.calefay.exodusDefence.radar.TargetAcquirer;
import com.jme.math.Vector3f;
import com.jme.scene.Node;


public abstract class Gun {
	protected String name;
	protected boolean enabled = true;
	protected Vector3f firePoint = null;
	
	protected Node parentNode = null;
	
	public Gun(String gunName, Node parent) {
		name = gunName;
		parentNode = parent;
		enabled = true;
		
		firePoint = new Vector3f( 0, 0, 0);
	}
	
	public void setFirePoint(Vector3f fp) {
		if(fp == null) return;
		firePoint.set(fp);
	}
	
	public abstract void fire();
	
	public abstract void stopFiring();
	
	/* Optionally, target acquisition may be set by overriding this method. For example guided missile launchers need to know what their target is.*/
	public void setTargetting(TargetAcquirer targeter) {}
	
	/** Disables the gun in game, so that it won't fire. Does not deactivate the object in - it will not be deleted.*/
	public void disable() {
		enabled = false;
	}
	
	/** Enables the gun in game, so that it can fire.*/
	public void enable() {
		enabled = true;
	}
	
	public void cleanup() {
		firePoint = null; parentNode = null;
		name = null; enabled = false;
	}
	
	public String toString() {
		return name;
	}
}
