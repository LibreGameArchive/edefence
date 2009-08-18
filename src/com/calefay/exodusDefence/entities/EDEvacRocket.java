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


import com.calefay.effects.ManagedParticleEffect;
import com.calefay.exodusDefence.EntityRegister;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameRemoveable;
import com.calefay.utils.GameWorldInfo;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Node;


public class EDEvacRocket implements GameRemoveable, GameEntity {
	private static final float ACCELERATION = 0.05f;
	private static final float MAXHITPOINTS = 1000f;
	
	private Node rocketNode = null;
	private float hitPoints = 0;
	private float speed = 0;
	private Vector3f velocity = null;
	private float maneuver = 0;
	private boolean launching = false;
	
	private boolean active = false;
	private String name = null;
	private String faction = "default";
	
	private ManagedParticleEffect blastEffect = null;
	private ManagedParticleEffect blowbackEffect = null;
	
	public EDEvacRocket(String name, Node model) {
		this.name = name;
		
		hitPoints = MAXHITPOINTS;
		speed = 0; maneuver = 0;
		launching = false;
		active = true;
		velocity = new Vector3f();
		
		rocketNode = model;		// TODO: Change to use SharedMesh or SharedNode.
		rocketNode.setName(name);
		
		EntityRegister.getEntityRegister().registerGeometryEntity(rocketNode, this);
	}
	
	public void update(float interpolation) {
		if(!active) return;
		
		if(launching) {
			speed += ACCELERATION * interpolation;
			
			velocity = rocketNode.getLocalRotation().getRotationColumn(2).mult(speed);
			rocketNode.getLocalTranslation().addLocal(velocity);
			
			float posY = rocketNode.getLocalTranslation().getY();
			
			if((posY > 500.0f) && (maneuver < 1.0f)) {
				maneuver += interpolation / 3f;
				
				Quaternion pitch = new Quaternion();
				pitch.fromAngleAxis(-FastMath.HALF_PI + (maneuver * FastMath.HALF_PI / 4.0f), Vector3f.UNIT_X);
				rocketNode.setLocalRotation(pitch);
			}
			
			if(posY > 1000.0f) {
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("RocketAway", "gameplayEvents", this, null);
				if(blastEffect != null) {
					Vector3f worldPos = new Vector3f(blastEffect.getParticleNode().getWorldTranslation());
					Vector3f worldScale = new Vector3f(blastEffect.getParticleNode().getWorldScale());
					blastEffect.startDying(60f, GameWorldInfo.getGameWorldInfo().getRootNode());
					blastEffect.getParticleNode().setLocalTranslation(worldPos);
					blastEffect.getParticleNode().setLocalScale(worldScale);
					blastEffect.getParticleNode().updateWorldVectors();
					blastEffect = null;
				}
				if(blowbackEffect != null) {
					blowbackEffect.startDying(10f);
					blowbackEffect = null;
				}
				deactivate();
			}
			
		}
	}
	
	public boolean handleAction(GameEvent action) {
		// TODO: replace the hardcoded string with an appropriate constant.
		if(action.getEventType() == "damage") {
			// TODO: This should be in a try..catch with error handling.
			float damageAmount = (Float)action.getEventTarget();
			hitPoints -= damageAmount;
			
			if(hitPoints <= 0) {
				hitPoints = 0;
				if(active) {
					GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("EntityDestroyed",  "gameplayEvents", this, null);
					GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("RocketDestroyed",  "default", this, null);
					deactivate();
				}
			}
			return true;
			} else {return false;}
	}

	public void addParticleTrail(String trailPath, String blowbackPath) {
		if(trailPath != null) {
			blastEffect = GameWorldInfo.getGameWorldInfo().getParticleManager().getManagedParticleEffect("EDRocketBlastEffect", new Vector3f( 0, 0, -15f), 0.0f, trailPath, rocketNode);
			//effectHandler.add(blastEffect);
		}
		
		if(blowbackPath != null) {
			Vector3f pos = new Vector3f(rocketNode.getWorldTranslation());
			pos.y -= 20f;
			blowbackEffect = GameWorldInfo.getGameWorldInfo().getParticleManager().getManagedParticleEffect("EDRocketBlowBackEffect", pos, 0.0f, blowbackPath, GameWorldInfo.getGameWorldInfo().getRootNode());
			//effectHandler.add(blowbackEffect);
		} 
	}
	
	public void initiateLaunch() {
		launching = true;
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("RocketLaunch", "audio", getRocketNode(), null);
	}
	public boolean isLaunching() {return launching;}
	public boolean isActive() {return active;}
	public Node getRocketNode() {return rocketNode;}		// TODO: Ideally would like this not to be available publicly, should interact with the node though method calls.
	public String getName() {return name;}
	// GameEntity implemented methods begin
	public Vector3f getPosition() {return rocketNode.getWorldTranslation();}
	public Quaternion getRotation() {return rocketNode.getWorldRotation();}
	public Vector3f getVelocity() {return velocity;}
	public boolean isDead() {return !active;}
	public String getFaction() {return faction;}
	public void setFaction(String faction) {this.faction = faction;}
	// GameEntity implemented methods end
	
	public void deactivate() {
		active = false;
		if(rocketNode != null) {rocketNode.removeFromParent();}
		if(blastEffect != null) {blastEffect.deactivate();}
		if(blowbackEffect != null) {blowbackEffect.deactivate();}
	}
	
	public void cleanup() {
		active = false;
		if(rocketNode != null) {rocketNode.removeFromParent(); rocketNode = null;}
		if(blastEffect != null) {blastEffect.cleanup(); blastEffect = null;}
		if(blowbackEffect != null) {blowbackEffect.cleanup(); blowbackEffect = null;}
	}
	
}
