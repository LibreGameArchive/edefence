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
import com.jme.intersection.PickData;
import com.jme.intersection.PickResults;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.Ray;
import com.jme.math.Vector3f;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial.CullHint;

/** Represents a weapon that is either on or off, and does damage to anything straight in front of it. Typically a laser beam.
 * BaseBeamWeapon has no graphical representation, but does do collision detection, hit event reporting and keeps track of it's length.
 * @author James Waddington
 *
 */
public class BaseBeamWeapon implements GameRemoveable {
	
	private String name = null;
	
	protected Node source = null;
	protected Node beamNode = null;
	protected Geometry objectHit = null;

	public boolean isBlocked = false;
	protected boolean isOn = false;
	protected boolean checkForHits = true;
	protected boolean registerHits = false;
	
	protected boolean active = true;
	protected boolean deactivating = false;
	
	protected float maxLength = 1000.0f;
    protected float length = 1.0f;
    protected float damagePerSecond = 0.0f;
    
    protected String hitEventType;
    
    private Ray collisionRay = null;
	private PickResults results = null;
    
	public BaseBeamWeapon(String newName, Node src) {
		// src is the object that the beam will be attached to. trg it will be pointed at. offset is the offset from src that it will start from. 
		source = src;
		name = newName;
		
		beamNode = new Node(name + "Node");
		beamNode.setIsCollidable(false);
		hitEventType = "BeamHit";
		
		isOn = false;
		active = true;
		deactivating = false;
		
		checkForHits = true;
		registerHits = false;
		objectHit = null;
		maxLength = 1000.0f;
		damagePerSecond = 0;
		source.attachChild(beamNode);
		beamNode.setCullHint(CullHint.Always);
		
		collisionRay = new Ray();
		results = new TrianglePickResults();
		results.setCheckDistance(true);
	}
	
	public BaseBeamWeapon(String newName, Node src, Vector3f offset) {
		this(newName, src);
		beamNode.setLocalTranslation(offset);	// Not sure this works when it hasn't been attached?
	}
	
	public BaseBeamWeapon(String newName, Node src, Vector3f offset, float dps) {
		this(newName, src, offset);
		damagePerSecond = dps;
	}
	
	public void update(float interpolation) {
		objectHit = null; isBlocked = false;
		if(isOn) {
			if (checkForHits) checkHits(); else isBlocked = false;
			if((objectHit != null) && registerHits) {
				// FIXME Queue name should not be hardcoded.
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(hitEventType,  "default", this, objectHit);
			}
		}	
	}
	
	public void checkHits() {
		collisionRay.setOrigin(beamNode.getWorldTranslation()); collisionRay.setDirection(beamNode.getWorldRotation().getRotationColumn(2));
		GameWorldInfo.getGameWorldInfo().getRootNode().findPick(collisionRay,results);
		isBlocked = false;
		length = maxLength;
		
		for (int i = 0; (i < results.getNumber() ) && !isBlocked; i++) {
			
			PickData picked = results.getPickData(i);
			objectHit = picked.getTargetMesh();
			
			isBlocked = true;
			
			length = picked.getDistance();
			if (length > maxLength) {
				length = maxLength; 
				objectHit = null;
				isBlocked = false;}
		}
		
		results.clear();
	}
	
	/** Turn the beam on.*/
	public void setOn() {
		if (!isOn && !deactivating) {
			beamNode.setCullHint(CullHint.Never);
		}
		isOn = true;
	}
	
	/** Turn the beam off.*/
	public void setOff() {
		if (isOn && active) {
			beamNode.setCullHint(CullHint.Always);
		}
		isOn = false;
	}

	public void setOffset(Vector3f offset) {
		beamNode.setLocalTranslation(offset);
	}
	
	public Geometry getHit() {	// Only valid until next update. Might be worth getting rid of now that the target is passed in hit event.
		return objectHit;
	}
	
	public String getName() {return name;}
	public boolean hasHit() {return (objectHit == null ? false:true);}
	public boolean isOn() {return isOn;}
	public void setSource(Node s) {source = s;}
	public void setCheckForHits(boolean b) {checkForHits = b;}
	public boolean getCheckForHits() {return checkForHits;}
	/** Note that this will be overwritten every frame if checkForHits is true (default).*/
	public void setLength(float l) {length = l;}
	public void setDamagePerSecond(float dps) {damagePerSecond = dps;}
	public float getDamagePerSecond() {return damagePerSecond;}
	
	public void setRegisterHits(boolean h) {registerHits = h;}
	
	/** Sets the event String set to the event manager when the beam hits something. Default: "BeamHit"*/
	public void setHitEventType(String e) {
		hitEventType = e;
	}
	
	public float getLength() {
		return length;
	}
	
	/** Signal that this object is no longer needed and can be deleted by the entity manager. NOT deactivate the beam.
	 * @see setOff()*/
	public void deactivate() {
		if(!deactivating) {
			setOff();
			deactivating = true;
			active = false;
			cleanup();		// Possibly this should not be done until after everything has seen it's inactivity.
		}
	}
	
	/** Active status in regard to removable entity status. NOT the active status of the laser beam. 
	 * @see isOn()*/
	public boolean isActive() {
		return active;
	}
	
	/** Releases all references and data that this is holding on to. It ceases to be useable.*/
	public void cleanup() {
		if(beamNode != null) {beamNode.removeFromParent(); beamNode = null;}
		objectHit = null;
		source = null;
		active = false;
		hitEventType = null;
	}
	
	/** Sets the length of the beam in the event that no htis are detected, or if the target is further away than maxLength.*/
	public void setMaxLength(float l) {
		maxLength = l;
	}
	
}
