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
package com.calefay.exodusDefence;

import java.net.URL;
import java.util.HashMap;

import com.calefay.effects.ManagedParticleEffect;
import com.calefay.effects.ScrollBoxInfluence;
import com.calefay.exodusDefence.EDLevel.WeatherTypes;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameEventHandler;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.LoadParticleEffect;
import com.calefay.utils.RemoveableEntityManager;
import com.calefay.utils.GameEventHandler.GameEventListener;
import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.Node;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jme.util.CloneImportExport;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.effects.particles.ParticleSystem;

public class EDParticleManager {

	private RemoveableEntityManager entityManager = null;	
	private GameEventListener effectListener = null;
	
	private HashMap<GameEntity, ManagedParticleEffect> damageEffects = null;
	
	private HashMap<String, CloneImportExport> templates = null; 
	
	private ManagedParticleEffect weatherEffect = null;
	
	private Camera cam = null;
	
	private ZBufferState particleZState = null;
	private BlendState smokeBlendState = null;
	
	public EDParticleManager(GameEventHandler eventHandler) {
		entityManager = new RemoveableEntityManager();
		effectListener = eventHandler.addListener("visualEffects", 500);
		damageEffects = new HashMap<GameEntity, ManagedParticleEffect>();
		templates = new HashMap<String, CloneImportExport>();
		
		particleZState = DisplaySystem.getDisplaySystem().getRenderer().createZBufferState();
		particleZState.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
		particleZState.setWritable(false);
		
		smokeBlendState = DisplaySystem.getDisplaySystem().getRenderer().createBlendState();
	    smokeBlendState.setBlendEnabled(true);
	    smokeBlendState.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
	    smokeBlendState.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
	    smokeBlendState.setTestEnabled(true);
	    smokeBlendState.setTestFunction(BlendState.TestFunction.GreaterThan);
	    smokeBlendState.setEnabled(true);
	}
	
	public void update(float interpolation) {
		entityManager.updateEntities(interpolation);
		if(weatherEffect != null) weatherEffect.setLocalTranslation(cam.getLocation());
		processEvents();
	}
	
	public void processEvents() {
		GameEvent gameEvent = effectListener.getNextEvent();
		while(gameEvent != null) {
			handleEvent(gameEvent);
			gameEvent = effectListener.getNextEvent();
		}
	}
	
	private void handleEvent(GameEvent e) {
		
		if(e.getEventType().equals("addDamageSmoke")) {	// TODO: Clean this up it's a bit of a mess/
			Node parent = null; Vector3f pos = null; Object et = e.getEventTarget();
			ManagedParticleEffect effect = null;
			GameEntity entity = null;
			if(et == null) {
				parent = GameWorldInfo.getGameWorldInfo().getRootNode();
				entity = (GameEntity)e.getEventInitiator();
				pos = entity.getPosition();
				effect = damageEffects.get(entity);
				if(effect != null) effect.setLocalTranslation(pos);
			}
			else {
				parent = (Node)et;
				pos = Vector3f.ZERO;
			}
			if(effect == null) {
				effect = buildDamageSmoke(parent, pos);
				if(entity != null) damageEffects.put(entity, effect);
				//entityManager.add(effect);
			}
		}
		
		if(e.getEventType().equals("removeDamageSmoke")) {
			ManagedParticleEffect effect = null;
			if(e.getEventInitiator() != null) effect = damageEffects.remove((GameEntity)e.getEventInitiator());
			if(effect != null) effect.startDying(2.0f);
		}
		
		if(e.getEventType().equals("Explosion")) {
			Vector3f t = (Vector3f)e.getEventInitiator();
			addExplosion(t);
		}
		
		if(e.getEventType().equals("GroundBlast")) {
			addGroundBlast( (Vector3f)e.getEventInitiator());
		}
		
		if(e.getEventType().equals("HugeExplosion")) {
			Vector3f pos = (Vector3f)e.getEventInitiator();
			addHugeExplosion(pos);
		}
	}
	
