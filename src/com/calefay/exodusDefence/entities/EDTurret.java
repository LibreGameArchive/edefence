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


import com.calefay.exodusDefence.EDFactory;
import com.calefay.exodusDefence.PlayableEntity;
import com.calefay.exodusDefence.radar.EDTriangleZoneRadar;
import com.calefay.exodusDefence.radar.TargetAcquirer;
import com.calefay.exodusDefence.weapons.Gun;
import com.calefay.utils.AttributeSet;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameRemoveable;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;


public class EDTurret implements PlayableEntity, GameRemoveable {
	
	private boolean active = false;
	
	private String name;
	private String faction = "default";
	private float maxHitPoints = 0, hitPoints = 0;
	private boolean dead = false;
	
	private Node structureNode = null;
	private Node gunTurretNode = null;
	private Node gunMountingNode = null;
	private Node gunBarrel1Node = null;
	private Node gunBarrel2Node = null;
	
	private Gun primaryGunBarrel1 = null;
	private Gun primaryGunBarrel2 = null;
	private Gun secondaryGun = null;
	private Vector3f primaryB1Firepoint = null, primaryB2Firepoint = null;
	private Vector3f secondaryFirepoint = null;
	
	private EDTriangleZoneRadar radar = null;
	
	private float elevation = 0;
	private Quaternion rot = null;
	
	public EDTurret(String turretName) {
	
		active = true;
		
		name = turretName;
		maxHitPoints = 1;
		setHealth(1);
		dead = false;
		rot = new Quaternion();
		
		primaryGunBarrel1 = null;
		primaryGunBarrel2 = null;
		secondaryGun = null;
		
		elevation = 0;
		
		structureNode = new Node(turretName + "StructureNode");
		gunTurretNode = new Node(turretName + "GunTurretNode");
		gunMountingNode = new Node(turretName + "GunMountingNode");
		gunBarrel1Node = new Node(turretName + "GunBarrel1Node");
		gunBarrel2Node = new Node(turretName + "GunBarrel2Node");
	}
	
	public void setStats(float hitPoints) {
		this.maxHitPoints = hitPoints; this.hitPoints = hitPoints;
	}
	
	public boolean handleAction(GameEvent action) {
		if(action.getEventType() == "damage") {
			float damageAmount = (Float)action.getEventTarget();
			applyDamage(damageAmount);
			return true;
		} else {return false;}
	}
	
	/** Adds the specified number of hitpoints. Brings back guns and reactivates if it was destroyed.*/
	public void repair(float hp) {
		applyDamage(-hp);
		if(hitPoints > maxHitPoints) {setHealth(maxHitPoints);}
		if( (hitPoints > 0) && dead) {reactivate();}
	}
	
	public void reactivate() {
		dead = false;
		
		if(primaryGunBarrel1 != null) primaryGunBarrel1.enable();
		if(gunBarrel1Node.getParent() == null) {gunMountingNode.attachChild(gunBarrel1Node);}
		
		if(secondaryGun != null) secondaryGun.enable();
		
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("removeDamageSmoke",  "visualEffects", this, null);
	}
	
	public void spin(float angle) {
		if(dead) return;
		
		rot.fromAngleNormalAxis(angle, Vector3f.UNIT_Y);
		gunTurretNode.getLocalRotation().multLocal(rot);
		gunTurretNode.getLocalRotation().normalize(); // This is to combat degradation caused by constant tiny rotations
	}
	
	public void elevate(float angle) {
		if(dead) return;
		
		elevation += angle;
		if(elevation < -FastMath.HALF_PI) {elevation = -FastMath.HALF_PI;}
		if(elevation > (FastMath.HALF_PI / 2)) {elevation = (FastMath.HALF_PI / 2);}
		rot.fromAngleNormalAxis(elevation, Vector3f.UNIT_X);
		gunMountingNode.getLocalRotation().set(rot);
	}
	
	public void stripUpgrades() {downgradeSingle();}
	
