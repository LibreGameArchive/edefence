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


import com.calefay.utils.GameReuseable;
import com.calefay.utils.GameWorldInfo;
import com.jme.intersection.BoundingPickResults;
import com.jme.intersection.PickData;
import com.jme.intersection.PickResults;
import com.jme.intersection.TrianglePickResults;
import com.jme.math.Quaternion;
import com.jme.math.Ray;
import com.jme.math.Vector3f;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jmex.terrain.TerrainBlock;

public class BaseProjectile extends GameReuseable {

	private static Vector3f projectileBoundsMin, projectileBoundsMax = new Vector3f();
	
	protected float projectileSpeed = 0;
	private float projectileLifeTime = 0;
	
	private boolean checkForHits = true;
	private Geometry objectHit = null;
	private Ray travelRay = null;
	private PickResults boundingResults = null;
	private PickResults triangleResults = null;
	
	protected String name = null;
	protected String hitEventType = null;
	protected String outOfBoundsEventType = null;
	protected String deactivatingEventType = null;
	
	protected Node projectileNode = null;
	protected Vector3f projectileDirection = null;
	protected Vector3f projectileVelocity = null;
	
	private float projectileDamage = 0;
	
	public BaseProjectile(String newName, Vector3f pos, Quaternion orientation,
							float speed, float lifeTime, float damage) {
		
		projectileNode = new Node();
		projectileDirection = new Vector3f();
		projectileVelocity = new Vector3f();
		
		travelRay = new Ray();
		boundingResults = new BoundingPickResults();
		boundingResults.setCheckDistance(true);
		triangleResults = new TrianglePickResults();
		triangleResults.setCheckDistance(true);
		
		if((projectileBoundsMin == null) || (projectileBoundsMax == null)) {
			setBounds(null, null);	// Null parameters will cause it to use defaults.
		}
		
		reset( newName, pos, orientation, speed, lifeTime, damage);
	}
	
	/** Basic implementation to set active and add back into the world. Mainly for internal use - would almost always supply more parameters */
	protected void reset() {
		active = true;
		deactivating = false;
		checkForHits = true;
		hitEventType = "ProjectileHit";
		outOfBoundsEventType = null; //"ProjectileOOB";
		deactivatingEventType = null;
		
		GameWorldInfo.getGameWorldInfo().getRootNode().attachChild(projectileNode);	// FIXME: Get rid of this in favor of attachTo()
		projectileNode.updateWorldVectors();
	}

	/** used by the manager class to reset when recycled. */
	protected void reset(String newName, Vector3f pos, Quaternion orientation, float speed, float lifeTime, float damage) {
		name = newName;
		projectileNode.setName(name + "ProjectileNode");

		projectileSpeed = speed;
		projectileLifeTime = lifeTime;
		projectileDamage = damage;
		
		if(pos != null) projectileNode.getLocalTranslation().set(pos);
		if(orientation != null) projectileNode.getLocalRotation().set(orientation);
		
		reset();
	}

	public void update(float interpolation) {
		if(deactivating) {
			active = false;
			cleanup();
		}
		if(active) {
			if (projectileLifeTime > 0) {	// initial life of 0 means it is not a timed life projectile.
				projectileLifeTime -= interpolation;
				if (projectileLifeTime <= 0) {		// ie. it just died
					projectileLifeTime = 0;
					onEndOfLifeTime();
				}
			}
			objectHit = null;
			if (checkForHits) checkHits(interpolation);
			if(objectHit != null) {
				onHit();
			}
			//FIXME: This whole approach of taking a node direction then getting velocity from it etc. is dodgy. Only really appropriate for missiles. Really travel direction and alignment should be somewhat independant.
			// FIXME: Would prefer not to rely on the node as an integral part of the projectile's motion - ideally have it just an attach point for visuals.
			projectileDirection.set(projectileNode.getLocalRotation().getRotationColumn(2));	// FIXME: Don't want to do a getRotationColumn every update in BaseProjectile!
			projectileVelocity.set(projectileDirection.mult(projectileSpeed));
			projectileNode.getLocalTranslation().addLocal( 
						projectileVelocity.mult(interpolation) );
			if(projectileBoundsMin != null) {checkBounds();}
		}
		
	}
		
