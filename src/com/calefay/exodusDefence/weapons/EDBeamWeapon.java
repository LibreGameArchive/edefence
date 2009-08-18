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
import com.calefay.utils.GameWorldInfo;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.BillboardNode;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial.LightCombineMode;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.scene.state.ZBufferState.TestFunction;
import com.jme.system.DisplaySystem;


public class EDBeamWeapon extends LaserBeamWeapon {

	private static Quad masterFlashMesh = null;
	private SharedMesh laserFlash = null;
	private BillboardNode flashNode = null;
	
	private Node hitEffectNode = null;
	private ManagedParticleEffect hitEffect = null;
	private boolean hitEffectOn = false;
	
	public EDBeamWeapon(String newName, Node src, float dps, Texture beamTx, Texture flashTx) {
		super(newName, src, new Vector3f(0f, 0f, 0f), dps);
		
		if(masterFlashMesh == null) {constructMasterFlashMesh();}
		
		laserFlash = new SharedMesh(getName() + "LaserFlash", masterFlashMesh);
		laserFlash.setLocalScale(4.0f);
		laserFlash.setIsCollidable(false);
		laserFlash.setModelBound(new BoundingBox());
		laserFlash.updateModelBound();
		
		setTextures(beamTx, flashTx);
		
		ZBufferState zs = DisplaySystem.getDisplaySystem().getRenderer().createZBufferState();
		zs.setFunction(TestFunction.LessThanOrEqualTo);
		zs.setWritable(false);
		
		BlendState as = DisplaySystem.getDisplaySystem().getRenderer().createBlendState();
		as.setBlendEnabled(true);
		as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
		as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
		as.setTestEnabled(true);
		as.setTestFunction(BlendState.TestFunction.GreaterThan);
		
		laserFlash.setRenderState(zs);
		laserFlash.setRenderState(as);
		laserFlash.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		laserFlash.setLightCombineMode(LightCombineMode.Off);
		
		flashNode = new BillboardNode(getName() + "flashNode");
		flashNode.setAlignment(BillboardNode.SCREEN_ALIGNED);
		flashNode.attachChild(laserFlash);
		beamNode.attachChild(flashNode);
		
		laserFlash.setLocalTranslation( 0f, 0f, 0f);
		laserFlash.updateRenderState();
		
		hitEffect = null; hitEffectOn = false;
	}
	
	public void addHitEffect(ManagedParticleEffect effect) {
		hitEffect = effect;
		hitEffect.burnOut();
		if(hitEffectNode == null) {
			hitEffectNode = new Node(getName() + "HitEffectNode");
			beamNode.attachChild(hitEffectNode);
		}
		hitEffect.attachTo(GameWorldInfo.getGameWorldInfo().getRootNode());
		hitEffect.setLocalTranslation(0, 0, 0);
	}
	
	@Override
	public void update(float interpolation) {
		super.update(interpolation);
		if(hitEffect != null) {
			if(isOn && isBlocked) {
				if(!hitEffectOn) {
					hitEffect.reStart();
					hitEffectOn = true;
				}
				hitEffectNode.setLocalScale(length);
				hitEffectNode.localToWorld(Vector3f.UNIT_Z, hitEffect.getParticleNode().getLocalTranslation());
			} else {
				if(hitEffectOn) {
					hitEffect.burnOut();
					hitEffectOn = false;
				}
			}
		}
	}
	
	public void cleanup() {
		flashNode.detachAllChildren();
		if(flashNode.getParent() != null) flashNode.getParent().detachChild(flashNode);
		laserFlash = null;
		
		if(hitEffect != null) hitEffect.deactivate(); hitEffect = null;
		if(hitEffectNode != null) hitEffectNode.removeFromParent(); hitEffectNode = null;
		super.cleanup();
	}
	
	protected void setTextures(Texture beamTexture, Texture flashTexture) {
		super.setLaserTexture(beamTexture);
		
		if(flashTexture == null) return;
		
		TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
		ts.setEnabled(true);
		flashTexture.setWrap(Texture.WrapMode.Repeat);
		flashTexture.setScale( new Vector3f(1.0f, 1.0f,1.0f));
		ts.setTexture(flashTexture);
		laserFlash.setRenderState(ts);
		laserFlash.updateRenderState();
	}
	
	private static void constructMasterFlashMesh() {
		masterFlashMesh = new Quad("MasterFlashMesh", 0.25f, 0.25f);
		masterFlashMesh.setIsCollidable(false);
		masterFlashMesh.setModelBound(new BoundingBox());
		masterFlashMesh.updateModelBound();
	}
	
}
