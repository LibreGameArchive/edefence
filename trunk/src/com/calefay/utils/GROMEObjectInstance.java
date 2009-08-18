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
package com.calefay.utils;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

public class GROMEObjectInstance {
	private String name = null, template = null;
	private Vector3f position = null, scale = null;
	private Quaternion rotation = null;
	private String[] factions = null, flags = null;
	
	public GROMEObjectInstance( String name, String template,
								Vector3f position, Vector3f scale, 
								Quaternion rotation,
								String[] factions, String[] flags) {
		this.name = name;
		this.template = template;
		this.position = position;
		this.scale = scale;
		this.rotation = rotation;
		this.factions = factions;
		this.flags = flags;
	}

	public String getName() {return name;}
	public String getTemplate() {return template;}
	public Vector3f getPosition() {return position;}
	public Vector3f getScale() {return scale;}
	public Quaternion getRotation() {return rotation;}
	public String[] getFactions() {return factions;}
	public String[] getFlags() {return flags;}
	
	/* Helper method to check if a specified flag is contained in the flags array.*/
    public static boolean flagPresent(String searchFor, String[] searchIn) {	// Tacky
    	if( (searchIn == null) || (searchFor == null) ) return false;
    	for(int i = 0; i < searchIn.length; i++) {
    		if( searchIn[i].equals(searchFor)) return true;
    	}
    	return false;
    }

}
