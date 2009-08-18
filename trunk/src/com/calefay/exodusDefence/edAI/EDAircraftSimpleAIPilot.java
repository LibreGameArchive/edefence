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


import com.calefay.exodusDefence.EDMap;
import com.calefay.exodusDefence.entities.EDAircraft;
import com.calefay.utils.DebugMethods;
import com.calefay.utils.GameRemoveable;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

/** Many aspects of this class make assumptions about the flight model, and it is full of magic numbers.
 *  Do not expect easy reuse.*/
public class EDAircraftSimpleAIPilot implements EDAIController, GameRemoveable {
	public boolean statsMode = false;	// FIXME: AIRCRAFT TESTING ONLY
	private boolean active = false;
	private boolean pointAtMode = false;
	private EDAircraft handledAircraft = null;
	
	private EDMap map = null;
	private boolean ignoreTerrain = true;
	
	private Vector3f destination = null;
	
	private boolean slowOnApproach = false;
	private float minSafeAltitude = 0;
	
	private float pitchChange = 0; 
	private float rollChange = 0;
	private float headingChange = 0;
	
	private boolean isHelicopter = false;
	private boolean playerAssistMode = false;	// NOTE: Only partially implemented - mainly for combatting helicopter roll.
	
	public EDAircraftSimpleAIPilot(EDAircraft aircraft) {
		active = true;
		pointAtMode = false;
		isHelicopter = false;
		playerAssistMode = false;
		slowOnApproach = true;	// FIXME: Temp only.
		handledAircraft = aircraft;
		destination = new Vector3f();
		ignoreTerrain = true;
	}
	
	public EDAircraftSimpleAIPilot(EDAircraft aircraft, EDMap map) {
		this(aircraft);
		if(map != null) {this.map = map; ignoreTerrain = false;} else ignoreTerrain = true;
	}
	
	public void update(float interpolation) {
		if(!handledAircraft.isActive()) {deactivate(); return;}
		
		float pitch = handledAircraft.getPitch();
		float heading = handledAircraft.getHeading();
		float roll = handledAircraft.getRoll();
		Vector3f pos = handledAircraft.getAircraftNode().getWorldTranslation();
		
		if(isHelicopter) pilotHelicopter(pitch, heading, roll, pos); else pilotFixedWing(pitch, heading, roll, pos);
		
	}
	
	public void pilotFixedWing(float pitch, float heading, float roll, Vector3f pos) {
		if(playerAssistMode) return; // Currently no assistance given for fixed wing aircraft.
		
		float desiredHeading = getDesiredHeading(pos, destination);
		headingChange = getHeadingAdjustment(heading, desiredHeading);
		
		float desiredPitch = 0;
		if( !ignoreTerrain && (minSafeAltitude > 0) && (map != null) ) {desiredPitch = FastMath.HALF_PI * getEmergencyTerrainAvoidanceFactor(pos, 2.0f);}
		if(desiredPitch <= 0) desiredPitch = getDesiredPitch(pos, destination);
		pitchChange = getAngleAdjustment(pitch, desiredPitch);
		
		float desiredRoll = calculateDesiredRoll(headingChange, pitchChange);
		rollChange = getAngleAdjustment(roll, desiredRoll);
		
		handledAircraft.setControlStickX(-getStickPos(rollChange, false));
		computePitchYawMovements(roll, headingChange, pitchChange);
		
		if(statsMode) {	// Debug only
			printStats(heading, pitch, roll, desiredHeading, desiredPitch, desiredRoll, pos);
		}
	}
	/* TODO: The helicopter pilot emergency terrain avoidance needs improving. 
	 * It  can switch from forward to straight up very quickly. When going up it is no longer on a collision course - 
	 * so it rapidly alternates between collision avoidance and forward flight. Possibly check for obstacles along the direction it wants to travel not its momentary velocity.*/ 
	public void pilotHelicopter(float pitch, float heading, float roll, Vector3f pos) {
		if(playerAssistMode) {
			float desiredRoll = 0;
			rollChange = getAngleAdjustment(roll, desiredRoll);
			handledAircraft.setControlStickX(-getStickPos(rollChange, false));
		} else {
			Vector3f posChange = destination.subtract(pos);
			if( (FastMath.abs(posChange.x) < 10f) && (FastMath.abs(posChange.z) < 10f) ) {
				pilotHelicopter4Directional(pitch, heading, roll, pos, 0.2f, 10f);	// Magic numbers
			} else {
				pilotHelicopterForwardFlight(pitch, heading, roll, pos);
			}
		}
	}
	