	public void addWeatherEffect(WeatherTypes type) {
		if(type == null) return;
		buildWeatherEffect(type, cam, GameWorldInfo.getGameWorldInfo().getRootNode());
		//if(weatherEffect != null) entityManager.add(weatherEffect);
		
	}
	
	public void addGroundBlast(Vector3f pos) {
		addLODParticleEffect(	pos, 1.5f, 8f, 25f, 
								"data/effects/explosion10.jme", "data/effects/groundblast-small.jme", "data/effects/groundblast.jme");
	}
	
	public void addExplosion(Vector3f pos) {
		addLODParticleEffect(	pos, 1.5f, 8f, 25f, 
				"data/effects/explosion10.jme", "data/effects/explosion60.jme", "data/effects/explosion60.jme");
	}
	
	public void addHugeExplosion(Vector3f pos) {
		getManagedParticleEffect("HugeExplosion", pos, 10.0f, "explosion-nuke-nosmoke.jme", GameWorldInfo.getGameWorldInfo().getRootNode() );
		//ManagedParticleEffect explosion = new ManagedParticleEffect("RocketExplosion", pos, 10.0f, "explosion-nuke-nosmoke.jme", GameWorldInfo.getGameWorldInfo().getRootNode());
		//entityManager.add(explosion);
	}
	
	/* Crude convenience method to load a different effect based on distance. Only checks once, when it is called.
	 * effect1 will be used if the camera is less than firstSwitchDist from the camera.
	 * effect2 if between first and second, and effect3 if futrher out than second.
	 * effect1 will be used if no camera is available, or no position is specified.*/ 
	public void addLODParticleEffect(Vector3f pos, float duration, float firstSwitchDist, float secondSwitchDist,
									String effect1, String effect2, String effect3) {
		String effect = null;
		float distSquared = pos.distanceSquared(cam.getLocation());
		if( (cam == null) 	|| (pos == null) 
							|| ( distSquared < (firstSwitchDist * firstSwitchDist) )) {
			effect = effect1;
		} else if( ( distSquared < (secondSwitchDist * secondSwitchDist) )) {
			effect = effect2;
		} else {
			effect = effect3;
		}
		
		getManagedParticleEffect("LODParticleEffect", pos, duration, effect, GameWorldInfo.getGameWorldInfo().getRootNode());
		//ManagedParticleEffect e = new ManagedParticleEffect("LODParticleEffect", pos, duration, effect, GameWorldInfo.getGameWorldInfo().getRootNode());
		//entityManager.add(e);
	}
	
	public void setCamera(Camera cam) {this.cam = cam;}
	
	/* Creates a ManagedParticleEffect with the specified parameters.
	 * Cloning will be done automatically, and the effect will be added to the ParticleManager's entityManager to handle updates and removal.*/
	public ManagedParticleEffect getManagedParticleEffect(String name, String filename, Node parent) {
		ManagedParticleEffect effect = new ManagedParticleEffect(name, getParticleNode(filename), parent);
		entityManager.add(effect);
		
		return effect;
	}
	

	public ManagedParticleEffect getManagedParticleEffect(String name, Vector3f pos, float duration, String filename, Node parent) {
		ManagedParticleEffect effect = new ManagedParticleEffect(name, pos, duration, getParticleNode(filename), parent);
		entityManager.add(effect);
		
		return effect;
	}
	
	/* Returns a clone of the specified particle effect - loading and storing the template in the process if this is the first time.*/ 
	public Node getParticleNode(String filename) {
		CloneImportExport cie = templates.get(filename);
		if(cie == null) {
			URL effectURL = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_PARTICLE, filename);
			Node particleNode = LoadParticleEffect.loadParticleEffect(effectURL);
		    cie = new CloneImportExport();
		    cie.saveClone(particleNode);
		    templates.put(filename, cie);
		}
	    
		Node particleNode = (Node)cie.loadClone();
		
		// Do some better exception handling - following code should not execute if particleNode is null.
        particleNode.setModelBound(new BoundingBox());
		particleNode.updateModelBound();
        /*********************** SHARE STATE ********************/		
		particleNode.setRenderState(particleZState);
		particleNode.updateRenderState();
		
