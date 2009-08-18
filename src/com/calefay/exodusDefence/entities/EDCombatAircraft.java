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

import java.util.ArrayList;

import com.calefay.effects.ManagedParticleEffect;
import com.calefay.exodusDefence.EDFactory;
import com.calefay.exodusDefence.EntityRegister;
import com.calefay.exodusDefence.PlayableEntity;
import com.calefay.exodusDefence.radar.TargetAcquirer;
import com.calefay.exodusDefence.weapons.BaseProjectile;
import com.calefay.exodusDefence.weapons.Gun;
import com.calefay.utils.AttributeSet;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameRemoveable;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.jme.intersection.BoundingCollisionResults;
import com.jme.intersection.CollisionResults;
import com.jme.math.Vector3f;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jmex.terrain.TerrainBlock;


public class EDCombatAircraft extends EDAircraft implements PlayableEntity {

	private TargetAcquirer radar = null;

	private Vector3f primaryB1Firepoint = null, primaryB2Firepoint = null;
	private Vector3f secondaryFirepoint = null;
	
	private Gun primaryGunBarrel1 = null;
	private Gun primaryGunBarrel2 = null;
	private ManagedParticleEffect thruster1Effect = null;
	private ManagedParticleEffect thruster2Effect = null;
	private ControlsType controlsType = ControlsType.FIXEDWING;
	
	private CollisionResults collisionResults = null;
	private ArrayList<Geometry> collisionContacts = null;
	
	/* NOTE: Defaults to fixed wing controls type.*/
	public EDCombatAircraft(String name) {
		super(name);
		controlsType = ControlsType.FIXEDWING;
		aircraftNode = new Node(name + "AircraftNode");
		
		collisionResults = new BoundingCollisionResults();
		collisionContacts = new ArrayList<Geometry>();
	}
	
	/* Specifies where each gun is positioned in relation to the aircraftNode.
	 * @param firepoints - an array of locations for the guns in this order: primary barrel 1, primary barrel 2, secondary.*/
	public void setFirepoints(Vector3f[] firepoints) {
		if(firepoints.length > 0) primaryB1Firepoint = firepoints[0];
		if(firepoints.length > 1) primaryB2Firepoint = firepoints[1];
		if(firepoints.length > 2) secondaryFirepoint = firepoints[2];
	}
	
	/** Sets the guns attached to the primary barrels. Null is valid.
	 * Note that this will mark the object being replaced for deletion., if it is a GameRemoveable*/
	public void setPrimaryGun(	AttributeSet barrel1Template, AttributeSet barrel2Template,
								GameResourcePack gameResources, RemoveableEntityManager entityManager) {

		// First deactivate the old guns:
		if(primaryGunBarrel1 instanceof GameRemoveable) {
			GameRemoveable b1 = (GameRemoveable)primaryGunBarrel1;
			b1.deactivate();
		}
		if(primaryGunBarrel2 instanceof GameRemoveable) {
			GameRemoveable b2 = (GameRemoveable)primaryGunBarrel2;
			b2.deactivate();
		}

		// Then build the new ones (null is a valid return)
		Gun barrel1 = EDFactory.buildGun(getName() + "Barrel1", getAircraftNode(), this, 
				barrel1Template, gameResources, entityManager, radar);
		Gun barrel2 = EDFactory.buildGun(getName() + "Barrel1", getAircraftNode(), this,
				barrel2Template, gameResources, entityManager, radar);
				
		if( (barrel1 != null) && (primaryB1Firepoint != null)) barrel1.setFirePoint(primaryB1Firepoint);
		primaryGunBarrel1 = barrel1;
		if(primaryGunBarrel1 != null) primaryGunBarrel1.setTargetting(radar);
		
		if( (barrel2 != null) && (primaryB2Firepoint != null) ) barrel2.setFirePoint(primaryB2Firepoint);	
		primaryGunBarrel2 = barrel2;
		if(primaryGunBarrel2 != null) primaryGunBarrel2.setTargetting(radar);
	}
	