	public void pilotHelicopterForwardFlight(float pitch, float heading, float roll, Vector3f pos) {
		float desiredPitch = 0; float avoidanceFactor = 0;
		if( !ignoreTerrain && (minSafeAltitude > 0) && (map != null) ) {
			avoidanceFactor = getEmergencyTerrainAvoidanceFactor(pos, 2.0f);
		}
		
		float altitudeChange = destination.y - pos.y;
		float changeRate = clamp(altitudeChange, -4f, 4f);	// FIXME: Magic number
		if( avoidanceFactor == 0) {
			handledAircraft.moveStickThrottle(calculateThrottleChangeForClimbRate(changeRate));
		} else {
			handledAircraft.setStickThrottle(1.0f); // Emergency altitude gain. Might want to wait till not pitched forward to go full throttle though!
		}
		
		float desiredHeading = getDesiredHeading(pos, destination);
		headingChange = getHeadingAdjustment(heading, desiredHeading);
		
		if(avoidanceFactor > 0) {
			desiredPitch *= (1f - avoidanceFactor);	// Scales pitch down towards zero for ascent if needed.
		} else {
			if(pointAtMode) {
				desiredPitch = getDesiredPitchPointAt(pos, destination);  
			} else {
				desiredPitch = getDesiredPitchHelicopter(pos, destination, 0.5f, 0.25f);
			}
		}
		pitchChange = getAngleAdjustment(pitch, desiredPitch);
		
		float desiredRoll = 0;
		rollChange = getAngleAdjustment(roll, desiredRoll);
		
		handledAircraft.setControlStickX(-getStickPos(rollChange, false));
		computePitchYawMovements(roll, headingChange, pitchChange);
		
		if(statsMode) {	// FIXME: TEMP STATS MODE ONLY
			printStats(heading, pitch, roll, desiredHeading, desiredPitch, desiredRoll, pos);
		}
	}
	
	/** Flies the helicopter without necessarily changing heading (by moving sideways and backwards aswell as forwards). 
	 *  Usually used for fine tuning position when very close to destination.*/
	public void pilotHelicopter4Directional( float pitch, float heading, float roll, 
												Vector3f pos,
												float maxTilt, float distanceForMaxTilt) {
		float desiredPitch = 0; float avoidanceFactor = 0;
		if( !ignoreTerrain && (minSafeAltitude > 0) && (map != null) ) {
			avoidanceFactor = getEmergencyTerrainAvoidanceFactor(pos, 2.0f);
			if(avoidanceFactor > 0.8f) avoidanceFactor = 1.0f; // Don't get too close before going vertical!
		}
		
		Vector3f posChange = destination.subtract(pos);
		Vector3f posChangeInLocal = handledAircraft.getAircraftNode().getWorldRotation().inverse().mult(posChange);
		
		float altitudeChange = posChange.y;
		float changeRate = clamp(altitudeChange, -4f, 4f);	// FIXME: Magic number
		if( avoidanceFactor == 0) {
			handledAircraft.moveStickThrottle(calculateThrottleChangeForClimbRate(changeRate));
		} else {
			handledAircraft.setStickThrottle(1.0f); // Emergency altitude gain. Might want to wait till not pitched forward to go full throttle though!
		}
		
		headingChange = 0; // Might want to specify a heading given in a waypoint
		if(avoidanceFactor > 0) {
			desiredPitch *= (1f - avoidanceFactor);	// Scales pitch down towards zero for ascent if needed.
		} else {
			if(pointAtMode) {
				desiredPitch = getDesiredPitchPointAt(pos, destination);
			} else { 
				// Might want to put a minimum adjustment in that is greater than zero to give some stability
				if(FastMath.abs(posChangeInLocal.z) > 0) desiredPitch = -maxTilt * clamp(posChangeInLocal.z / distanceForMaxTilt, -1.0f, 1.0f);
			}
		}		
		pitchChange = getAngleAdjustment(pitch, desiredPitch);
		
		float desiredRoll = 0;
		if(FastMath.abs(posChangeInLocal.x) > 0) desiredRoll = -maxTilt * clamp(posChangeInLocal.x / distanceForMaxTilt, -1.0f, 1.0f);
		rollChange = getAngleAdjustment(roll, desiredRoll);
		
		handledAircraft.setControlStickX(-getStickPos(rollChange, false));
		computePitchYawMovements(roll, headingChange, pitchChange);
		
		if(statsMode) {	// FIXME: TEMP STATS MODE ONLY
			printStats(heading, pitch, roll, 0, desiredPitch, desiredRoll, pos);
		}
	}
	