	public void deactivate() {
		if(!deactivating) {
			deactivating = true;
			if(projectileNode != null) {projectileNode.removeFromParent();}
			if(deactivatingEventType != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(deactivatingEventType,  "default", this, null);
		}
	}
	
	/** Nulls all object references, detatches all nodes and geometry from their parents. Called during deactivation*/
	public void cleanup() {
		if(projectileNode != null) {
			projectileNode.removeFromParent();
			projectileNode.detachAllChildren(); 
			 
			projectileNode = null;
		}
		
		objectHit = null;
		travelRay = null;
		name = null;
		hitEventType = null;
		outOfBoundsEventType = null;
		projectileDirection = null;
	}
	
	public void checkHits(float interpolation) {
		// Currently using a ray to ensure that anything in the projectile's path is detected, triangle collision might not work at high speeds.
		travelRay.setOrigin(projectileNode.getWorldTranslation());
		travelRay.setDirection(projectileNode.getWorldRotation().getRotationColumn(2));
		
		GameWorldInfo.getGameWorldInfo().getRootNode().findPick(travelRay,boundingResults);
			
		objectHit = null;
		for(int i = 0; (i < boundingResults.getNumber()) && (objectHit == null); i++) {
			PickData picked = boundingResults.getPickData(i);
			float travelDist = projectileSpeed * interpolation;
			if (picked.getDistance() <= travelDist) {
				objectHit = picked.getTargetMesh();
				if(objectHit instanceof TerrainBlock) objectHit = checkTriangleHits(objectHit, travelRay, travelDist);
				if(objectHit != null) if(!confirmHit(objectHit)) objectHit = null;
			}

		}
		
		boundingResults.clear();
	}
	
	/* Subclass can override in order to check if a hit should be ignored.*/
	protected boolean confirmHit(Geometry geom) {
		return true;
	}
	
	private Geometry checkTriangleHits(Spatial s, Ray r, float travelDist) {
		s.findPick(r, triangleResults);
		PickData picked = null;
		if(triangleResults.getNumber() > 0) picked = triangleResults.getPickData(0);
		triangleResults.clear();
		
		if( (picked == null) || (picked.getDistance() > travelDist) ) return null; else return picked.getTargetMesh();
	}
	
	protected void onHit() {
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(hitEventType, "default", this, objectHit);
		deactivate();
	}
	
	private void checkBounds() {
		if ( 	(projectileNode.getWorldTranslation().x > projectileBoundsMax.x) ||
				(projectileNode.getWorldTranslation().y > projectileBoundsMax.y) ||
				(projectileNode.getWorldTranslation().z > projectileBoundsMax.z) ||
				(projectileNode.getWorldTranslation().x < projectileBoundsMin.x) || 
				(projectileNode.getWorldTranslation().y < projectileBoundsMin.y) ||
				(projectileNode.getWorldTranslation().z < projectileBoundsMin.z) ) {
			onOutOfBounds();
		}
	}
	
	protected void onOutOfBounds() {
		if(outOfBoundsEventType != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(outOfBoundsEventType,  "default", this, null);
		deactivate();
	}
	
	protected void onEndOfLifeTime() {
		if(outOfBoundsEventType != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(outOfBoundsEventType,  "default", this, null);
		deactivate();
	}
	
	public static void setBounds(Vector3f min, Vector3f max) {
		if(projectileBoundsMin == null) {projectileBoundsMin = new Vector3f(-100f, -100f, -100f);}
		if(projectileBoundsMax == null) {projectileBoundsMax = new Vector3f( 100f, 100f, 100f);}
		projectileBoundsMin.set(min);
		projectileBoundsMax.set(max);
	}
	
	public Node getProjectileNode() {
		// TODO might want to get rid of this method
		return projectileNode;
	}
	
	public Vector3f getProjectileDirection() {return projectileDirection;}

	/** Returns velocity in units per SECOND*/
	public Vector3f getProjectileVelocity() {return projectileVelocity;}
	
	public Vector3f getWorldTranslation() {if(projectileNode != null) return projectileNode.getWorldTranslation(); else return null;}

	public float getProjectileSpeed() {
		return projectileSpeed;
	}
	
	/** Attaches the projectile node to a new parent, and updates it's position.*/
	public void attachTo(Node parent) {
		parent.attachChild(projectileNode);
	}
	
	/** Sets the position of the projectile relative to it's parent node. Node that it copies the values given and does not retain a reference to the Vector. */
	public void setPosition(Vector3f pos) {
		projectileNode.getLocalTranslation().set(pos);
	}
	
	public void setDirection(Quaternion q) {
		projectileNode.setLocalRotation(q);
		projectileDirection = q.getRotationColumn(2);
	}
	
	/** Setting to false will prevent the normal collision check from being done. Does NOT set the geometry's collidable to false. Default true.*/
	public void setCheckForHits(boolean check) {
		checkForHits = check;
	}
	
	/** Sets the event String sent to the event manager when the projectile hits something. Default: "ProjectileHit"*/
	public void setHitEventType(String e) {hitEventType = e;}
	/* Sets the type of event sent to the event manager when the projectile goes out of bounds OR reaches the end of it's life, if timed.*/
	public void setOOBEventType(String e) {outOfBoundsEventType = e;}
	/* Sets the type of event sent to the event manager when the projectile is actually deactivating.*/
	public void setDeactivatingEventType(String e) {deactivatingEventType = e;}
	
	public Geometry getHit() {
		return objectHit;
	}
	
	public float getDamage() {
		return projectileDamage;
	}
	
	public String getName() {
		return name;
	}
	
	public String toString() {
		return name;
	}
}
