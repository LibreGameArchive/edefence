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
package com.calefay.exodusDefence.entities;


import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameRemoveable;
import com.calefay.utils.GameWorldInfo;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;

public class EDAircraft implements GameRemoveable, GameEntity {
	
	protected String name = null;
	
	protected Node aircraftNode = null;
	private float maxHitPoints = 0, hitPoints = 0;
	private boolean active = false;
	
	private Quaternion rot = null;
	private boolean hasRotated = false;
	private float maxRollRate = 0, maxPitchRate = 0, maxYawRate = 0;	// Maximum turn rates, in radians per second.
	private float maxThrust = 0, maxLift = 0, weight = 0;	// None of these are implemented accurately. They are just three speeds (not accelerations) applied in the up, down and thrustVector directions
	private float stickRoll = 0, stickPitch = 0, stickYaw = 0;	// From -1..1 represents the position of the virtual control stick. This can be changed externally, rather than manouvering the node directly.
	private float stickThrottle = 0;
	
	private Vector3f thrustVector = null; private Vector3f worldThrustVector = null;
	private Vector3f velocity = null;
	private float airSpeed = 0;
	
	private String onDestroyedEventType = "EDAircraftDestroyed";
	private String faction = "default";
	
	private float pitch = 0, roll = 0, heading = 0;
	
	public EDAircraft(String name) {
		this.name = name;
		aircraftNode = new Node(name + "EDAircraftNode");
		maxHitPoints = 1f; hitPoints = 1f;
		active = true;
		
		stickRoll = 0; stickPitch = 0; stickYaw = 0; stickThrottle = 0;
		airSpeed = 0; 
		maxRollRate = FastMath.PI; maxPitchRate = FastMath.HALF_PI / 2.0f;	maxYawRate = FastMath.HALF_PI / 2.0f; // TODO: Hardcoded turn rates.
		thrustVector = Vector3f.UNIT_Z; worldThrustVector = new Vector3f();
		worldThrustVector = thrustVector;	// Needs an initial value.
		
		velocity = new Vector3f();
		rot = new Quaternion();
	}
	
	/** These parameters are not implemented in a physically accurate way:
	 * @param Thrust: Speed at which you will move in the thustVector (default is forward) direction at 100% throttle.
	 * @param Weight: Speed (not acceleration) at which you will move downwards in units per second.
	 * @param Lift: Speed at which you will move upwards in units per second. Does NOT currently change with speed etc.*/
	public void setCharacteristics(float maxThrust, float lift, float weight, 
									float rollRate, float pitchRate, float yawRate, 
									float hitPoints) {
		this.maxThrust = maxThrust;
		this.maxLift = lift; 
		this.weight = weight;
		this.maxRollRate = rollRate;
		this.maxPitchRate = pitchRate;
		this.maxYawRate = yawRate;
		
		this.maxHitPoints = hitPoints; this.hitPoints = hitPoints;
	}
	
	public void update(float interpolation) {
		// FIXME: Do some out of bounds checking.
		hasRotated = false;
		
		// TODO: Consider only doing the heading/pitch/roll calculations if the results are needed (eg. the first time one of the getters is called).
		heading = calculateHeading();
		pitch = calculatePitch();
		roll = calculateRoll(heading);
		
		if(FastMath.abs(stickRoll) >= 0.1f) {	// TODO: Hardcoding
			float t = stickRoll * maxRollRate * interpolation;
			rot.fromAngleNormalAxis(t, Vector3f.UNIT_Z);
			aircraftNode.getLocalRotation().multLocal(rot);
			hasRotated = true;
		}
		if( FastMath.abs(stickPitch) >= 0.1f) {	// TODO: Hardcoding
			float t = stickPitch * maxPitchRate * interpolation;
			rot.fromAngleNormalAxis(t, Vector3f.UNIT_X);
			aircraftNode.getLocalRotation().multLocal(rot);
			hasRotated = true;
		}
		if( FastMath.abs(stickYaw) >= 0.1f) {	// TODO: Hardcoding
			float t = stickYaw * maxYawRate * interpolation;
			rot.fromAngleNormalAxis(t, Vector3f.UNIT_Y);
			aircraftNode.getLocalRotation().multLocal(rot);
			hasRotated = true;
		}
		
		worldThrustVector = aircraftNode.getWorldRotation().mult(thrustVector);
		 
		 //float effectiveLift = calculateLift(maxLift, airSpeed) * aircraftNode.getWorldRotation().getRotationColumn(1).y;
		 float effectiveLift = aircraftNode.getWorldRotation().getRotationColumn(1).mult(calculateLift(maxLift, airSpeed)).y;
		 velocity = worldThrustVector.mult(maxThrust * stickThrottle);	// Thrust
		 velocity.y += effectiveLift; // Lift
		 velocity.y -= weight; // Gravity
		 Vector3f velocityInLocal = aircraftNode.getWorldRotation().inverse().mult(velocity);
		 airSpeed = velocityInLocal.z;
		 
		// TODO: Currently the none-vertical component of lift is discarded. Should create a turning force for normal banking turns.
		aircraftNode.getLocalTranslation().addLocal(velocity.mult(interpolation));

		if(hasRotated) aircraftNode.getLocalRotation().normalize(); // FIXME: This is to combat degradation caused by constant tiny rotations
		aircraftNode.updateWorldVectors();	// TODO: Really this is just to fix a bit of jumpiness when under player control - probably there's a more efficient solution.
	}	
	