	// This is a right mess but works quite well. Needs tidying up.
	private void computePitchYawMovements(float roll, float desiredHeadingChange, float desiredPitchChange) {
		// FIXME: Pitch values are backwards for most calculations which is making things quite messy. Might be worth reversing them universally and if needbe just turning them back for display purposes.
		if(FastMath.abs(desiredHeadingChange) < 0.01f) desiredHeadingChange = 0; if(FastMath.abs(desiredPitchChange) < 0.01f) desiredPitchChange = 0;
		if((desiredHeadingChange == 0) && (desiredPitchChange == 0)) {
			handledAircraft.setControlStickY(0);
			handledAircraft.setControlStickZ(0);
			return;
		}
		
		Vector3f pr = new Vector3f(0, handledAircraft.getMaxPitchRate(), 0);	// Represents the change in angle that pulling back on the stick will produce
		Quaternion q = new Quaternion();
		q.fromAngleAxis(roll, Vector3f.UNIT_Z);
		Vector3f fullPitch = q.mult(pr);	// This gives the heading&pitch change we would get from 1s of full pitch at the current roll.
		Vector3f yr = new Vector3f(handledAircraft.getMaxYawRate(), 0, 0);	// Represents the change in angle that pulling right on the stick will produce
		Vector3f fullYaw = q.mult(yr);	// This gives the heading&pitch change we would get from 1s of full yaw at the current roll.
		fullPitch.x = -fullPitch.x; fullYaw.y = -fullYaw.y; // FIXME: Correcting for backwards pitch numbers
		Vector3f fullNegPitch = fullPitch.negate(); Vector3f fullNegYaw = fullYaw.negate();
		
		Vector3f desiredChange = new Vector3f(desiredHeadingChange, desiredPitchChange, 0);
		float posPitchScale = calculateTurnScaling(desiredChange, fullPitch);
		float negPitchScale = calculateTurnScaling(desiredChange, fullNegPitch);
		float posYawScale = calculateTurnScaling(desiredChange, fullYaw);
		float negYawScale = calculateTurnScaling(desiredChange, fullNegYaw);
		
		float posPitchRanking = desiredChange.subtract(fullPitch.mult(posPitchScale)).lengthSquared();
		float negPitchRanking = desiredChange.subtract(fullNegPitch.mult(negPitchScale)).lengthSquared();
		float posYawRanking = desiredChange.subtract(fullYaw.mult(posYawScale)).lengthSquared();
		float negYawRanking = desiredChange.subtract(fullNegYaw.mult(negYawScale)).lengthSquared();
		boolean useNegPitch = false; boolean useNegYaw = false;
		float pitchRanking = posPitchRanking; float yawRanking = posYawRanking;
		if(negPitchRanking < posPitchRanking) {useNegPitch = true; pitchRanking = negPitchRanking;}
		if(negYawRanking < posYawRanking) {useNegYaw = true; yawRanking = negYawRanking;}
		
		boolean pitchPreferred = true; boolean useNegativeCompensator = false;
		float preferredScale = 0;
		Vector3f fullPreferred = null;		// The heading&pitch change we would get from using full preferred axis (whichever that may be)
		Vector3f fullCompensator = null;	// The other axis, which may be used to compensate if the preferred moves one axis the wrong way.
		
		if( pitchRanking <= yawRanking) {	// Pitch axis is the best tool for getting to where we need to be fastest
			fullPreferred = useNegPitch ? fullNegPitch : fullPitch;
			fullCompensator = useNegYaw ? fullNegYaw : fullYaw;
			preferredScale = useNegPitch ? negPitchScale : posPitchScale;
			pitchPreferred = true; useNegativeCompensator = useNegYaw;
			}
		else {								// Yaw axis is the best tool for getting to where we need to be fastest
			fullPreferred = useNegYaw ? fullNegYaw : fullYaw; 
			fullCompensator = useNegPitch ? fullNegPitch : fullPitch;
			preferredScale = useNegYaw ? negYawScale : posYawScale;
			pitchPreferred = false; useNegativeCompensator = useNegPitch;
		}
		
		boolean compensatorNeeded = false;
		if( (fullPreferred.x > 0) != (desiredChange.x > 0) ) {compensatorNeeded = true;}
		if( (fullPreferred.y > 0) != (desiredChange.y > 0) ) {compensatorNeeded = true;}

		float compensatorScale = 1.0f; 
		if(compensatorNeeded) {	// We need to recalculate the scale for the compensator axis to take account of the change in angles caused by the main turn
			// TODO: Would probably be better to calculate the compensator anyway then rate turn with compensator vs without, rather than just deciding on the basis of if the main goes the wrong way.
			// eg. if straight and level with a big desired change in both axes it will only do one when it could do both with no negatives.
			Vector3f adjustedDesiredChange = desiredChange.subtract(fullPreferred.mult(preferredScale)); // This will be the new target
			compensatorScale = calculateTurnScaling(adjustedDesiredChange, fullCompensator);
		}
		float stickPitch = 0; float stickYaw = 0;
		if(pitchPreferred) {
			if(useNegPitch) stickPitch = -negPitchScale; else stickPitch = posPitchScale;
			if(compensatorNeeded) {
				if(useNegativeCompensator) stickYaw = -compensatorScale; else stickYaw = compensatorScale;
			}
		} else {
			if(useNegYaw) stickYaw = -negYawScale; else stickYaw = posYawScale;
			if(compensatorNeeded) {
				if(useNegativeCompensator) stickPitch = -compensatorScale; else stickPitch = compensatorScale;
			}
		}

		handledAircraft.setControlStickY(stickPitch);
		handledAircraft.setControlStickZ(stickYaw);
			
	}
	
