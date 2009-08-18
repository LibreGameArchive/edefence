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
package com.calefay.exodusDefence.radar;

import java.util.ArrayList;


import com.calefay.exodusDefence.EDEntityMap;
import com.calefay.utils.GameEntity;
import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.scene.Node;

public class EDTriangleZoneRadar extends EDGenericRadar {
	
	private Vector3f point1 = null; private Vector3f point2 = null; private Vector3f point3 = null;
	private Vector3f a3f = null, b3f = null, c3f = null;
	private Vector2f a = null, b = null, c = null;
	
	
	public EDTriangleZoneRadar(Node detectorNode, EDEntityMap map) {

		super(detectorNode, map);
		
		point1 = new Vector3f(0, 0, 0);
		point2 = new Vector3f(-75, 0, 300);
		point3 = new Vector3f(75, 0, 300);	// FIXME: Hardcoded ugliness
		
		a3f = new Vector3f(); b3f = new Vector3f(); c3f = new Vector3f();
		a = new Vector2f(); b = new Vector2f(); c = new Vector2f();
	}
	
		
	protected void computeTrackingZone() {
		radarNode.localToWorld(point1, a3f);	// Not too happy with this way of getting the triangle.
		radarNode.localToWorld(point2, b3f);
		radarNode.localToWorld(point3, c3f);
		a.set(a3f.x, a3f.z); b.set(b3f.x, b3f.z); c.set(c3f.x, c3f.z);

	}
	
	protected boolean targetInTrackingZone() {
		return map.inTriangle(currentTarget, a, b, c);
	}
	
	protected ArrayList<GameEntity> getValidTargets() {
		return map.getEntitiesInTriangle(a, b, c);
	}
		
	public void cleanup() {
		super.cleanup();
		
		point1 = null; point2 = null; point3 = null;
		a = null; b = null; c = null;
		a3f = null; b3f = null; c3f = null;
	}
}