	/** Placeholder - currently EDCombatAircrafts cannot have a secondary weapon. */
	public void setSecondaryGun(AttributeSet template, GameResourcePack gameResources, RemoveableEntityManager entityManager) {
		return;
	}
	
	/** Not currently used for EDCombatAircraft.*/
	public void upgradeDouble() {return;}
	
	public void setTargetting(TargetAcquirer radar) {
		this.radar = radar;
		if(primaryGunBarrel1 != null) primaryGunBarrel1.setTargetting(radar);
		if(primaryGunBarrel2 != null) primaryGunBarrel2.setTargetting(radar);
	}
	
	public void addParticleTrail(ManagedParticleEffect effect1, ManagedParticleEffect effect2) {
		thruster1Effect = effect1; thruster2Effect = effect2;
	}
	
	public void update(float interpolation) {
		super.update(interpolation);
		checkCollisions(interpolation);
	}
	
	public void checkCollisions(float interpolation) {
		collisionResults.clear(); collisionContacts.clear();
		Node root = GameWorldInfo.getGameWorldInfo().getRootNode();
		
		getAircraftNode().findCollisions(root, collisionResults);
		
		Geometry target = null;
		GameEntity targettEntity = null;
		
		for(int i = 0; i < collisionResults.getNumber(); i++) {
			target = collisionResults.getCollisionData(i).getTargetMesh();
			targettEntity = null; targettEntity = EntityRegister.getEntityRegister().lookupEntity(target);
			
			if( (targettEntity == null) || ((targettEntity != this) && !(targettEntity instanceof BaseProjectile)) ) {
				EntityRegister.getEntityRegister().lookupEntity(target);
				if(!collisionContacts.contains(target)) {collisionContacts.add(target);}
			}	
		}
		
		for(Geometry g : collisionContacts) {
			if( !(g instanceof TerrainBlock) || (checkTerrainCollision((TerrainBlock)g)) ) {
				handleAction(new GameEvent("damage", null, 3000f * interpolation));	// Hardcoded action/damage, should depend on type of collision etc.
			}
		}

	}
	
	public boolean checkTerrainCollision(TerrainBlock target) {
		float altitude = getAircraftNode().getWorldTranslation().y - target.getHeightFromWorld(getAircraftNode().getWorldTranslation());

		if((altitude < 5f) && getAircraftNode().hasCollision(target, true)) return true;		// FIXME: Hardcoded altitude threshold.
		return false;
	}
	
	public void setControlsType(ControlsType t) {controlsType = t;}
	
	public TargetAcquirer getRadar() {return radar;}
	public Gun getPrimaryGunBarrel1() {return primaryGunBarrel1;}
	public Gun getPrimaryGunBarrel2() {return primaryGunBarrel2;}
	public Gun getSecondaryGun() {return null;}
	public Node getCameraTrackNode() {return getAircraftNode();}
	public ControlsType getControlsType() {return controlsType;}
	public void stripUpgrades() {return;}
	
	public void deactivate() {
		super.deactivate();
		if(radar != null) radar.deactivate();
		EntityRegister.getEntityRegister().deRegisterGeometryEntity(aircraftNode);
		if(thruster1Effect != null) thruster1Effect.deactivate();
		if(thruster2Effect != null) thruster2Effect.deactivate();
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		if(getPrimaryGunBarrel1() != null) getPrimaryGunBarrel1().cleanup();
		if(getPrimaryGunBarrel2() != null) getPrimaryGunBarrel2().cleanup();
		
		if(radar != null) radar.deactivate(); radar = null;
		primaryGunBarrel1 = null; primaryGunBarrel2 = null;
		thruster1Effect = null; thruster2Effect = null;
		
		if(collisionResults != null) collisionResults.clear(); collisionResults = null;
		if(collisionContacts != null) collisionContacts.clear(); collisionContacts = null;
	}
}