	/** Takes two Vector3f's which should hold angles in radians. In x is a heading change and in y is a pitch change.
	 *  This will check if proposedTurn would overshoot desiredTurn in either angle, and if so return a scaling factor to prevent that.*/
	private float calculateTurnScaling(Vector3f desiredTurn, Vector3f proposedTurn) {
		// FIXME: This is sometimes scaling turns down to almost nothing because one is very close even if the other could be improved massively with only a small overshoot on the other.
		float headingScale = 1.0f; float pitchScale = 1.0f;
		if( (proposedTurn.x > 0) == (desiredTurn.x > 0) ) {	// First check it's even going in the same direction
			if(FastMath.abs(proposedTurn.x) > FastMath.abs(desiredTurn.x)) {headingScale = desiredTurn.x / proposedTurn.x; if(headingScale < 0.1f) headingScale = 0.1f;}
		}
		if( (proposedTurn.y > 0) == (desiredTurn.y > 0) ) {	// First check it's even going in the same direction
			if(FastMath.abs(proposedTurn.y) > FastMath.abs(desiredTurn.y)) {pitchScale = desiredTurn.y / proposedTurn.y; if(pitchScale < 0.1f) pitchScale = 0.1f;}
		}
		// TODO: Check this ranking is actually working
		if( (desiredTurn.subtract(proposedTurn.mult(headingScale)).lengthSquared()) <	// Rank the two scales against eachother to choose one. 
			(desiredTurn.subtract(proposedTurn.mult(pitchScale)).lengthSquared()) )
			return headingScale; else return pitchScale;
	}
	
