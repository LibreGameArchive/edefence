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

import java.util.Random;

import com.jme.math.FastMath;
import com.jme.math.Vector3f;
import com.jmex.terrain.TerrainBlock;

public class EDMap {
	public enum EDMapEdge {NORTH, SOUTH, EAST, WEST};
	
	private TerrainBlock terrain = null;
	private float xBound = 0, zBound = 0;
	private Random rand = null;
	
	public EDMap(TerrainBlock tb) {
		if(tb != null) setTerrainBlock(tb);
		rand = new Random();
	}
	
	public void setTerrainBlock(TerrainBlock tb) {terrain = tb; resetBounds();}
	public float getHeightFromWorld(Vector3f pos) {if(terrain == null) return Float.NEGATIVE_INFINITY; else return terrain.getHeightFromWorld(pos);}
	public float getHeightFromLocal(float posX, float posZ) {return terrain.getHeight(posX, posZ);}
	public EDMapEdge getRandomMapEdge() {return EDMapEdge.values()[rand.nextInt(4)];}	// Not sure if this is the best way but hey...
	public Vector3f getRandomEdgeLocation() {return getRandomEdgeLocation(getRandomMapEdge());}
	public Vector3f getRandomEdgeLocation(float altitude) {return getRandomEdgeLocation(getRandomMapEdge(), altitude);}
	
	/** Takes a position in WORLD coordinates, and returns true if it lies outside the map area.*/
	public boolean isOutofBounds(Vector3f pos) {
		Vector3f posInLocal = terrain.worldToLocal(pos, new Vector3f());
		if( (posInLocal.x < 0) || (posInLocal.z < 0) || (posInLocal.x > xBound) ||(posInLocal.z > zBound) ) return true;
		return false;
	}
	
	private void resetBounds() {
		xBound = (terrain.getSize() - 1) * terrain.getStepScale().x;
		zBound = (terrain.getSize() - 1) * terrain.getStepScale().z;
	}
	/* Gets the height value directly from the heightmap without doing any bound checks, or interpolating values.
	 * Should be significantly faster, but checks MUST be done before passing values to it.
	 * Will not be as accurate as getHeight(), especially on rough terrain due to lack of interpolation.
	 * Intended for line checking or other methods that do many height checks in a short space of time, and don't need to recheck values each iteration.
	 * NOTE: posX and posY are points on the underlying heightmap NOT scaled - so they should range from 0 to terrain.getSize()*/
	private float getHeightDirect(int posX, int posY) {
		int arrayPos = posX + (posY * terrain.getSize());
		float rawHeight = terrain.getHeightMap()[arrayPos];
		return(rawHeight * terrain.getStepScale().y);
	}
	
	public Vector3f getRandomEdgeLocation(EDMapEdge edge, float altitude) {
		Vector3f loc = getRandomEdgeLocation(edge);
		loc.y += altitude;
		return loc;
	}
	
	public Vector3f getRandomEdgeLocation(EDMapEdge edge) {
		if(terrain == null) return new Vector3f();	// FIXME: Should be able to define the map size rather than depending on a terrain block.
		
		Vector3f location = new Vector3f();
		float sizeX = (terrain.getSize() - 1f) * terrain.getStepScale().x;
		float sizeY = (terrain.getSize() - 1f) * terrain.getStepScale().z;
		sizeX--; sizeY--; // Map size is 0..(getSize-1) but for the interpolated scaled lookups we need to be <(getSize - 1) or it returns NaN
		
		switch(edge) {
		  case NORTH: location.set(rand.nextFloat() * sizeX, 0, 0); break;
		  case SOUTH: location.set(rand.nextFloat() * sizeX, 0, sizeY); break;
		  case EAST: location.set(sizeX, 0, rand.nextFloat() * sizeY); break; 
		  case WEST: location.set(0, 0, rand.nextFloat() * sizeY); break;
		}
		 
		float height = getHeightFromLocal(location.x, location.z);
		if(Float.isNaN(height)) {height = 0;}
		location.setY(height);
		terrain.localToWorld(location, location);
		return location;
	}
	
	/* Looks for the highest point on a straight line between two points (y part of the parameters is ignored).
	 * If either of the supplied points lies outside the terrain block then NaN is returned, regardless of the rest of the line.
	 * @param interpolated - When true, does bounds checks and interpolates values, at the expense of speed.*/ 
	public float getHighestPointBetween(Vector3f p1, Vector3f p2, boolean interpolated) {
		float scaleX = terrain.getStepScale().x; float scaleZ = terrain.getStepScale().z;
		// TODO: RETEST THIS ALGORITHM, HAVE NOT PROPERLY TESTED SINCE WORLD/LOCAL NO LONGER MATCH
		Vector3f start = new Vector3f(), end = new Vector3f();
		terrain.worldToLocal(p1, start); terrain.worldToLocal(p2, end);
		// TODO: Instead of returning NaN, replace the point which lies outside with the point where the line enters the map (if at all).
		// TODO: Consider returning the full location of the highest point, not just the height (might not fit easily with the line algorithm).
		int x0 = (int)(start.x / scaleX); int y0 = (int)(start.z / scaleZ);
		int x1 = (int)(end.x / scaleX); int y1 = (int)(end.z / scaleZ);
		int size = terrain.getSize() - 1;
		if( (x0 > size) || (x1 > size) ||( y0 > size) || (y1 > size) ||
				(x0 < 0) || (x1 < 0) || (y0 < 0) || (y1 < 0)) return Float.NaN;
		
		int Dx = x1 - x0; 
		int Dy = y1 - y0;
		boolean steep = (FastMath.abs(Dy) >= FastMath.abs(Dx));
		if (steep) {
			int temp = x0; x0 = y0; y0 = temp; // Just swapping values around
			temp = x1; x1 = y1; y1 = temp;
			Dx = x1 - x0;
			Dy = y1 - y0;
		}
		int xstep = 1;
		if (Dx < 0) {
			xstep = -1;
			Dx = -Dx;
		}
		int ystep = 1;
		if (Dy < 0) {
			ystep = -1;		
			Dy = -Dy; 
		}
		int TwoDy = 2 * Dy; 
		int TwoDyTwoDx = TwoDy - 2 * Dx;
		int E = TwoDy - Dx;
		int y = y0;
		int xLookup, yLookup;	
		float maxHeight = 0; float currentHeight = 0;
		   
		for (int x = x0; x != x1; x += xstep) {		
			if (steep) {			
				xLookup = y;
				yLookup = x;
			} else {			
				xLookup = x;
				yLookup = y;
			}
			
			if(interpolated) {
				currentHeight = getHeightFromLocal(xLookup * scaleX, yLookup * scaleZ);
			} else {
				currentHeight = getHeightDirect(xLookup, yLookup);
			}
			
			if(currentHeight > maxHeight) maxHeight = currentHeight;
	
		    
			if (E > 0) {
				E += TwoDyTwoDx;
				y = y + ystep;
			} else {
				E += TwoDy;
			}
		}
		return maxHeight;
	}
	
	public void cleanup() {
		terrain = null;
	}
}
