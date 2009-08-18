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


import com.calefay.exodusDefence.entities.EDTurret;
import com.calefay.exodusDefence.radar.EDGenericRadar;
import com.calefay.exodusDefence.weapons.EDProjectileGun;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameRemoveable;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

public class EDTurretSimpleAI implements GameRemoveable, EDAIController {
	private static final float MAXSPIN = FastMath.PI / 2;	// TODO: Should make these variables
	private static final float MAXELEV = FastMath.PI / 2;	// Should make these variables
	private static final float MINSPIN = 0.001f;
	private static final float MINELEV = 0.001f;
	private static final float SHOOTANGLE = FastMath.PI / 20f;
	
	private float weaponRange = -1f;
	
	private EDTurret handledTurret = null;
	private GameEntity target = null;
	private EDGenericRadar targetFinder = null;
	private boolean active = false;
	private boolean suspended = false;
	private boolean firing = false; private boolean wantToFire = false;
	
	private float spinAngle = 0;
	private float elevAngle = 0;
	private float targetRangeSquared = 0;
	
	public EDTurretSimpleAI(EDTurret t, EDGenericRadar targetFinder) {
		active = true; suspended = false;
		handledTurret = t;
		this.targetFinder = targetFinder;
		spinAngle = 0; elevAngle = 0;
		firing = false; wantToFire = false;
	}
	
	public void update(float interpolation) {
		if(!active || (handledTurret.isDead()) ) return;
		if(!handledTurret.isActive()) deactivate();
			
		wantToFire = false;
		if(suspended) return;
		target = targetFinder.getTarget();
		
		if(target != null) { 
			calculateAngles(interpolation);
			
			float spinActual = spinAngle; float elevActual = elevAngle;
			float ms = MAXSPIN * interpolation; float me = MAXELEV * interpolation;
			if(spinActual > ms) spinActual = ms; if(spinActual < -ms) spinActual = -ms;
			if(elevActual > me) elevActual = me; if(elevActual < -me) elevActual = -me;
			
			// TODO: Neaten up all these bounds checks. Possibly do the minimum thing inside EDTurret.
			if( (spinActual > MINSPIN) || (spinActual < -MINSPIN) ) handledTurret.spin(spinActual); 
			if( (elevActual > MINELEV) || (elevActual < -MINELEV) ) handledTurret.elevate(elevActual);
			
			handledTurret.getTurretNode().updateWorldData(interpolation);	// Does this have to be done here?

			if( (spinAngle > -SHOOTANGLE) && (spinAngle < SHOOTANGLE) && (elevAngle > -SHOOTANGLE) && (elevAngle < SHOOTANGLE) ) {
				wantToFire = true;
			} else {
				wantToFire = false;
			}
			
		}
		
		if( (weaponRange > 0) && (targetRangeSquared > (weaponRange * weaponRange) ) ) {wantToFire = false; target = null;}
		
		if(wantToFire && !firing) {
			if(handledTurret.getPrimaryGunBarrel1() != null) handledTurret.getPrimaryGunBarrel1().fire(); 
			if(handledTurret.getPrimaryGunBarrel2() != null) handledTurret.getPrimaryGunBarrel2().fire();
			firing = true;
		} else if(firing && !wantToFire) {
			if(handledTurret.getPrimaryGunBarrel1() != null) handledTurret.getPrimaryGunBarrel1().stopFiring(); 
			if(handledTurret.getPrimaryGunBarrel2() != null) handledTurret.getPrimaryGunBarrel2().stopFiring();
			firing = false;
		}
		
	}
	
	protected void calculateAngles(float tpf) {
		// This is really crude tracking but will do for now.
		Quaternion rotWorldtoLocal = handledTurret.getTurretNode().getWorldRotation().inverse();
		Vector3f actualPos = target.getPosition(); // TODO: Estimating code that takes speed (target and projectile) in to account!
		Vector3f targetPos = actualPos; // No leading - for lasers
		if(handledTurret.getPrimaryGunBarrel1() instanceof EDProjectileGun) targetPos = new Vector3f(actualPos.add(target.getVelocity().mult(tpf * 20))); 

		Vector3f position = handledTurret.getTurretNode().getWorldTranslation();
		Vector3f offset = new Vector3f();
		targetPos.subtract(position, offset);	// Offset is the position of the target relative to the turret
		targetRangeSquared = offset.lengthSquared();
		Vector3f direction = new Vector3f( rotWorldtoLocal.mult(new Vector3f(offset.x, 0, offset.z).normalize() )); // The normalized offset, in turret local coordinates
		
		spinAngle = Vector3f.UNIT_Z.angleBetween(direction); // Angle between our facing (z axis) and the target direction is the angle we need to turn by
		if(direction.x < 0) spinAngle = -spinAngle; // If the direction in local coords has negative x, turn the other way!
		
		// Now elevation
		rotWorldtoLocal = handledTurret.getMountingNode().getWorldRotation().inverse();
		
		Quaternion q = new Quaternion();
		q.fromAngleNormalAxis(-spinAngle, Vector3f.UNIT_Y); // Rotation that would bring us perfectly in line
		direction.set( q.mult(offset.normalize() )); // The direction to target if we were perfectly lined up on spin
		direction.set(rotWorldtoLocal.mult(direction));	// Change to local coords. Will always be in line with Z axis as we have used exact spin.
		
		elevAngle = Vector3f.UNIT_Z.angleBetween(direction) / 2; // Angle between our facing (z axis) and the target direction is the angle we need to elevate by.
		if(direction.y > 0) elevAngle = -elevAngle; // If it's up, go the other way don't try to do a somersault

	}

	public void setWeaponRange(float range) {weaponRange = range;}
	public boolean isActive() {return active;}
	public void deactivate() {active = false; targetFinder.deactivate();}
	/** Suspends the ai. Used for when the turret is under player control.*/
	public void setSuspend(boolean s) {suspended = s;}
	
	public void cleanup() {
		targetFinder = null;
		active = false;
		suspended = false;
		handledTurret = null;
		target = null;
		targetFinder = null;
		spinAngle = 0; elevAngle = 0;
	}
	
}