	private float calculateDesiredRoll(float headingChange, float pitchChange) {
		float hc = FastMath.abs(headingChange); float pc = FastMath.abs(pitchChange);
		if(hc < 0.1f) return 0;	// Depending on yaw here, should be smaller if yaw is not a viable option. TODO: Hardcoding.
				
		if(hc > FastMath.HALF_PI) hc = FastMath.HALF_PI; if(pc > FastMath.HALF_PI) pc = FastMath.HALF_PI;
		
		float turnRatio = 1.0f;
		if(pc > hc) {turnRatio = 0.5f * (hc / pc);}	else {turnRatio = 1 - (0.5f * pc / hc);}
		
		float roll = hc * turnRatio;	// This gives a maximum roll as 90 degrees. Adjust for a gentler turn.
		// TODO: This doesn't deal with descending very well, as you will be rolled the wrong way to both turn and pitch with the pitch axis. Not sure if there's a good way to improve that short of rolling upside down.
		if(headingChange < 0) roll = -roll;
		return roll;
	}
	
	private float getDesiredHeading(Vector3f currentPos, Vector3f targetPos) {
		Vector3f travelVect = targetPos.subtract(currentPos);
		Vector3f headingVect = new Vector3f( travelVect.x, 0, travelVect.z ).normalizeLocal();
		float desiredHeading = Vector3f.UNIT_Z.angleBetween(headingVect);
		if(headingVect.x > 0) desiredHeading = FastMath.TWO_PI - desiredHeading;
		return desiredHeading;
	}

	private float getDesiredPitch(Vector3f position, Vector3f destination) {
		if(pointAtMode) return getDesiredPitchPointAt(position, destination);
		return getDesiredPitchAuto(position, destination);
	}
	
	private float getDesiredPitchAuto(Vector3f position, Vector3f destination) {
		float altitudeChange = destination.y - position.y;
		if(FastMath.abs(altitudeChange) < 0.5f) return 0;	// TODO: Hardcoding
		
		float desiredPitch = FastMath.HALF_PI / 2.0f;
		if(FastMath.abs(altitudeChange) < 30.0f) {		// TODO: Hardcoded height at which to start  pitching less than 45 degrees
			desiredPitch *= (FastMath.abs(altitudeChange) / 30f);
		}
		if(desiredPitch < 0.05f) desiredPitch = 0.05f;	// TODO: Hardcoding
		if(altitudeChange < 0) desiredPitch = -desiredPitch;
		return desiredPitch;
	}
	
	private float getDesiredPitchPointAt(Vector3f position, Vector3f destination) {	// TODO: getDesiredPitchPointAt not thoroughly tested
		Vector3f offset = destination.subtract(position);
		Vector3f direction = offset.normalize();
		Vector3f directionCurrentY = new Vector3f(offset.x, 0, offset.z).normalize();
		float pitch = directionCurrentY.angleBetween(direction);
		if(offset.y < 0) pitch = -pitch;
		return pitch;
	}
	