	/** Sets the position of a virtual control stick, in the axis which will cause the aircraft to roll. Range is -1 to 1, representing full left to full right. 
	 *  This should be used by the pilot interface instead of maneuvering the node directly.*/ 
	public void setControlStickX(float x) {	
		// FIXME: Cap the maximum amount you can change the value by as you shouldn't be able to go from full left to full right every frame (causes the aircraft to judder).
		if(Float.isNaN(x)) return;
		stickRoll = -x;
		if(stickRoll > 1.0f) stickRoll = 1.0f;
		if(stickRoll < -1.0f) stickRoll = -1.0f;
	}
	
	/** Sets the position of a virtual control stick, in the axis which will cause the aircraft to pitch. Range is -1 to 1, representing full down to full up. 
	 *  This should be used by the pilot interface instead of maneuvering the node directly.*/
	public void setControlStickY(float y) {
		// FIXME: Cap the maximum amount you can change the value by as you shouldn't be able to go from full up to full down every frame (causes the aircraft to judder).
		if(Float.isNaN(y)) return;
		stickPitch = -y;
		if(stickPitch > 1.0f) stickPitch = 1.0f;
		if(stickPitch < -1.0f) stickPitch = -1.0f;
	}
	
	/** Sets the position of a virtual control stick, in the axis which will cause the aircraft to yaw. Range is -1 to 1, representing full left to full right. 
	 *  This should be used by the pilot interface instead of maneuvering the node directly.*/
	public void setControlStickZ(float z) {
		// FIXME: Cap the maximum amount you can change the value by as you shouldn't be able to go from full up to full down every frame (causes the aircraft to judder).
		if(Float.isNaN(z)) return;
		stickYaw = -z;
		if(stickYaw > 1.0f) stickYaw = 1.0f;
		if(stickYaw < -1.0f) stickYaw = -1.0f;
	}
	
	public void setStickThrottle(float t) {
		if(Float.isNaN(t)) return;
		stickThrottle = t;
		if(stickThrottle > 1.0f) stickThrottle = 1.0f;
		if(stickThrottle < 0f) stickThrottle = 0f;
	}
	
	public void moveStickThrottle(float deltaT) {
		if( FastMath.abs(deltaT) < 0.01) return;
		setStickThrottle(stickThrottle + deltaT);
	}
	
	/** Calculate pitch by taking the angle between our Z axis and the X-Z (ground) plane. UNIT_Y is the normal to the X-Z plane which is used to calculate that angle.*/
	private float calculatePitch() {
		Vector3f aircraftZ = getAircraftNode().getWorldRotation().getRotationColumn(2);	// Aircraft Z axis in world space
		float pitch = aircraftZ.angleBetween(Vector3f.UNIT_Y) - FastMath.HALF_PI;
		return -pitch;
	}
	
	/** Calculate heading as the angle between North (world UNIT_Z), and the X-Z component of our direction (Z axis).*/
	private float calculateHeading() {
		Vector3f aircraftZ = getAircraftNode().getWorldRotation().getRotationColumn(2);	// Aircraft Z axis in world space
		float heading = Vector3f.UNIT_Z.angleBetween( new Vector3f(aircraftZ.x, 0, aircraftZ.z).normalize() );
		if(aircraftZ.x > 0) {heading = FastMath.TWO_PI - heading;}
		return heading;
	}
	
