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
package com.calefay.exodusDefence.edControls;

import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.Node;
import com.jmex.terrain.TerrainBlock;

public class TurretCameraHandler {

	protected Node trackNode = null;
	protected Camera cam = null;
	
	protected Vector3f idealLoc = null;
	protected Vector3f currentLoc = null;
	protected Vector3f currentVector = null;
	private TerrainBlock tb;
	
	protected float maxSpeed = 0.1f;
	protected float terrainClearance = 1.0f;
	protected float minFollowDistance = 1.0f;
	protected float maxFollowDistance = 30.0f;
	protected float followDistance = 15.0f;
	
	public TurretCameraHandler(Node n, Camera c) {
		minFollowDistance = 1.0f;
		maxFollowDistance = 30.0f;
		followDistance = 15.0f;
		
		trackNode = n;
		cam = c;
		
		idealLoc = new Vector3f();
		getIdealLoc();
		currentLoc = new Vector3f(idealLoc);
		currentVector = new Vector3f();
	}
	
	public void update() {	// TODO: Should this be a GameRemoveable for convenience?
		getIdealLoc();
		currentVector = idealLoc.subtract(currentLoc);
		currentVector.normalize();
		currentVector.multLocal(maxSpeed);
		currentLoc.addLocal(currentVector);
		
		if(tb != null) {
			float targetHeight = tb.getHeightFromWorld(currentLoc);
			if( !Float.isNaN(targetHeight) && !Float.isInfinite(targetHeight) ) {
				if(targetHeight >= (currentLoc.y - terrainClearance) ) {
					currentLoc.y = targetHeight + terrainClearance;
				}
			}
			}
		cam.setLocation( currentLoc);
		
		Vector3f targetPoint = new Vector3f();
		trackNode.localToWorld( new Vector3f( 0, 0, 50.0f), targetPoint);
		
		cam.lookAt(targetPoint, Vector3f.UNIT_Y);
	}
	
	public void addFollowDistance(float amount) {
		followDistance += amount;
		if(followDistance > maxFollowDistance) {followDistance = maxFollowDistance;}
		if(followDistance < minFollowDistance) {followDistance = minFollowDistance;}
	}
	
	private Vector3f getIdealLoc() {
		trackNode.localToWorld( new Vector3f( 0, terrainClearance, -followDistance), idealLoc);
		if(idealLoc.y <= (trackNode.getWorldTranslation().y + 1.0f) ) {idealLoc.y = (trackNode.getWorldTranslation().y + 1.0f);}
		return idealLoc;
	}
	
	/** Sets the node which the camera is to track.*/
	public void setTrackNode(Node t) {
		trackNode = t;
	}
	
	/** Use if you have a TerrainBlock that the camera must stay above.*/
	public void setTerrainAvoidance(TerrainBlock t) {
		tb = t;
	}
	
	public void cleanup() {
		trackNode = null;
		tb = null;
		cam = null;
		
		idealLoc = null;
		currentLoc = null;
		currentVector = null;
	}
}
