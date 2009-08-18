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
package com.calefay.effects;

import com.calefay.utils.GameReuseable;
import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jmex.effects.particles.ParticleSystem;

public class ManagedParticleEffect extends GameReuseable {

	private Node particleNode = null;
	/** remainingLife is the time in seconds before the effect will be flagged for deletion. */
	private float remainingLife = 0;
	/** isDying signals that the effect will be flagged for deletion when remainingLife hits 0. */
	private boolean isDying = false; 

	private Integer[] releaseRates = null;
	
	// TODO: Should probably instantiate using a factory method with reuse, and hide constructors.
	// FIXME: Currently particle effects are being scaled to 10% on load. This can be done in the editor and should be removed (will mean tweaking all current files).
	public ManagedParticleEffect(String name, Vector3f pos, float lifeSpan, Node effect, Node parentNode) {
		//URL effectURL = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_PARTICLE, particleEffectPath);
		// TODO: Error handling if null
		//particleNode = LoadParticleEffect.loadParticleEffect(effectURL);
		particleNode = effect;
		particleNode.setName(name);
		
		particleNode.setLocalScale( new Vector3f(0.1f, 0.1f, 0.1f)); // TODO: Don't want to have to scale particle effects every load
		particleNode.setModelBound(new BoundingBox());
		particleNode.updateModelBound();
		particleNode.setIsCollidable(false);
		reset(parentNode, pos);

		if (lifeSpan > 0) {
			isDying = true;
			remainingLife = lifeSpan;
		} 
	}

	public ManagedParticleEffect(	String name, 
			float lifeSpan, 
			Node effect, 
			Node parentNode) {
		this(name, null, lifeSpan, effect,  parentNode);

	}
	
	public ManagedParticleEffect(	String name, 
			Node effect, 
			Node parentNode) {
		this(name, null, 0, effect, parentNode);
	}
	
	protected void reset() {
		remainingLife = 0;
		isDying = false;
		active = true;
		deactivating = false;
		respawn();	// TODO: REMOVE respawn - Redundant to respawn newly loaded effect. Reused effect can do it manually.
	}
	
	protected void reset(Node parentNode, Vector3f pos) {
		if(parentNode != null) parentNode.attachChild(particleNode);
		if(pos != null) particleNode.getLocalTranslation().set(pos);
		particleNode.updateGeometricState(0, true);
		reset();
	}
	
	public void update(float interpolation) {
		if (active && isDying) {
			remainingLife -= interpolation;
			if (remainingLife <= 0) {deactivate();} 
		} 
	} 
	
	/** Used to remove an ongoing effect while giving it time to "burn out".
	 *  Note that it will usually be necessary to assign a new local translation 
	 *  eg. setting it to previous world translation when attaching to root node.
	 * @param timeToDeath - time in seconds that the effect will burn out for
	 * @param newParent - can be used to assign the node to a new parent if it's previous parent has been removed (eg. when a missile explodes)
	 */
	public void startDying(float timeToDeath, Node newParent) {	// FIXME: Could do the positioning here easily using localToWorld and worldToLocal?
		startDying(timeToDeath);
		particleNode.removeFromParent();
		newParent.attachChild(particleNode);
	}
	
	public void startDying(float timeToDeath) {
		isDying = true;
		remainingLife = timeToDeath;
		burnOut();
	}
	
	/** Stops any more particles from spawning but does NOT mark the effect for removal.*/
	public void burnOut() {
		// FIXME: Creating an array for every particle effect that burns out is horrible - very few will ever use it.
		if( (releaseRates == null) ||( (releaseRates != null) && (releaseRates.length < particleNode.getQuantity())) ) releaseRates = new Integer[particleNode.getQuantity()];
		
		for(int i=0; i < particleNode.getQuantity(); i++) { // TODO: Use get children here to save a null check every cycle?
			ParticleSystem p = (ParticleSystem)particleNode.getChild(i);
			p.setRepeatType(Controller.RT_CLAMP); // TODO: Have it store the original value so so that it can be properly reset.
			releaseRates[i] = p.getReleaseRate();
			p.setReleaseRate(0);
		}
	}

	/* Starts particles flowing again after burnOut has been called. 
	 * NOTE: Assumes it will be a cyclic repeat type (as would usually be no point burning out an effect that doesn't recycle).
	 * NOTE: Resets remaining life to 0 (which means indefinite).*/
	public void reStart() {
		if( (releaseRates == null) || (releaseRates.length < particleNode.getQuantity()) ) return;
		for(int i = 0; i < particleNode.getQuantity(); i++) {
			ParticleSystem p = (ParticleSystem)particleNode.getChild(i);
			p.setRepeatType(Controller.RT_CYCLE);
			p.setReleaseRate( releaseRates[i] );
		}
		remainingLife = 0;
	}
	
	public void deactivate() {
		if(!deactivating) {
			cleanup();
			deactivating = true;
			active = false; // TODO: Added this to be compatible with none reusable manager. Will it cause problems in reuseable manager?	
		}
	}
	
	public void cleanup() {
		active = false;
		if(particleNode != null) {
			particleNode.removeFromParent();
			particleNode = null;
		}
	}
	
	public Node getParticleNode() {	// FIXME: Should not be able to do this. Attaching anything other than particleGeometry to the node would cause an cast error.
		return particleNode;
	}
	
	public void attachTo(Node parentNode) {parentNode.attachChild(particleNode);}
	/* NOTE: Initial location should be set in the constructor if possible to avoid particles being spawned out of position.*/
	public void setLocalTranslation(Vector3f translation) {particleNode.setLocalTranslation(translation);}
	public void setLocalTranslation(float x, float y, float z) {particleNode.setLocalTranslation(x, y, z);}
	public void setLocalScale(float scale) {particleNode.setLocalScale(scale);}
	
	public void respawn() {
		for(int i=0; i < particleNode.getQuantity(); i++) {
			ParticleSystem p = (ParticleSystem)particleNode.getChild(i);
			p.forceRespawn();
		}
	}

}
