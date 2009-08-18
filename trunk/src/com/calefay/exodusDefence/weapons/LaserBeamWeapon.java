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


import com.calefay.effects.LaserLine;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial.LightCombineMode;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;

public class LaserBeamWeapon extends BaseBeamWeapon {
	
	private static LaserLine masterLaserMesh = null;
	
	private float textureScale = 50.0f;		// Smaller scale = more repetitions
	private float cyclesPerSecond = 1.0f; 
	private SharedMesh laserQuad = null;
	private float cycle = 1.0f;
	private float beamWidth = 1.0f;
    
    private Texture laserTx = null;
    
    public LaserBeamWeapon(String newName, Node src) {
    	super(newName, src);
    	setupLaser();	
	}
    
	public LaserBeamWeapon(String newName, Node src, Vector3f offset) {
		super(newName, src, offset);
		setupLaser();
	}
	
	public LaserBeamWeapon(String newName, Node src, Texture beamTx) {
		this(newName, src);
		this.setLaserTexture(beamTx);
	}
	
	public LaserBeamWeapon(String newName, Node src, Vector3f offset, Texture beamTx) {
		this(newName, src, offset);
		this.setLaserTexture(beamTx);
	}
	
	public LaserBeamWeapon(String newName, Node src, Vector3f offset, float dps) {
		super(newName, src, offset, dps);
		setupLaser();
	}
	
	public LaserBeamWeapon(String newName, Node src, Vector3f offset, float dps, Texture beamTx) {
		super(newName, src, offset, dps);
		setupLaser();
		this.setLaserTexture(beamTx);
	}
	
	private void setupLaser() {
		if(masterLaserMesh == null) {constructMasterMesh();}
		
		beamWidth = 1.0f;
		
		ZBufferState zs = DisplaySystem.getDisplaySystem().getRenderer().createZBufferState();
		zs.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
		zs.setWritable(false);
		
		BlendState as = DisplaySystem.getDisplaySystem().getRenderer().createBlendState();
		as.setBlendEnabled(true);
		as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
		as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
		as.setTestEnabled(true);
		as.setTestFunction(BlendState.TestFunction.GreaterThan);
		
		laserQuad = new SharedMesh(getName() + "LaserQuad", masterLaserMesh);
		laserQuad.setIsCollidable(false);
		laserQuad.setModelBound(new BoundingBox());		// TODO: Do something about the bounding boxes as they are huge.
		laserQuad.updateModelBound();
		
		laserQuad.setRenderState(zs);
		laserQuad.setRenderState(as);
		laserQuad.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		laserQuad.setLightCombineMode(LightCombineMode.Off);
		
		laserQuad.updateRenderState();
		beamNode.attachChild(laserQuad);
	}

	public void update(float interpolation) {
		super.update(interpolation);
		
		if(isOn) {
			cycle -= cyclesPerSecond * interpolation;
			if (cycle <= 0.0f) cycle = 1.0f;
			
			if(laserTx != null) {	// FIXME: Laser scaling needs changing as they almost always share the texture.
				laserTx.setScale( new Vector3f(1.0f, length / textureScale,1.0f));
				laserTx.setTranslation(new Vector3f(0.0f,cycle,0.0f));
			}
			laserQuad.setLocalScale( new Vector3f(beamWidth, 1.0f, length) );
			
			showFlatToCamera();
		}
	}
	
	public void setBeamWidth(float w) {	// Should do this at the superclass level perhaps, though should not do it to the master mesh.
		beamWidth = w;
	}
	
	protected void showFlatToCamera() {
		// This method rotates the laserQuad around it's z-axis to ensure that it is always shown flat  to the camera, not edge on.
			Vector3f lVisibleAxis = laserQuad.getWorldRotation().getRotationColumn(0).normalize();
			Vector3f laserViewVector = DisplaySystem.getDisplaySystem().getRenderer().getCamera().getLocation().subtract( 
										laserQuad.getWorldTranslation())
											.normalize();
			float laserViewAngle = lVisibleAxis.angleBetween(laserViewVector);
			Quaternion correctionRot = new Quaternion();
			float cr = (FastMath.PI / 2.0f) - laserViewAngle;
			correctionRot.fromAngleNormalAxis(cr, new Vector3f(0,0,1.0f));
			laserQuad.getLocalRotation().multLocal(correctionRot);
			
		}
	
	/** Releases all references and data that this is holding on to. It ceases to be useable.*/
	public void cleanup() {
		laserTx = null;		// TODO: May need to release texture on gfx card.
		if(laserQuad != null) {laserQuad.removeFromParent(); laserQuad = null;}
		
		super.cleanup();
	}
	
	protected void setLaserTexture(Texture beamTexture) {
		if(beamTexture == null) return;
		
		TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
		ts.setEnabled(true);
		laserTx = beamTexture;
		laserTx.setWrap(Texture.WrapMode.Repeat);
		laserTx.setScale( new Vector3f(1.0f, 1.0f,1.0f));
		ts.setTexture(laserTx);
		laserQuad.setRenderState(ts);
		laserQuad.updateRenderState();
	}
	
	public Texture getLaserTexture() {
		return laserTx;
	}
	
	protected static void constructMasterMesh() {
		masterLaserMesh = new LaserLine("MasterLaserMesh", 1.0f, 1.0f);
		masterLaserMesh.setIsCollidable(false);
		masterLaserMesh.setModelBound(new BoundingBox());	// TODO: Shouldn't need this?
		masterLaserMesh.updateModelBound();
	}
	
	public static void setMasterMesh(LaserLine laserMesh) {
		// TODO: Allow it to accept different types of mesh - kinda pointless like this.
		masterLaserMesh = laserMesh;
	}
}

