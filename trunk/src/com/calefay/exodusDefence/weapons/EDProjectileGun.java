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


import com.calefay.effects.ManagedParticleEffect;
import com.calefay.utils.GameRemoveable;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.jme.math.Vector3f;
import com.jme.scene.Node;


public class EDProjectileGun extends Gun implements GameRemoveable {

	public enum edGunType {TRACER, PARTICLE};
	
	private boolean active = true;
	private boolean firing = false;
	private float reloadTime = 0;
	private float currentReloadTimer = 0;
	private int burstLength = 1;	// Minimum number of rounds that will be fired when the gun fires.
	private int tracerFrequency = 1;	// How many shots fired per visible shot (first of a burst is always visible).
	private int burstRoundsRemaining = 0;	// How many rounds still to be fired in this burst
	private float damagePerRound = 0;
	
	private edGunType gunType = edGunType.TRACER;
	private String fireEventType = null;
	
	private ManagedParticleEffect muzzleFlash = null;
	
	private RemoveableEntityManager entityManager = null; // TODO: Possibly should pass an event to spawn projectiles rather than hold an entity manager reference.
	
	private Vector3f launchLoc = null;
	
	public EDProjectileGun(String name, float reload, float damagePerRound, Node parentNode, RemoveableEntityManager manager) {
		super(name, parentNode);
		setGunStats(reload, damagePerRound, 1, 1);
		currentReloadTimer = 0;
		firing = false;
		
		active = true;
		entityManager = manager;	// TODO: Don't add to manager in constructor, or do with an event.
		launchLoc = new Vector3f();
		
		fireEventType = "EDProjectileGunFire";
	}
	
	public EDProjectileGun(String name, float reload, float damagePerRound, Node parentNode, Vector3f offset, RemoveableEntityManager manager) {
		this(name, reload, damagePerRound, parentNode, manager);
		firePoint.set(offset);
	}
	
	@Override
	public void fire() {
		if(!enabled) return;
		firing = true;
	}
	
	public void stopFiring() {
		firing = false;
	}

	public boolean isActive() {
		return active;
	}
	
	public void setGunStats(float reload, float damagePerRound, int burstLength, int tracerFrequency) {
		this.reloadTime = reload;
		this.burstLength = burstLength;
		this.tracerFrequency = tracerFrequency;
		this.damagePerRound = damagePerRound;
	}
	
	public void setFireEventType(String type) {
		fireEventType = type;
	}
	
	// TODO: This is not a good approach
	public void addMuzzleFlash() {
		muzzleFlash = GameWorldInfo.getGameWorldInfo().getParticleManager().getManagedParticleEffect(name + "MuzzleFlashParticles", firePoint, 0.0f, "muzzle-flash-fast.jme", parentNode);
		//entityManager.add(muzzleFlash);
		//muzzleFlash.setWorldOrLocalSpace(false);
		//muzzleFlash.setLocalTranslation(firePoint);
		
	}

	public void update(float interpolation) {
		if(!active) return;
		
		if(firing && (burstRoundsRemaining <= 0) && (currentReloadTimer == 0) ) {burstRoundsRemaining = burstLength;}
		
		if( (currentReloadTimer == 0) && (burstRoundsRemaining > 0) ) {
			// TODO: CHANGE TO USE REUSEABLEENTITYMANAGER!
			currentReloadTimer = reloadTime;			
			parentNode.localToWorld(firePoint, launchLoc);

			// Create a new projectile. Depending how often the gun fires a tracer round it may be a visible bullet or just an BaseProjectile
			BaseProjectile m = null;
			if( (tracerFrequency > 0) && ((burstRoundsRemaining % tracerFrequency) == 0) ) {
				if(gunType == edGunType.TRACER) {
					m = new TracerProjectile(name + "ProjectileRound", launchLoc, parentNode.getWorldRotation(), 400.0f, 1.5f, damagePerRound);
				}
				if(gunType == edGunType.PARTICLE) {
					m = new BaseProjectile(name + "ProjectileRound", launchLoc, parentNode.getWorldRotation(), 400.0f, 2.0f, damagePerRound);
					GameWorldInfo.getGameWorldInfo().getParticleManager().getManagedParticleEffect(name + "PlasmaBolt", null, 0.0f, "data/effects/plasmaball-fast-20x20.jme", m.getProjectileNode());
					//entityManager.add(e);
				}
			} else {
				m = new BaseProjectile(name + "ProjectileRound", launchLoc, parentNode.getWorldRotation(), 400.0f, 1.5f, damagePerRound);
			}
			//m.setHitEventType(e);	// TODO: Add in hit event types for different projectiles
			entityManager.add(m);
			
			
			// Add or respawn the muzzle flash effect.
			if(gunType == edGunType.TRACER) {
				if(muzzleFlash == null) {addMuzzleFlash();} else {muzzleFlash.respawn();}
			}
			
			// Add a sound effect if it is the start of a new burst
			if(burstRoundsRemaining == burstLength) {
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(fireEventType,  "audio", launchLoc, null);
			}
			burstRoundsRemaining--;
		}
		if (currentReloadTimer > 0) {currentReloadTimer -= interpolation;}
		if(currentReloadTimer < 0) {currentReloadTimer = 0;}

	}
	
	public void setGunType(edGunType type) {gunType = type;}
	
	public void disable() {
		super.disable();
		firing = false;
		burstRoundsRemaining = 0;
	}

	public void deactivate() {
		firing = false;
		active = false;
	}

	public void cleanup() {
		super.cleanup();
		active = false;
		entityManager = null;
	}
	
}