	/** Calculate roll by working out a level vector (X axis spun to our heading), and taking the angle between it and our actual X axis.
	 *  Requires an accurate heading to be passed in (eg. from caclulateHeading).*/
	private float calculateRoll(float heading) {
		Vector3f aircraftX = getAircraftNode().getWorldRotation().getRotationColumn(0);	// Aircraft X axis in world space
		Quaternion q = new Quaternion();
		q.fromAngleAxis(FastMath.TWO_PI - heading, Vector3f.UNIT_Y);
		Vector3f levelX = q.mult(Vector3f.UNIT_X);
		float roll = levelX.angleBetween(aircraftX);
		if(aircraftX.y < 0) {roll = -roll;}
		return roll;
	}
	
	/** Very simple implementation - lift scales linearly with airspeed up to a maximum of liftFactor when airSpeed = thrust.
	 * Override for more realism.
	 * @param liftFactor
	 * @param airSpeed
	 */
	protected float calculateLift(float liftFactor, float airSpeed) {
		float actualLift = (airSpeed / maxThrust) * liftFactor;
		if(actualLift < 0) actualLift = 0; if(actualLift > liftFactor) actualLift = liftFactor;
		return actualLift;
	}
	
	public boolean handleAction(GameEvent action) {
		if(!active) return false;
		// TODO: replace the hardcoded string with an appropriate constant.
		if(action.getEventType() == "damage") {
			// TODO: This should be in a try..catch with error handling.
			float damageAmount = (Float)action.getEventTarget();
			hitPoints -= damageAmount;
			
			if(hitPoints <= 0) {
				hitPoints = 0;
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("EntityDestroyed",  "gameplayEvents", this, null);
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(onDestroyedEventType,  "default", this, null);
				deactivate();
			}
			return true;
			} else {return false;}
	}
	
	public Node getAircraftNode() {
		return aircraftNode;		// TODO: Ideally would like this not to be available publicly, should interact with the node though method calls.
	}
	
	/** Sets the direction of thrust for this aircraft. Default is UNIT_Z (eg. a simple plane). UNIT_Y would be appropriate for a simple helicopter.*/
	public void setThrustVector(Vector3f v) {
		this.thrustVector = v;
	}
	
	public void repair(float hp) {
		this.hitPoints += hp;
		if(this.hitPoints > this.maxHitPoints) this.hitPoints = this.maxHitPoints;
	}
	
	public void setFaction(String faction) {this.faction = faction;}
	
	public boolean isActive() {return active;}
	public float getMaxRollRate() {return maxRollRate;}
	public float getMaxPitchRate() {return maxPitchRate;}
	public float getMaxYawRate() {return maxYawRate;}
	public float getHeading() {return heading;}
	public float getPitch() {return pitch;}
	public float getRoll() {return roll;}
	public String getName() {return name;}
	public String getFaction() {return faction;}

	// TODO: Would prefer to separate these getters into a new class to represent instruments, as they clutter up the simple aircraft.
	/** Returns the direction of the aircraft's thrust, in world space (as of last update).*/
	public Vector3f getWorldThrustVector() {return worldThrustVector;}
	/** Returns a COPY of the thrust vector, in local space.*/
	public Vector3f getLocalThrustVector() {return new Vector3f(thrustVector);}
	public float getSpeed() {return maxThrust;}	// FIXME: This is definitely not a keeper!
	public float getThrust() {return maxThrust;}
	public float getStickThrottle() {return stickThrottle;}
	public void setOnDestroyedEventType(String eventType) {onDestroyedEventType = eventType;}
	public float getHitpoints() {return hitPoints;}
	
	// GameEntity implemented methods begin
	public float getMaxHitpoints() {return maxHitPoints;}
	public Vector3f getPosition() {return aircraftNode.getWorldTranslation();}
	public Quaternion getRotation() {return aircraftNode.getWorldRotation();}
	public Vector3f getVelocity(){return velocity;}
	public boolean isDead() {return !isActive();}
	// GameEntity implemented methods end
	
	public void deactivate() {
		active = false;
		if(aircraftNode != null) {aircraftNode.removeFromParent();}
	}
	
	public void cleanup() {	// FIXME: Looks like there's some stuff missing from this cleanup.
		deactivate();
		if(aircraftNode != null) aircraftNode.detachAllChildren(); aircraftNode = null;
	}
	
}
