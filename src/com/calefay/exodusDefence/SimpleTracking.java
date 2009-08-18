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


import com.calefay.exodusDefence.weapons.BaseProjectile;
import com.calefay.utils.GameEntity;
import com.jme.math.Vector3f;


public class SimpleTracking extends MissileTrackingModule {

	private Vector3f actualPos = null;
	private Vector3f targetPos = null;
	private float extraHeight = 0;
	
	public SimpleTracking() {
		actualPos = new Vector3f();
		targetPos = new Vector3f();
		extraHeight = 0;
	}
	
	public SimpleTracking(float approachHeight) {
		this();
		extraHeight = approachHeight;
	}
	
	@Override
	public void trackTarget(BaseProjectile missile, GameEntity target) {
		// Might want to rethink this method, probably better just to set the target then have a standard update method, supplying interpolation.
		if( (target == null) || (target.isDead()) ) return;
		
		targetPos.set(target.getPosition()); 
		float targetY = targetPos.y; targetPos.setY(0);
		actualPos.set(missile.getProjectileNode().getWorldTranslation()); actualPos.setY(0);
		float distSquared = targetPos.distanceSquared(actualPos);
		float h = extraHeight;
		if( (h != 0) && (distSquared < 10000f)) h = distSquared / 200;	// TODO: Crude and hardcoded missile tracking
		targetPos.y = targetY + h;
		
		missile.getProjectileNode().lookAt(targetPos, Vector3f.UNIT_Y);

	}

}
