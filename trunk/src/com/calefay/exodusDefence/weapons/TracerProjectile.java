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


import com.jme.bounding.BoundingBox;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.SharedMesh;
import com.jme.scene.TriMesh;
import com.jme.scene.Spatial.LightCombineMode;
import com.jme.scene.shape.Cylinder;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;

public class TracerProjectile extends BaseProjectile {

	private static Cylinder masterMesh = null;
	private static TextureState masterTS = null;
	private static BlendState masterAS = null;
	private static ZBufferState masterZS = null;
	
	private static float gravityAcceleration = 0.098f;
	
	private SharedMesh tracerMesh = null;
	
	private Quaternion rot = null;
	private Vector3f xAxis = null; private Vector3f yAxis = null; private Vector3f zAxis = null;
	private Vector3f projectileTravel = null;
	
	private float gravitySpeed = 0;
	
	public TracerProjectile(String newName, Vector3f pos, Quaternion orientation,
			float speed, float lifeTime, float damage,
			TriMesh masterMesh) {
		super(newName, pos, orientation, speed, lifeTime, damage);
		
		rot = new Quaternion();
		
		if(masterMesh != null) tracerMesh = new SharedMesh(name + "TracerMesh", masterMesh);
		reset( newName, pos, orientation, speed, lifeTime, damage);

	}
	
	// TODO: Get rid of this constructor and get rid of static master meshes.
	public TracerProjectile(String newName, Vector3f pos, Quaternion orientation,
							float speed, float lifeTime, float damage) {
		super(newName, pos, orientation, speed, lifeTime, damage);
		
		rot = new Quaternion();
		
		if(masterMesh != null) {
			tracerMesh = new SharedMesh(name + "TracerMesh", masterMesh);			

			tracerMesh.setRenderState(masterAS);
			tracerMesh.setRenderState(masterTS);
			tracerMesh.setRenderState(masterZS);
			tracerMesh.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
			tracerMesh.setLightCombineMode(LightCombineMode.Off);
			tracerMesh.updateRenderState();
			
			projectileNode.attachChild(tracerMesh);
			projectileNode.setModelBound(new BoundingBox());
			projectileNode.updateModelBound();
			
			tracerMesh.setIsCollidable(false);
			projectileNode.setIsCollidable(false);
		}
		
		reset( newName, pos, orientation, speed, lifeTime, damage);
	}
	
	/** used by the manager class to reset a projectile when recycled. */
	protected void reset(String newName, Vector3f pos, Quaternion orientation, float speed, float lifeTime, float damage) {
		super.reset(newName, pos, orientation, speed, lifeTime, damage);
		gravitySpeed = 0;
		if(tracerMesh != null) {tracerMesh.setName(name + "tracerMesh");}
	}
	
	public void update(float interpolation) {
		super.update(interpolation);
		if(active) {
			gravitySpeed += (gravityAcceleration  * interpolation);
			
			projectileTravel = projectileDirection.mult(projectileSpeed * interpolation );
			projectileTravel.y -= gravitySpeed;
			
			zAxis = projectileTravel.normalize();
			xAxis = projectileNode.getWorldRotation().getRotationColumn(0);
			yAxis = zAxis.cross(xAxis);
			rot.fromAxes(xAxis, yAxis, zAxis);
			projectileNode.setLocalRotation(rot);

			projectileNode.getLocalTranslation().y -= gravitySpeed;
		}
		
	}
	
	public void checkHits(float interpolation) {
		if(tracerMesh != null) tracerMesh.setIsCollidable(false);		// TODO: Sloppy!
		super.checkHits(interpolation);
		if(tracerMesh != null) tracerMesh.setIsCollidable(true);		// TODO: Sloppy!
	}
	
	public static void setGravity(float g) {gravityAcceleration = g;}
	
	public void cleanup() {
		super.cleanup();
		
		if(tracerMesh != null) {tracerMesh.removeFromParent(); tracerMesh = null;}
		xAxis = null; yAxis = null; zAxis = null;
		rot = null;
		projectileTravel = null;
	}
	
	/* NOTE: This accesses the renderer so must be called from the OpenGL thread.*/ 
	public static void setupMasterMesh(TextureState tracerTexture) {
		masterMesh = new Cylinder("TracerMaster", 2, 4, 0.07f, 3.0f);
		
		Renderer renderer = DisplaySystem.getDisplaySystem().getRenderer();
		masterZS = renderer.createZBufferState();
		masterZS.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
		masterZS.setWritable(false);
		
		masterAS = renderer.createBlendState();
		masterAS.setBlendEnabled(true);
		masterAS.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
		masterAS.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
		masterAS.setTestEnabled(true);
		masterAS.setTestFunction(BlendState.TestFunction.GreaterThan);
		
		masterTS = tracerTexture;
		
		setBounds( new Vector3f(-100f, -100f, -100f), new Vector3f(100f, 100f, 100f) );
	}
	
}
