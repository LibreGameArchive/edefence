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

import java.io.IOException;
import java.util.ArrayList;

import com.jme.math.Quaternion;
import com.jme.math.Vector3f;

public class GROMEParser {
	/*************** GROME Loading Methods Begin ********************/
	// TODO: Split GROME loading methods into their own class, preferably doing all the work of adding entities in through prefabs - GROME loader will pass the prefab name, spawn position etc to a factory method.
	public static ArrayList<GROMEObjectInstance> loadGROMEScene(String filename) {
		ArrayList<GROMEObjectInstance> instances = new ArrayList<GROMEObjectInstance>();
		
		try {
			BracketedTextParser reader = new BracketedTextParser();
			reader.openSourceFile(filename);
			
			parseGROMEBlock(reader, instances);
			
			reader.closeSourceFile();
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
		
		return instances;
	}
	
	/* Parses the block, adding instances into the ArrayList.*/
	private static void parseGROMEBlock(BracketedTextParser parser, ArrayList<GROMEObjectInstance> instances) {
		if(instances == null) return;
		StringBuffer block = null;
		
		try {
			do {
				block = parser.readBlock();
				if(block != null) {
					// These recursive calls are very memory inefficient at the moment, as multiple copies of the same data are held in memory (ie the whole block at this level, plus the section sent to the next level.. for however many levels)
			    	if(block.toString().trim().equals("TerrainZone")) {
			    		parseGROMEBlock(new BracketedTextParser(parser.readBlock()), instances);
			    	} else if(block.toString().trim().equals("Instances")) {
			    		parseGROMEBlock(new BracketedTextParser(parser.readBlock()), instances);
			    	} else if(block.toString().trim().equals("ObjectContainer")) {
			    		parseGROMEBlock(new BracketedTextParser(parser.readBlock()), instances);
			    	} else if(block.toString().trim().equals("Instance")) {
			    		GROMEObjectInstance instance = processGROMEInstance(BracketedTextParser.parseAttributeSet(parser.readBlock()));
			    		if(instance != null) instances.add(instance);
			    	}
				}
			} while (block != null);
		} catch (IOException e) {
			System.err.println("Error parsing BFT block: " + e.toString());
		}
		
	}
    
    public static GROMEObjectInstance processGROMEInstance(AttributeSet data) {
    	Vector3f posVec = null, scale = null;
    	Quaternion rotQ = null;
    	String template = null, name = null;
    	String[] factions = null;
    	String[] flags = null;
    	String[] strTemp = null; float[] fTemp = null;
    	
    	if(data.hasAttribute("name")) {
    		strTemp = data.getStringAttribute("name");
    		if(strTemp.length > 0) name = strTemp[0];
    	}
    	if(data.hasAttribute("template")) {
    		strTemp = data.getStringAttribute("template");
    		if(strTemp.length > 0) template = strTemp[0];
    	}
    	if(data.hasAttribute("flags")) {
    		strTemp = data.getStringAttribute("flags");
    		if(strTemp.length > 0) flags = strTemp;
    	}

    	factions = data.getStringAttribute("factions");

    	if(data.hasAttribute("link_pos")) {
    		fTemp = data.getFloatAttribute("link_pos");
    		if(fTemp.length == 3) {posVec = new Vector3f(fTemp[0], fTemp[1], fTemp[2]);} 
    	}
    	if(data.hasAttribute("tran")) {
    		fTemp = data.getFloatAttribute("tran");
    		if(fTemp.length == 3) {
    			if(posVec == null) posVec = new Vector3f(0, 0, 0);
    			posVec.addLocal(fTemp[0], fTemp[1], fTemp[2]);
    			} 
    	}
    	if(data.hasAttribute("scale")) {
    		fTemp = data.getFloatAttribute("scale");
    		if(fTemp.length == 3) {scale = new Vector3f(fTemp[0], fTemp[1], fTemp[2]);} 
    	}
    	if(data.hasAttribute("rot")) {
    		fTemp = data.getFloatAttribute("rot");
    		if(fTemp.length == 3) {
    			fTemp[0] = -fTemp[0]; fTemp[1] = -fTemp[1]; fTemp[2] = -fTemp[2];	// FIXME: Have to reverse angles; Find a better way!
    			rotQ = new Quaternion(); 
    			rotQ.fromAngles(fTemp);
    		}
    	}
    	
    	GROMEObjectInstance instance = null;
    	if( (posVec != null) && (template != null) ) {
    		instance = new GROMEObjectInstance(name, template, posVec, scale, rotQ, factions, flags);
    	} else {
    		if(posVec == null) System.err.println("Error parsing GROME instance - no or malformed position.");
        	if(template == null) System.err.println("Error parsing GROME instance - no template.");
    	}
    	
    	return instance;
    }
    
}
