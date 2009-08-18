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
import com.jme.scene.Node;

public class EDCircleZoneRadar extends EDGenericRadar {
	
	private float detectionRadius = 0.0f;
	private Vector2f pos2D = null;
	
	public EDCircleZoneRadar(Node detectorNode, EDEntityMap map, float detectionRadius) {
		super(detectorNode, map);		
		this.detectionRadius = detectionRadius;
		pos2D = new Vector2f();
	}
	
	protected void computeTrackingZone() {
		pos2D.set(radarNode.getWorldTranslation().x, radarNode.getWorldTranslation().z);
	}
	
	protected boolean targetInTrackingZone() {
		return map.inCircle(currentTarget, pos2D, detectionRadius);
	}
	
	protected ArrayList<GameEntity> getValidTargets() {
		return map.getEntitiesInCircle(pos2D, detectionRadius);
	}
		
	public void cleanup() {
		super.cleanup();
		detectionRadius = 0;
		pos2D = null;
	}
}