	/** Sprint pitch is the maximum angle that the helicopter will pitch at for maximum forward speed.
	 *  Climb pitch is the maximum angle it will adopt if it wants to climb quickly aswell.*/
	private float getDesiredPitchHelicopter(Vector3f pos, Vector3f destination, float sprintPitch, float climbPitch) {
		float altitudeChange = destination.y - pos.y;
		
		float desiredPitch = -sprintPitch;
		if(slowOnApproach) {	// Don't make two vectors here just do the distanceSquared calculation locally!
			float distToDestSquared = new Vector3f(destination.x, 0, destination.z).distanceSquared(new Vector3f(pos.x, 0, pos.z));
			float falloffDist = FastMath.abs(handledAircraft.getThrust());
			float falloffDistSquared = falloffDist * falloffDist;
			if(distToDestSquared < falloffDistSquared) {
				float distToDest = FastMath.sqrt(distToDestSquared);
				desiredPitch *= distToDest / falloffDist;
			}
		}
		float climbAdjustment = 0;
		if(altitudeChange > 0) climbAdjustment = climbPitch * ( clamp(altitudeChange, 0f, 20f) / 20f);
		if(desiredPitch < (-sprintPitch + climbAdjustment)) desiredPitch = -sprintPitch + climbAdjustment;
		if(desiredPitch > 0) desiredPitch = 0;
		
		return desiredPitch;
	}
	/** Returns the change in angle needed to get from currentAngle to desiredAngle, where all angles are expressed in the range -PI to PI.*/
	private float getAngleAdjustment(float currentAngle, float desiredAngle) {
		float angleChange =  desiredAngle - currentAngle;
		if(angleChange > FastMath.PI) angleChange = -FastMath.TWO_PI + angleChange;
		if(angleChange < -FastMath.PI) angleChange = FastMath.TWO_PI + angleChange;
		return angleChange;
	}

	/** Returns the change in heading needed to get from currentHeading to desiredHeading, where all angles are expressed in the range 0 to 2 PI.*/
	private float getHeadingAdjustment(float currentHeading, float desiredHeading) {
		float headingChange = desiredHeading - currentHeading;
		if(headingChange > FastMath.PI) headingChange -= FastMath.TWO_PI;
		if(headingChange < -FastMath.PI) headingChange += FastMath.TWO_PI;
		return headingChange;
	}
	
	/**This method will only function if a map is available. Zero will always be returned if not.
	 * @return A fraction of 1 representing how extreme avoidance measures need to be. 0 means none needed, 1 means go straight up!
	 * @param lookAheadTime - Number of seconds in the future that it will estimate your course and check the terrain.*/
	private float getEmergencyTerrainAvoidanceFactor(Vector3f pos, float lookAheadTime) {
		if(!ignoreTerrain || (map == null)) return 0;
		
		float avoidanceFactor = 0;
		float alt = getAltitude(pos);
		if(minSafeAltitude > alt) {
			avoidanceFactor = 1 - (alt / minSafeAltitude);
		} else {
			Vector3f projectedPos = pos.add(handledAircraft.getVelocity().mult(lookAheadTime));
			// Currently assumes current altitude at the highest point, which may not be true (highest point may not even be the closest you come to the ground depending on your rate of ascent)
			alt = pos.y - map.getHighestPointBetween(pos, projectedPos, false); 
			if(minSafeAltitude > alt) {
				avoidanceFactor = 1 - (alt / minSafeAltitude);
			}
		}
		return avoidanceFactor;
	}
	
	/** Returns a stick position (-1..1) based on a required change in angle. 
	 *  ie if the angle change is big it will return full lock (-1 or 1), if the angle is small it will be a partial movement.*/
	private float getStickPos(float angleChange, boolean inverted) {
		float stickPos = 0;		
		if(FastMath.abs(angleChange) > 0.01f) {	// TODO: Hardcoding. This means adjust if angle is out by more than about half a degree.
			if(FastMath.abs(angleChange) < 0.2f) {
				stickPos = FastMath.abs(angleChange) / 0.2f;
			} else {
				stickPos = 1.0f;
			}
			if(stickPos < 0.1f) stickPos = 0.1f;
			if(angleChange < 0)	stickPos = -stickPos;
			if(inverted) stickPos = -stickPos; // Aircraft is upside down so reverse the controls.
		}
		return stickPos;
	}

	// Returns true if the aircraft is on its desired heading and pitch, or at least within +- headingVariance and pitchVariance of them.
	public boolean isOnCourse(float headingVariance, float pitchVariance) {
		if( (FastMath.abs(headingChange) <= headingVariance) && (FastMath.abs(pitchChange) <= pitchVariance)) return true;
		return false;
	}
	
