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

import java.util.ArrayList;
import java.util.Random;

import com.calefay.utils.GameEntity;
import com.jme.math.FastMath;
import com.jme.math.Vector2f;

/* This will handle a large, sparsely populated map with fast moving objects ok, but will need a full rewrite to handle detailed scenes efficiently.
 */
public class EDEntityMap {

	private ArrayList<GameEntity> allEntities = null;
	private Random rand = null;
	private ArrayList<GameEntity> resultSet = null;
	
	public EDEntityMap() {
		allEntities = new ArrayList<GameEntity>();
		resultSet = new ArrayList<GameEntity>();
		
		rand = new Random();
	}
	
	public void addEntity(GameEntity entity) {
		if(entity != null) allEntities.add(entity);
	}
	
	public void removeEntity(GameEntity entity) {
		allEntities.remove(entity);
	}
	
	public GameEntity getRandomEntity() {
		if(allEntities.size() == 0) {return null;}
		else {
			return allEntities.get( rand.nextInt(allEntities.size()));
		}
	}
	
	/* Performs a sequential search for an entity with a matching name. Not an optimized routine. Will only return the first match if there is more than one. Not case sensitive.*/
	public GameEntity getNamedEntity(String name) {
		GameEntity match = null;
		for(GameEntity e : allEntities) {
			if(e.getName().equalsIgnoreCase(name)) match = e;
		}
		return match;
	}
	
	public GameEntity getRandomEntityInCircle(Vector2f origin, float radius) {
		ArrayList<GameEntity> possibles = getEntitiesInCircle(origin, radius);
		if(possibles.size() == 0) {return null;}
		else {
			GameEntity ge = possibles.get( rand.nextInt(possibles.size()));
			possibles.clear(); possibles = null;
			return ge;
		}
	}
	
	/* Returns an ArrayList of all GameEntities which fall within the supplied triangle.
	 * NOTE: Current implementation checks every entity - not very efficient for crowded maps with slow moving entities.*/
	public ArrayList<GameEntity> getEntitiesInTriangle(Vector2f a, Vector2f b, Vector2f c) {
		resultSet.clear();
		for(GameEntity entity : allEntities) {
			if(inTriangle(entity, a, b, c)) {
				resultSet.add(entity);	// Perhaps not efficient to add all new elements every time.
			}
		}
		return resultSet;
	}
	
	/* Returns an ArrayList of all GameEntities which fall within the supplied triangle.
	 * NOTE: Current implementation checks every entity - not very efficient for crowded maps with slow moving entities.*/
	public ArrayList<GameEntity> getEntitiesInCircle(Vector2f origin, float radius) {
		resultSet.clear();
		for(GameEntity entity : allEntities) {
			if(inCircle(entity, origin, radius)) {
				resultSet.add(entity);	// Perhaps not efficient to add all new elements every time.
			}
		}
		return resultSet;
	}
	
	/* Returns an ArrayList of all GameEntities which fall within the supplied triangle.
	 * NOTE: Current implementation checks every entity - not very efficient for crowded maps with slow moving entities.*/
	public ArrayList<GameEntity> getEntitiesInTriangle(Vector2f origin, float length, float turnAngle, float fovAngle) {
		resultSet.clear();
		for(GameEntity entity : allEntities) {
			if(inTriangle(entity, origin, length, turnAngle, fovAngle)) {
				resultSet.add(entity);	// Perhaps not efficient to add all new elements every time.
			}
		}
		return resultSet;
	}
	
	/* Note that this is intended to define a view arc, but it is imperfect as the shape is a triangle - so it is not length long directly ahead.*/
	@Deprecated
	public boolean inTriangle(GameEntity entity, Vector2f origin, float length, float turnAngle, float fovAngle) {	// FIXME: There is a FastMath method to do this.
		if( (entity == null) || (entity.isDead()) ) return false;
		
		float halfAngle = fovAngle / 2;
		float cosAngle = FastMath.cos(-turnAngle + halfAngle);
		float sinAngle = FastMath.sin(-turnAngle + halfAngle);
		// This must be mathematically horrible...
		Vector2f b = new Vector2f( origin.x - (length * sinAngle), origin.y + (length * cosAngle) );
		cosAngle = FastMath.cos(-turnAngle - halfAngle);
		sinAngle = FastMath.sin(-turnAngle - halfAngle);
		Vector2f c = new Vector2f(  origin.x - (length * sinAngle), origin.y + (length * cosAngle)  );

		return inTriangle(entity, origin, b, c);
	}
	
	private Vector2f p = new Vector2f();
	private Vector2f v0 = new Vector2f(), v1 = new Vector2f(), v2 = new Vector2f();
	private float dot00, dot01, dot02, dot11, dot12;
	/* Returns true if the given entity is within a 2D defined area of the map (a triangle).*/
	public boolean inTriangle(GameEntity entity, Vector2f a, Vector2f b, Vector2f c) {
		if( (entity == null) || (entity.isDead()) ) return false;
		
		p.set(entity.getPosition().x, entity.getPosition().z);
		c.subtract(a, v0);
		b.subtract(a, v1);
		p.subtract(a, v2);
		dot00 = v0.dot(v0);
		dot01 = v0.dot(v1);
		dot02 = v0.dot(v2);
		dot11 = v1.dot(v1);
		dot12 = v1.dot(v2);
		
		float invDenom = 1 / (dot00 * dot11 - dot01 * dot01);
		float u = (dot11 * dot02 - dot01 * dot12) * invDenom;
		float v = (dot00 * dot12 - dot01 * dot02) * invDenom;

		return (u > 0) && (v > 0) && (u + v <= 1);
	}
	
	/* Returns true if the given entity is within a 2D defined area of the map (a circle).*/
	public boolean inCircle(GameEntity entity, Vector2f centre, float radius) {
		if( (entity == null) || (entity.isDead()) ) return false;
		
		p.set(entity.getPosition().x, entity.getPosition().z);
		return inCircle(p, centre, radius);
	}
	
	private boolean inCircle(Vector2f pos, Vector2f centre, float radius) {
		return (centre.subtract(pos).lengthSquared() <= (radius * radius));
	}
	
	/* Returns true if the given entity is within a 2D defined area of the map (a hollow circle).*/
	public boolean inRing(GameEntity entity, Vector2f centre, float innerRadius, float outerRadius) {
		if( (entity == null) || (entity.isDead()) ) return false;
		
		p.set(entity.getPosition().x, entity.getPosition().z);
		return (inCircle(p, centre, outerRadius) && !inCircle(p, centre, innerRadius));
	}
	
	public void listEntities() {
		System.out.println("--- Entity list begins ---");
		for(GameEntity n : allEntities) {
			System.out.println(n.getClass());
		}
		System.out.println("---  Entity list ends  ---");
	}
	
	public void flush() {
		if(allEntities != null) allEntities.clear();
		if(resultSet != null) resultSet.clear();
	}
	
	public void cleanup() {
		if(allEntities != null) allEntities.clear(); allEntities = null;
		if(resultSet != null) resultSet.clear(); resultSet = null;
		rand = null;
	}
}