		return particleNode;
	}
	
	/***************** Weather factory methods follow ****************/
	public void buildWeatherEffect(WeatherTypes type, Camera cam, Node parent) {
		if(type == null) return;
		
		switch(type) {
		case NONE: break;
		case RAIN: buildRainEffect(cam, parent); break;
		case SNOW: buildSnowEffect(cam, parent); break;
		}
		
	}
	
	public void buildRainEffect(Camera cam, Node parent) {
		//ManagedParticleEffect rainEffect = new ManagedParticleEffect("rainEffect", cam.getLocation(), 0.0f,  "weather-rain.jme", parent);
		ManagedParticleEffect rainEffect = getManagedParticleEffect("rainEffect", cam.getLocation(), 0.0f,  "weather-rain.jme", parent);
		
		if(rainEffect == null) return;
		
		rainEffect.getParticleNode().setLocalScale(1.0f);
		((ParticleSystem)rainEffect.getParticleNode().getChild(0)).addInfluence( new ScrollBoxInfluence(cam, 25f, 30f, 25f) );
		((ParticleSystem)rainEffect.getParticleNode().getChild(1)).addInfluence( new ScrollBoxInfluence(cam, 100f, 50f, 50f) );
		((ParticleSystem)rainEffect.getParticleNode().getChild(0)).warmUp(3);
		((ParticleSystem)rainEffect.getParticleNode().getChild(1)).warmUp(3);
	}
	
	public void buildSnowEffect(Camera cam, Node parent) {
		//ManagedParticleEffect snowEffect = new ManagedParticleEffect("snowEffect", cam.getLocation(), 0.0f,  "weather-snow.jme", parent);
		ManagedParticleEffect snowEffect = getManagedParticleEffect("snowEffect", cam.getLocation(), 0.0f,  "weather-snow.jme", parent);
		snowEffect.getParticleNode().setLocalScale(1.0f);
		((ParticleSystem)snowEffect.getParticleNode().getChild(0)).addInfluence( new ScrollBoxInfluence(cam, 15f, 20f, 15f) );
		((ParticleSystem)snowEffect.getParticleNode().getChild(1)).addInfluence( new ScrollBoxInfluence(cam, 50f, 30f, 50f) );
		((ParticleSystem)snowEffect.getParticleNode().getChild(0)).warmUp(10);
		((ParticleSystem)snowEffect.getParticleNode().getChild(1)).warmUp(15);
		
	}
	
	public ManagedParticleEffect buildDamageSmoke(Node parent, Vector3f pos) {
		// FIXME: Hardcoded effect path
		ManagedParticleEffect damageEffect = getManagedParticleEffect("DamageSmokeEffect", pos, 0.0f, "smoke-40.jme", parent);
	    damageEffect.getParticleNode().getChild(0).setRenderState(smokeBlendState);
	    damageEffect.getParticleNode().getChild(0).updateRenderState();
	    
	    return damageEffect;
	}
	/************** Weather factory methods end *******************/
	
	/** Clears out all managed effects, without removing the handlers - unlike cleanup(), the manager can still be used.*/
	public void flush() {
		if(entityManager != null) entityManager.clearEntities();
		if(effectListener != null) effectListener.flushEvents();
		if(damageEffects != null) damageEffects.clear();	// The effects should already be cleaned up by their entity manager.
		if(templates != null) templates.clear();
		
		weatherEffect = null;
	}
	
	public void cleanup() {
		weatherEffect = null; cam = null;
		
		if(entityManager != null) {
			entityManager.printEntityList();
			entityManager.cleanup();
			entityManager = null;
		}
		
		System.out.println("------- Particle Template list begins --------");
		for(String s : templates.keySet()) System.out.println(s);
		System.out.println("------- Particle Template list ends --------");
		
		if(damageEffects != null) damageEffects.clear(); damageEffects = null;	// The effects should already be cleaned up by their entity manager.
		if(templates != null) templates.clear();
		
		GameWorldInfo.getGameWorldInfo().getEventHandler().removeListener("visualEffects"); effectListener = null;
	}
}