	public void upgradeDouble() {
		gunMountingNode.attachChild(gunBarrel2Node);
		gunBarrel1Node.getLocalTranslation().set(new Vector3f(-0.5f, 0.0f, 0.0f));
		gunBarrel2Node.getLocalTranslation().set(new Vector3f(0.5f, 0.0f, 0.0f));
		//primaryGunBarrel2.enable();	// FIXME: Turret barrels need a rethink!
	}
	
	private void downgradeSingle() {
		if(gunBarrel2Node != null) {gunBarrel2Node.removeFromParent();}
		if(primaryGunBarrel2 != null) primaryGunBarrel2.disable();
		gunBarrel1Node.getLocalTranslation().set(new Vector3f(0.0f, 0.0f, 0.0f));
	}
	
	protected void setHealth(float health) {
		if(health > maxHitPoints) hitPoints = maxHitPoints; else hitPoints = health;
		if( (hitPoints <= 0) && !dead) makeDead();
	}
	
	protected void applyDamage(float damage) {
		hitPoints -= damage;
		if( (hitPoints <= 0) && !dead) makeDead();
		if(hitPoints < 0) hitPoints = 0;
		if( (hitPoints > 0) && dead) {reactivate();}
	}
	
	protected void makeDead() {
		if( (hitPoints <= 0) && !dead) {
			dead = true;
			
			if(primaryGunBarrel1 != null) primaryGunBarrel1.disable();
			gunBarrel1Node.removeFromParent();
			if(primaryGunBarrel2 != null) primaryGunBarrel2.disable();
			if(gunBarrel2Node != null) gunBarrel2Node.removeFromParent();
			
			if(secondaryGun != null) secondaryGun.cleanup(); secondaryGun = null;	// Secondary weapon is removed.
			
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("TurretDamaged",  "gameplayEvents", this, null);
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("addDamageSmoke",  "visualEffects", this, null);
			// May want to send an event back to clear the fire keys as they will be set up fresh if the turret is repaired.
			// May want to disable input handler when dead so as not to waster CPU time.
		}
	}

	/** Sets the guns attached to the primary barrels. Null is valid.
	 * Note that this will mark the object being replaced for deletion., if it is a GameRemoveable*/
	public void setPrimaryGun(	AttributeSet barrel1Template, AttributeSet barrel2Template,
			GameResourcePack gameResources, RemoveableEntityManager entityManager) {
		Gun barrel1 = EDFactory.buildGun(getName() + "Barrel1", getBarrelNode1(), this,
				barrel1Template, gameResources, entityManager, radar);
		Gun barrel2 = EDFactory.buildGun(getName() + "Barrel1", getBarrelNode2(), this,
				barrel2Template, gameResources, entityManager, radar);
		
		if(primaryGunBarrel1 instanceof GameRemoveable) {
			GameRemoveable b1 = (GameRemoveable)primaryGunBarrel1;
			b1.deactivate();
		}
		
		primaryGunBarrel1 = barrel1;
		if( (primaryGunBarrel1 != null) && (primaryB1Firepoint != null) ) primaryGunBarrel1.setFirePoint(primaryB1Firepoint);
		
		if(primaryGunBarrel2 instanceof GameRemoveable) {
			GameRemoveable b2 = (GameRemoveable)primaryGunBarrel2;
			b2.deactivate();
		}
		primaryGunBarrel2 = barrel2;
		if((primaryGunBarrel2 != null) && (primaryB2Firepoint != null)) primaryGunBarrel2.setFirePoint(primaryB2Firepoint);
	}
		
	/** Sets the secondary Gun. Note that this will mark the object being replaced for deletion if it is a GameRemoveable.*/
	public void setSecondaryGun(AttributeSet template, GameResourcePack gameResources, RemoveableEntityManager entityManager) {
		if(secondaryGun instanceof GameRemoveable) {
			GameRemoveable sg = (GameRemoveable)secondaryGun;
			sg.deactivate();	// TODO: Guns should possibly be cleaned up directly as they may not be part of an entity manager.
		}
		
		Gun secondary = EDFactory.buildGun(getName() + "SecondaryGun", getTurretNode(), this, template, gameResources, entityManager, getRadar());
		if( (secondaryGun != null) && (secondaryFirepoint != null) ) secondaryGun.setFirePoint(secondaryFirepoint);
		secondaryGun = secondary;
	}
	
