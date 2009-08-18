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

import java.util.ArrayList;
import java.util.Random;


import com.calefay.exodusDefence.EntityRegister;
import com.calefay.exodusDefence.SimpleTracking;
import com.calefay.exodusDefence.radar.TargetAcquirer;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameRemoveable;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;


public class EDMissileLauncher extends Gun implements GameRemoveable {

	private static final float AUTORELOADTIME = 10.0f;
	
	private boolean active = true;
	private float fireTime = 0;
	private float currentFireTimer = 0;
	private float damage = 0;
	private boolean firing = false;
	
	private RemoveableEntityManager entityManager = null; // TODO: Change to use ReuseableEntityManager
	
	private GameEntity parentEntity = null;
	private Node launcherNode = null;
	private ArrayList<EDSeekingMissile> missiles = null;
	private ArrayList<Vector3f>missileLocs = null;
	private int nextToFire = 0;
	
	private float reloadTimer = 0;
	
	private TargetAcquirer targetAcquirer = null;
	
	private Random rand = null;
	
	/** A missile launcher which has a number of missiles all of which are externally visible before firing.
	 * They are fired in the sequence that the hardpoints are added. Once all are fired the gun will not fire until reloaded.
	 * @param fireDelay - The minimum time in seconds in between missile launches.
	 * @param damage - The damage done by a single missile when it hits.*/
	public EDMissileLauncher(String name, GameEntity parentEntity, float fireDelay, float damage, Node parentNode, RemoveableEntityManager manager) {
		super(name, parentNode);
		this.parentEntity = parentEntity;
		fireTime = fireDelay;
		currentFireTimer = 0;
		active = true;
		firing = false;
		entityManager = manager;
		launcherNode = new Node(name + "launcherNode");
		parentNode.attachChild(launcherNode);
		firePoint = new Vector3f( 0, 0, 0);
		
		this.damage = damage;
		nextToFire = 0;
		missileLocs = new ArrayList<Vector3f>();
		missiles = new ArrayList<EDSeekingMissile>();
		
		rand = new Random();
	}
	
	public EDMissileLauncher(String name, GameEntity parentenEntity, float fireDelay, float damage, Node parentNode, RemoveableEntityManager manager,
							Vector3f... hardPoints) {
		this(name, parentenEntity, fireDelay, damage, parentNode, manager);
		for(Vector3f v : hardPoints) {
			addHardPoint(v);
		}
	}
	
	public EDMissileLauncher(String name, GameEntity parentEntity, float fireDelay, float damage, Node parentNode, Vector3f offset, RemoveableEntityManager manager) {
		this(name, parentEntity, fireDelay, damage, parentNode, manager);
		setFirePoint(offset);
	}
	
	@Override
	public void fire() {
		if(enabled) {
			firing = true;
		}
	}
	
	public void reload() {
		// TODO: Missile should start off with the speed of the launcher
		for(int i = 0; i < missileLocs.size(); i++) {	// TODO: Err does this even work?!
			if(missiles.size() <= i) missiles.add(null);
			if(missiles.get(i) == null) {
				EDSeekingMissile m = null;
				SimpleTracking simpleTracker = new SimpleTracking();
				float speed = 45.0f + (rand.nextFloat() * 10);
				m = new EDSeekingMissile(	name + "Missile", null, simpleTracker, 
						speed, 200.0f, 200.0f, 0f, this.damage, false, 0.2f);
				
				if(parentEntity != null) EntityRegister.getEntityRegister().registerGeometryEntity(m.getProjectileNode(), parentEntity);
				
				m.attachTo(launcherNode);
				m.setPosition(missileLocs.get(i));
				m.setHitEventType("SmallMissileHit");
				missiles.set(i, m);
			}
		}
	}
	
	public void stopFiring() {
		firing = false;
	}

	public boolean isActive() {
		return active;
	}

	public void update(float interpolation) {
		if(reloadTimer > 0) {
			reloadTimer-= interpolation;
			if(reloadTimer <= 0) {reload(); reloadTimer = AUTORELOADTIME;}
		}
		
		if (currentFireTimer > 0) {currentFireTimer -= interpolation;} else if (firing) {launchNext();}
		if(currentFireTimer < 0) {currentFireTimer = 0;}
	}
	
	private void launchNext() {
		currentFireTimer = fireTime;
		EDSeekingMissile m = missiles.get(nextToFire);
		if(m != null) {
			reloadTimer = AUTORELOADTIME;
			Vector3f loc = m.getWorldTranslation();
			Quaternion q = m.getProjectileNode().getWorldRotation();
			m.attachTo(GameWorldInfo.getGameWorldInfo().getRootNode());
			m.setPosition(loc); m.setDirection(q);
			if(targetAcquirer != null) m.launch(targetAcquirer.getTarget()); else m.launch();
			m.addParticleTrail("rocket-slowc-80.jme");
			//m.addParticleTrail("data/effects/rocket-fast-100.jme", entityManager);	// FIXME: Use a smaller particle effect?
			entityManager.add(m);	// Add to manager through an event when launched?
			missiles.set(nextToFire, null);
		}
		nextToFire++;
		if(nextToFire >= missiles.size()) nextToFire = 0;
	}
	
	public void setFirePoint(Vector3f firePoint) {
		super.setFirePoint(firePoint);
		launcherNode.setLocalTranslation(firePoint);
		launcherNode.updateWorldVectors();
	}
	
	/** Adds an extra missile point to the launcher - it must be loaded separately with reload().*/
	public void addHardPoint(Vector3f offset) {
		missileLocs.add(offset);
	}
	
	/** Clears all loaded missiles (and deactivates their objects).*/
	public void unloadMissiles() {
		for(MissileProjectile m : missiles) {
			if(m != null) m.deactivate();
		}
	}
	
	@Override
	public void setTargetting(TargetAcquirer acquirer) {
		this.targetAcquirer = acquirer;
	}
	
	public void deactivate() {
		if(missiles != null) unloadMissiles();
		if(launcherNode != null) launcherNode.removeFromParent();
		active = false;
		firing = false;
	}
	
	public void cleanup() {
		super.cleanup();
		active = false;
		firing = false;
		
		if(missiles != null) unloadMissiles(); missiles = null;
		if(launcherNode != null) launcherNode.removeFromParent(); launcherNode = null;
		missileLocs = null;
		
		parentEntity = null;
		targetAcquirer = null;
		rand = null;
		
		entityManager = null;
	}
	
}
