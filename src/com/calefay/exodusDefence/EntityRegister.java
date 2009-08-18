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

import java.util.HashMap;
import java.util.Map;

import com.calefay.utils.GameActionable;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.jme.scene.Node;
import com.jme.scene.Spatial;

public class EntityRegister {
	// TODO: Consider whether it's best for this to be a Singleton.
	private static EntityRegister register;
	
	private HashMap<Spatial, GameEntity> entityXRef;
	
	private EntityRegister() {
		entityXRef = new HashMap<Spatial, GameEntity>();	// TODO: Might want to include initial capacity and load factor here.
	}
	
	public static void createEntityRegister() {	// Not created in the get method so that it can be seen to be gone after cleanup.
		register = new EntityRegister();
	}
	
	public static EntityRegister getEntityRegister() {	// Guess this is not thread safe
		return register; 
	}
	
	public Object clone() throws CloneNotSupportedException
	{
		throw new CloneNotSupportedException(); 
	}
	  	
	public void registerGeometryEntity(Node geomNode, GameEntity entity) {
		if( (geomNode == null) || (geomNode.getQuantity() == 0) || (entity == null)) return;
		for(Spatial geometry : geomNode.getChildren() ) {
			if(geometry instanceof Node) {
				Node childNode = (Node)geometry;
				registerGeometryEntity(childNode, entity);
			} else {
				registerGeometryEntity(geometry, entity);
			}
		}
	}
	
	public void registerGeometryEntity(Spatial geometry, GameEntity entity) {
		if( (geometry == null) || (entity == null) || !geometry.isCollidable() ) return;
		entityXRef.put(geometry, entity);
	}

	public void deRegisterGeometryEntity(Node geomNode) {
		if( (geomNode == null) || (geomNode.getQuantity() == 0) ) return;
		for(Spatial geometry : geomNode.getChildren() ) {
			if(geometry instanceof Node) {
				Node childNode = (Node)geometry;
				deRegisterGeometryEntity(childNode);
			} else {
				deRegisterGeometryEntity(geometry);
			}
		}
	}
	
	public void deRegisterGeometryEntity(Spatial geometry) {
		if(geometry == null) return;
		if(geometry instanceof Node) deRegisterGeometryEntity((Node)geometry);
		entityXRef.remove(geometry);
	}

	/** Looks up and returns the GameActionable which owns geometry.*/
	public GameEntity lookupEntity(Spatial geometry) {
		return entityXRef.get(geometry);
	}
	
	/** Attempts to perform action on the entity that owns geometry. Returns true only if the entity was found and successfully handled the action.*/
	public boolean actUpon(Spatial geometry, GameEvent action) {
		GameActionable entity = lookupEntity(geometry);
		if( (entity != null) && entity.handleAction(action)) {return true;}
		return false;
	}
	
	/** Empties out the register*/
	public void flush() {
		/** Might want to cleanup each entry aswell, but currently that is be done elsewhere.*/
		entityXRef.clear();
	}
	
	public void cleanup() {
		flush();
		entityXRef = null;
		register = null;
	}
	
	// Debugging/Informational methods
	public void printRegisterSize() {
		System.out.println("HashMap size: " + entityXRef.size());
	}
	
	public void printRegister() {
		System.out.println("*************Register dump begins ***********");
		for (Map.Entry<Spatial, GameEntity> a : entityXRef.entrySet()) {
			System.out.println(" Key: " + a.getKey() + " value: " + a.getValue().toString());
		}
		System.out.println("*************Register dump ends ***********");
	}
}