	/* Specifies where each gun is positioned in relation to the barrelNode.
	 * @param firepoints - an array of locations for the guns in this order: primary barrel 1, primary barrel 2, secondary.*/
	public void setFirepoints(Vector3f[] firepoints) {
		if(firepoints.length > 0) primaryB1Firepoint = firepoints[0];
		if(firepoints.length > 1) primaryB2Firepoint = firepoints[1];
		if(firepoints.length > 2) secondaryFirepoint = firepoints[2];
	}
	
	public void addTargetAcquisition(EDTriangleZoneRadar radar) {this.radar = radar;}
	public void setRadarEnabled(boolean on) {
		if(radar == null) return;
		if(on) radar.startScanning(); else radar.stopScanning();
	}
	
	public void setPosition(Vector3f pos) {getStructureNode().getLocalTranslation().set(pos); getStructureNode().updateWorldVectors();}
	public TargetAcquirer getRadar() {return radar;}
	public Node getTurretNode() {return gunTurretNode;}
	public Node getMountingNode() {return gunMountingNode;}
	public Node getBarrelNode1() {return gunBarrel1Node;}
	public Node getBarrelNode2() {return gunBarrel2Node;}
	public Node getStructureNode() {return structureNode;}
	public Node getCameraTrackNode() {return getMountingNode();}
	public Gun getPrimaryGunBarrel1() {return primaryGunBarrel1;}
	public Gun getPrimaryGunBarrel2() {return primaryGunBarrel2;}
	public Gun getSecondaryGun() {return secondaryGun;}
	public float getHitpoints() {return hitPoints;}
	public ControlsType getControlsType() {return ControlsType.TURRET;}
	
	/* Dummy implementation to satisfy GameRemoveable interface ONLY (so that it can be added to a manager). Does nothing.*/
	public void update(float interpolation) {}
	
	public void deactivate() {active = false;}
	
	public void cleanup() {
		active = false;
		
		if(structureNode != null) {structureNode.removeFromParent(); structureNode = null;}
		if(gunTurretNode != null) {gunTurretNode.removeFromParent(); gunTurretNode = null;}
		if(gunMountingNode != null) {gunMountingNode.removeFromParent(); gunMountingNode = null;}
		if(gunBarrel1Node != null) {gunBarrel1Node.removeFromParent(); gunBarrel1Node = null;} 
		if(gunBarrel2Node != null) {gunBarrel2Node.removeFromParent(); gunBarrel2Node = null;}
		
		if(primaryGunBarrel1 != null) {primaryGunBarrel1.cleanup(); primaryGunBarrel1 = null;} 
		if(primaryGunBarrel2 != null) {primaryGunBarrel2.cleanup(); primaryGunBarrel2 = null;} 
		if(secondaryGun != null) {secondaryGun.cleanup(); secondaryGun = null;}
		
		primaryB1Firepoint = null; primaryB2Firepoint = null; secondaryFirepoint = null;
		
		name = null;
		hitPoints = 0;
		rot = null; elevation = 0;
		dead = true;
	}
	
	public String getName() {return name;}
	
	// GameEntity implemented methods begin
	public Vector3f getPosition() {return structureNode.getWorldTranslation();}
	public Quaternion getRotation() {return structureNode.getWorldRotation();}
	public Vector3f getVelocity() {return Vector3f.ZERO;}
	public float getMaxHitpoints() {return maxHitPoints;}
	public boolean isDead() {return dead;}
	public boolean isActive() {return active;}
	public String getFaction() {return faction;}
	public void setFaction(String faction) {this.faction = faction;}
	// GameEntity implemented methods end;
	
}
