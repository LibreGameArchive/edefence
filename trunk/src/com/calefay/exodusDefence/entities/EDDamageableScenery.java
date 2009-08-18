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


import com.calefay.exodusDefence.EntityRegister;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameWorldInfo;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Spatial;

public class EDDamageableScenery implements GameEntity {

	private String name = null;
	private String faction = "default";
	private float hitPoints = 0;
	private Spatial scenerySpatial = null;
	private boolean destroyed = false;
	
	public EDDamageableScenery(String name, Spatial model, float hitPoints) {
		this.name = name;
		this.hitPoints = hitPoints;
		destroyed = false;
		
		this.scenerySpatial = model;
		EntityRegister.getEntityRegister().registerGeometryEntity(scenerySpatial, this);
	}
	
	public boolean handleAction(GameEvent action) {
		if(action.getEventType() == "damage") {
			float damageAmount = (Float)action.getEventTarget(); // TODO: Cast may fail.
			hitPoints -= damageAmount;
			
			if(hitPoints <= 0) {
				hitPoints = 0;
				if(!destroyed) {
					GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("EntityDestroyed",  "gameplayEvents", this, null);
					GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("addDamageSmoke",  "visualEffects", this, null);
					destroyed = true;
				}
			}
			return true;
			} else {return false;}
	}
	
	public String getFaction() {return faction;}
	public void setFaction(String faction) {this.faction = faction;}
	public String getName() {return name;}
	public Vector3f getPosition() {return scenerySpatial.getWorldTranslation();}
	public Vector3f getVelocity() {return Vector3f.ZERO;};
	public Quaternion getRotation() {return scenerySpatial.getWorldRotation();}
	public boolean isDead() {return destroyed;}
	
	public void cleanup() {
		name = null;
		hitPoints = 0; destroyed = true;
		if(scenerySpatial != null) {
			scenerySpatial.removeFromParent();
			EntityRegister.getEntityRegister().deRegisterGeometryEntity(scenerySpatial);
			scenerySpatial = null;
		}
	}
}