	/** For helicopters - the required throttle to achieve desiredClimbRate, based on thrust, weight, lift and current orientation.
	 * desiredClimbRate is in units per second and can be positive, negative or zero.*/
	public float calculateThrottleChangeForClimbRate(float desiredClimbRate) {
		// TODO: This is not very good as it ignores the lift generated by airspeed. Still does ok as it looks at current vertical speed which is affected by that lift.
		Vector3f fullThrust = handledAircraft.getWorldThrustVector().mult(handledAircraft.getThrust());
		float throttleChangeToMaintainAltitude = -handledAircraft.getVelocity().y / fullThrust.y;
		float adjustmentForClimbRate = (desiredClimbRate / fullThrust.y);
		float netThrottleChange = throttleChangeToMaintainAltitude + adjustmentForClimbRate;
		netThrottleChange = clamp(netThrottleChange, -0.1f, 0.1f);
		return netThrottleChange;
	}
	
	public float clamp(float n, float min, float max) {if(n > max) n = max; if(n < min) n = min; return n;}
	
	/* Returns the height of world position pos above the terrain. Returns a negative value if below.*/
	public float getAltitude(Vector3f pos) {
		if(!ignoreTerrain || (map == null)) return 0;
		return pos.y - map.getHeightFromWorld(pos);
	}
	
	private void printStats(float heading, float pitch, float roll,
							float desiredHeading, float desiredPitch, float desiredRoll, Vector3f pos) {
		DebugMethods.text.get(0).print("Pitch: " + Math.round(pitch * FastMath.RAD_TO_DEG) 
				+ "     Roll: " + Math.round(roll * FastMath.RAD_TO_DEG) + "     Heading: " + Math.round(heading * FastMath.RAD_TO_DEG) + 
				" VSI: " + Math.round(handledAircraft.getVelocity().y));
		float groundSpeed = new Vector3f(handledAircraft.getWorldThrustVector().x, 0, handledAircraft.getWorldThrustVector().z).mult(handledAircraft.getThrust() * handledAircraft.getStickThrottle()).length();
		DebugMethods.text.get(1).print("Coordinates: " + Math.round(pos.x) + ", " + Math.round(pos.z) 
				+ "     Altitude: " + Math.round(pos.y) + "     Ground Speed: " + Math.round(groundSpeed)
				+ "     Destination: " + Math.round(destination.x) + ", " + Math.round(destination.z) + " x " + Math.round(destination.y) );
		DebugMethods.text.get(2).print("Heading change: " + Math.round(headingChange * FastMath.RAD_TO_DEG) + 
				"     Pitch change: " + Math.round(pitchChange * FastMath.RAD_TO_DEG) +
				"     Roll change: " + Math.round(rollChange * FastMath.RAD_TO_DEG));
		DebugMethods.text.get(3).print("DPitch: " + Math.round(desiredPitch * FastMath.RAD_TO_DEG) 
				+ "     DRoll: " + Math.round(desiredRoll * FastMath.RAD_TO_DEG) 
				+ "     DHeading: " + Math.round(desiredHeading * FastMath.RAD_TO_DEG) 
				+ "     PointAt Mode: " + pointAtMode);
	}
	
	public void setHelicopterMode(boolean isHelicopter) {
		this.isHelicopter = isHelicopter;
	}
	
	public void setPlayerAssistMode(boolean assist) {
		playerAssistMode = assist;
	}
	
	/* Suspending this pilot ai just sets player assist mode.*/
	public void setSuspend(boolean suspend) {setPlayerAssistMode(suspend);}
	
	public boolean isPlayerAssist() {return playerAssistMode;}
	
	public void cleanup() {
		active = false;
	}

	public void deactivate() {
		active = false;
	}

	public void setMinSafeAltitude(float altitude) {this.minSafeAltitude = altitude;}
	
	/** If this is set to true, the AI will attempt to point the nose directly at the destination point. 
	 * Default is false which means it will try to reach the altitude of the destination and the correct heading, using its own approach.*/
	public void setPointAt(boolean pointAt) {pointAtMode = pointAt;}
	
	protected void setDestination(Vector3f dest) {destination.set(dest);}
	
	public boolean isActive() {return active;}

}
