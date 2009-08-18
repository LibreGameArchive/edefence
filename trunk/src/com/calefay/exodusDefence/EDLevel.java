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

import java.io.IOException;
import java.util.ArrayList;


import com.calefay.exodusDefence.mission.EDMission;
import com.calefay.exodusDefence.mission.EDObjective;
import com.calefay.exodusDefence.mission.EntityDestroyedObjective;
import com.calefay.exodusDefence.mission.EventOccurredObjective;
import com.calefay.utils.AttributeSet;
import com.calefay.utils.BracketedTextParser;
import com.calefay.utils.GameEvent;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;

public class EDLevel {
	public enum WeatherTypes {NONE, RAIN, SNOW}
	public enum LightType {DIRECTIONAL, POINT, SPOT};
	public enum skyboxSides {North, South, East, West, Up, Down};

	public static final String[] tutorialPaths = {"data/scripts/turret_tutorial.txt", "data/scripts/turret_tutorial.txt"};	// FIXME: UGLY HARDCODED TUTORIAL PATHS
	public static String[] levelNames = null, levelPaths = null;
	
	private String levelName = null;
	private String[] factions = null;
	
	private String resourceFilePath = null;
	private String sceneryFilePath = null, foliageFilePath = null;
	private String heightMapPath = null;
	private int heightMapSize = 0;
	private float mapMinHeight = 0, mapMaxHeight = 0;
	private float terrainScale = 1.0f;
	public SplatLayer splatBaseLayer = null; // FIXME: Don't like these public fields.
	public SplatLayer splatLightMap = null;
	public ArrayList<SplatLayer> splatDetailLayers = null;

	public Vector3f initialCameraPos = null;
	
	public Vector3f playerBoundsMin = null, playerBoundsMax = null;
	public Vector3f projectileBoundsMin = null, projectileBoundsMax = null;
	public float turretRange = 1000f;
	
	private String[] skyTextures = null;
	private float[] fogData = null;

	public ArrayList<LightData> lights = null;

	public ArrayList<EDMission> missions = null;
	
	private WeatherTypes weather = null;
	private boolean useWaterPlane = false;
	private float waterHeight = 30;
	
	public EDLevel() {
		levelName = "Blank Level";
	}
	
	public EDLevel(String name) {
		this();
		levelName = name;
	}
			
	public void setResourceFilePath(String path) {resourceFilePath = path;}
	public void setWeatherType(WeatherTypes type) {weather = type;}
	public void addWater(float waterHeight) {this.useWaterPlane = true; this.waterHeight = waterHeight;}
	public void setLevelName(String name) {this.levelName = name;}
	public void setSceneryFilePath(String path) {this.sceneryFilePath = path;}
	public void setFoliageFilePath(String path) {this.foliageFilePath = path;}
	
	public void setHeightMapData(String mapPath, int mapSize, float mapScale, float minHeight, float maxHeight) {
		this.heightMapPath = mapPath;
		this.heightMapSize = mapSize;
		this.terrainScale = mapScale;
		this.mapMinHeight = minHeight; this.mapMaxHeight = maxHeight;
	}
	
	public void addSplatBaseLayer(String texture, int tiling) {
		if(texture == null) return;
		splatBaseLayer = new SplatLayer(null, texture, tiling);
	}
	
	public void addSplatLightMap(String texture, int modulateScale) {
		if(texture == null) return;
		if( modulateScale == 1 || modulateScale == 2 || modulateScale == 4 ) {
			System.err.println("Invalid modulate scale for lightmap - using 1.");
			modulateScale = 1;
		}
		splatLightMap = new SplatLayer(null, texture, modulateScale);
	}
	
	public void addSplatDetailLayer(String mask, String texture, int tiling) {
		if(texture == null) return;
		if(splatDetailLayers == null) splatDetailLayers = new ArrayList<SplatLayer>();
		splatDetailLayers.add( new SplatLayer(mask, texture, tiling) );
	}
	
	public void setSkyboxTexturePaths(String north, String south, String east, String west, String up, String down) {
		if(skyTextures == null) skyTextures = new String[6];
		skyTextures[skyboxSides.North.ordinal()] = north;
		skyTextures[skyboxSides.South.ordinal()] = south;
		skyTextures[skyboxSides.East.ordinal()] = east;
		skyTextures[skyboxSides.West.ordinal()] = west;
		skyTextures[skyboxSides.Up.ordinal()] = up;
		skyTextures[skyboxSides.Down.ordinal()] = down;
	}
	
	public void setFogSettings(float start, float end, float density,
								float colorR, float colorG, float colorB, float colorA) {
		fogData = new float[] {start, end, density, colorR, colorG, colorB, colorA};
	}
	
	/* Returns fogSettings (or null if none present).
	 * Returned values, in order are: start, end, density, colorR, colorG, colorB, colorA. */
	public float[] getFogSettings() {return fogData;}
	
	/* Sets the skybox texture paths. Takes an array of 6 strings. If the size is not 6, it will be ignored.
	 * Order is north, south, east, west, up, down.*/
	public void setSkyboxTexturePaths(String[] paths) {
		if(paths.length != 6) {System.err.println("Skybox texture path array was the wrong size - ignored."); return;}
		skyTextures = paths;
	}
	
	public void addLight(LightType type, ColorRGBA ambient, ColorRGBA diffuse, ColorRGBA specular, 
			Vector3f direction, Vector3f location) {
		if(lights == null) lights = new ArrayList<LightData>();
		lights.add( new LightData(type, ambient, diffuse, specular, direction, location) );
	}
	
	public void addMission(EDMission mission) {
		if(missions == null) missions = new ArrayList<EDMission>();
		missions.add(mission);
	}
	
	public String getSkyboxTexturePath(skyboxSides side) {
		if( (skyTextures == null) || (skyTextures.length < (side.ordinal() - 1) )) return null;
		return skyTextures[ side.ordinal() ];
	}
	
	public String getResourceFilePath() {return resourceFilePath;}
	public String getMapPath() {return heightMapPath;}
	public String getSceneryFilePath() {return sceneryFilePath;}
	public String getFoliageFilePath() {return foliageFilePath;}
	public int getMapSize() {return heightMapSize;}
	public float getTerrainScale() {return terrainScale;}
	public float getMapMinHeight() {return mapMinHeight;}
	public float getMapMaxHeight() {return mapMaxHeight;}
	public String getLevelName() {return levelName;}
	public WeatherTypes getWeatherType() {return weather;}
	public boolean hasWaterPlane() {return useWaterPlane;}
	public float getWaterHeight() {return waterHeight;}
	public String[] getFactions() {return factions;}
	
	public class SplatLayer {
		public String texture = null;
		public String mask = null;
		public int scale = 1;
		
		public SplatLayer(String mask, String texture, int tiling) {
			this.mask = mask; this.texture = texture; 
			if(tiling > 0) this.scale = tiling;
		}
	}
	
	public class LightData {
		public LightType type;
		public ColorRGBA ambient, diffuse, specular;
		public Vector3f location, direction;
		
		public LightData(LightType type, ColorRGBA ambient, ColorRGBA diffuse, ColorRGBA specular) {
			location = null; direction = null;
			this.type = type;
			this.ambient = ambient;
			this.diffuse = diffuse;
			this.specular = specular;
		}
		public LightData(LightType type, ColorRGBA ambient, ColorRGBA diffuse, ColorRGBA specular, 
				Vector3f direction, Vector3f location) {
			this(type, ambient, diffuse, specular);
			this.direction = direction;
			this.location = location;
		}
	}
	
	public void cleanup() {
		
	}
	/*************************** LEVEL LOADER METHODS FOLLOW **********************************/
	
	public static EDLevel loadLevel(String levelPath) {
		if(levelPath == null) {System.err.println("Error loading level - path was null."); return null;}
		EDLevel loadingLevel = null;

		try {
			BracketedTextParser reader = new BracketedTextParser();
			reader.openSourceFile(levelPath);
			
			loadingLevel = parseLevel(reader);
			
			reader.closeSourceFile();
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
		
		return loadingLevel;
	}
	
	private static EDLevel parseLevel(BracketedTextParser parser) {	// This doesn't really need to be a method in its own right.
		EDLevel level = new EDLevel();
		StringBuffer block;
		
		try {
			do {
				block = parser.readBlock();
				if(block != null) {
					// These recursive calls are very memory inefficient at the moment, as multiple copies of the same data are held in memory (ie the whole block at this level, plus the section sent to the next level.. for however many levels)
					if(block.toString().trim().equalsIgnoreCase("skybox")) {
						parseSkybox(BracketedTextParser.parseAttributeSet(parser.readBlock()), level);
					}
					if(block.toString().trim().equalsIgnoreCase("light")) {
						parseLight(BracketedTextParser.parseAttributeSet(parser.readBlock()), level);
					}
					if(block.toString().trim().equalsIgnoreCase("fog")) {
						parseFog(BracketedTextParser.parseAttributeSet(parser.readBlock()), level);
					}
					if(block.toString().trim().equalsIgnoreCase("levelheader")) {
						parseLevelHeader(BracketedTextParser.parseAttributeSet(parser.readBlock()), level);
					}
					if(block.toString().trim().equalsIgnoreCase("terrain")) {
						parseTerrainData(new BracketedTextParser(parser.readBlock()), level);
					}
					if(block.toString().trim().equalsIgnoreCase("mission")) {
						EDMission mission = parseMission(new BracketedTextParser(parser.readBlock()), level);
						if(mission != null) level.addMission(mission);
					}
				}
			} while (block != null);
		} catch (IOException e) {
			System.err.println("Error parsing BFT block: " + e.toString());
		}
		
		return level;
	}
	
	
	
    private static void parseSkybox(AttributeSet data, EDLevel level) {
    	String[] texturePaths = new String[6];
    	String[] attributeData = null;
    	if(data.hasAttribute("north")) {
    		attributeData = data.getStringAttribute("north");
    		if(attributeData.length > 0) texturePaths[0] = attributeData[0];
    	}
    	if(data.hasAttribute("south")) {
    		attributeData = data.getStringAttribute("south");
    		if(attributeData.length > 0) texturePaths[1] = attributeData[0];
    	}
    	if(data.hasAttribute("east")) {
    		attributeData = data.getStringAttribute("east");
    		if(attributeData.length > 0) texturePaths[2] = attributeData[0];
    	}
    	if(data.hasAttribute("west")) {
    		attributeData = data.getStringAttribute("west");
    		if(attributeData.length > 0) texturePaths[3] = attributeData[0];
    	}
    	if(data.hasAttribute("up")) {
    		attributeData = data.getStringAttribute("up");
    		if(attributeData.length > 0) texturePaths[4] = attributeData[0];
    	}
    	if(data.hasAttribute("down")) {
    		attributeData = data.getStringAttribute("down");
    		if(attributeData.length > 0) texturePaths[5] = attributeData[0];
    	}
    	level.setSkyboxTexturePaths(texturePaths);
    }

    private static void parseLight(AttributeSet data, EDLevel level) {
    	String[] attrS = null; float[] attrF = null;
    	LightType type = null;
    	ColorRGBA diffuse = null, ambient = null, specular = null;
    	Vector3f direction = null; Vector3f location = null;
    	
    	if(data.hasAttribute("type")) {
    		attrS = data.getStringAttribute("type");
    		if(attrS.length == 1) {
    			if(attrS[0].trim().toLowerCase().equals("directional")) type = LightType.DIRECTIONAL;
    			if(attrS[0].trim().toLowerCase().equals("point")) type = LightType.POINT;
    			if(attrS[0].trim().toLowerCase().equals("spot")) type = LightType.SPOT;
    		}
    	}
    	if(data.hasAttribute("direction")) {
    		attrF = data.getFloatAttribute("direction");
    		if(attrF.length == 3) {
    			direction = new Vector3f(attrF[0], attrF[1], attrF[2]).normalizeLocal();
    		}
    	}
    	if(data.hasAttribute("location")) {
    		attrF = data.getFloatAttribute("location");
    		if(attrF.length == 3) {
    			location = new Vector3f(attrF[0], attrF[1], attrF[2]);
    		}
    	}
    	if(data.hasAttribute("ambient")) {
    		attrF = data.getFloatAttribute("ambient");
    		if(attrF.length == 4) {
    			ambient = new ColorRGBA(attrF[0], attrF[1], attrF[2], attrF[3]);
    		}
    	}
    	if(data.hasAttribute("diffuse")) {
    		attrF = data.getFloatAttribute("diffuse");
    		if(attrF.length == 4) {
    			diffuse = new ColorRGBA(attrF[0], attrF[1], attrF[2], attrF[3]);
    		}
    	}
    	if(data.hasAttribute("specular")) {
    		attrF = data.getFloatAttribute("specular");
    		if(attrF.length == 4) {
    			specular = new ColorRGBA(attrF[0], attrF[1], attrF[2], attrF[3]);
    		}
    	}
    	if(type != null) {
    		level.addLight(type, ambient, diffuse, specular, direction, location);
    	}
    }
    
    private static void parseFog(AttributeSet data, EDLevel level) {
    	float[] attrF = null;
    	float[] color = null;
    	float start = 0f, end = 0f, density = 0f;
    	
    	attrF = data.getFloatAttribute("color");
    	if( (attrF != null) && (attrF.length == 4)) {color = attrF;}
    	
    	attrF = data.getFloatAttribute("start");
    	if( (attrF != null) && (attrF.length == 1)) {start = attrF[0];}
    	attrF = data.getFloatAttribute("end");
    	if( (attrF != null) && (attrF.length == 1)) {end = attrF[0];}
    	attrF = data.getFloatAttribute("density");
    	if( (attrF != null) && (attrF.length == 1)) {density = attrF[0];}
    	
    	if( (color != null) && (end > 0f) && (density > 0f) ) {
    		level.setFogSettings(start, end, density, color[0], color[1], color[2], color[3]);
    	}
    }
    
    private static void parseLevelHeader(AttributeSet data, EDLevel level) {
    	String[] attributeData = null; float[] floatData = null;
    	if(data.hasAttribute("name")) {
    		attributeData = data.getStringAttribute("name");
    		if(attributeData.length > 0) level.setLevelName(attributeData[0]);
    	}
    	if(data.hasAttribute("resourcefile")) {
    		attributeData = data.getStringAttribute("resourcefile");
    		if(attributeData.length > 0) level.setResourceFilePath(attributeData[0]);
    	}
    	if(data.hasAttribute("turretrange")) {
    		floatData = data.getFloatAttribute("turretrange");
    		if(floatData.length > 0) level.turretRange = floatData[0];
    	}
    	if(data.hasAttribute("initialcamerapos")) {
    		floatData = data.getFloatAttribute("initialcamerapos");
    		if(floatData.length == 3) {
    			level.initialCameraPos = new Vector3f( floatData[0], floatData[1], floatData [2]);
    		}
    	}
    	if(data.hasAttribute("playerbounds")) {
    		floatData = data.getFloatAttribute("playerbounds");
    		if(floatData.length == 6) {
    			level.playerBoundsMin = new Vector3f( floatData[0], floatData[2], floatData [4]);
    			level.playerBoundsMax = new Vector3f( floatData[1], floatData[3], floatData [5]);
    		}
    	}
    	if(data.hasAttribute("projectilebounds")) {
    		floatData = data.getFloatAttribute("projectilebounds");
    		if(floatData.length == 6) {
    			level.projectileBoundsMin = new Vector3f( floatData[0], floatData[2], floatData [4]);
    			level.projectileBoundsMax = new Vector3f( floatData[1], floatData[3], floatData [5]);
    		}
    	}
    	if(data.hasAttribute("sceneryfile")) {
    		attributeData = data.getStringAttribute("sceneryfile");
    		if(attributeData.length > 0) level.setSceneryFilePath(attributeData[0]);
    	}
    	if(data.hasAttribute("foliagefile")) {
    		attributeData = data.getStringAttribute("foliagefile");
    		if(attributeData.length > 0) level.setFoliageFilePath(attributeData[0]);
    	}
    	if(data.hasAttribute("waterHeight")) {
    		float[] f = data.getFloatAttribute("waterHeight");
    		if(f.length > 0) level.addWater(f[0]);
    	}
    	if(data.hasAttribute("weather")) {
    		attributeData = data.getStringAttribute("weather");
    		if(attributeData.length > 0) {
    			if(attributeData[0].equalsIgnoreCase("rain")) level.setWeatherType(WeatherTypes.RAIN);
    			if(attributeData[0].equalsIgnoreCase("snow")) level.setWeatherType(WeatherTypes.SNOW);
    		}
    	}
    	level.factions = data.getStringAttribute("factions");
    }
    
    private static void parseTerrainData(BracketedTextParser parser, EDLevel level) {
		if(parser == null) {System.err.println("No input provided to parse terrain."); return;}
		StringBuffer block = null;
		String[] stringData = null;
		float[] floatData = null;
		
		try {
			do {
				block = parser.readBlock();
				if(block != null) {
					
					
					
			    	if(block.toString().trim().equalsIgnoreCase("map")) {
			    		String mapPath = null;
			    		int mapSize = 0;
			    		float mapScale = 1.0f, minHeight = 0, maxHeight = 100f;
			    		
			    		AttributeSet data = BracketedTextParser.parseAttributeSet(parser.readBlock());
			        	if(data.hasAttribute("file")) {
			        		stringData = data.getStringAttribute("file");
			        		if(stringData.length > 0) mapPath = stringData[0];
			        	}
			        	
			        	floatData = data.getFloatAttribute("heightMapsize");
			        	if((floatData != null) && (floatData.length > 0)) mapSize = (int)floatData[0];
			        	
			        	floatData = data.getFloatAttribute("scale");
			        	if((floatData != null) && (floatData.length > 0)) mapScale = floatData[0];
			        	
			        	floatData = data.getFloatAttribute("heightRange");
			        	if( (floatData != null) && (floatData.length == 2)) {minHeight = floatData[0]; maxHeight = floatData[1];}
			        	
			        	if( (mapPath != null) && (mapSize > 0) )
			        		level.setHeightMapData(mapPath, mapSize, mapScale, minHeight, maxHeight);
			        	else
			        		System.err.println("ERROR: Map data may be incomplete or corrupt.");
			    	}
			    	
			    	if(block.toString().trim().equalsIgnoreCase("detailtexture")) {
			    		parseSplatLayer( BracketedTextParser.parseAttributeSet(parser.readBlock()), level, false );
			    	}
			    	
			    	if(block.toString().trim().equalsIgnoreCase("basetexture")) {
			    		parseSplatLayer( BracketedTextParser.parseAttributeSet(parser.readBlock()), level, true );
			    	}
			    	
			    	
				}
			} while (block != null);
		} catch (IOException e) {
			System.err.println("Error parsing level terrain: " + e.toString());
		}
		
    }
    
    public static void parseSplatLayer(AttributeSet data, EDLevel level, boolean isBaseLayer) {
    	String[] stringData = null;
		float[] floatData = null;
    	String mask = null, texture = null;
		int tiling = 1;
		
		if(data == null) return;
		
		stringData = data.getStringAttribute("mask");
		if( (stringData != null) && (stringData.length > 0) ) mask = stringData[0];
		stringData = data.getStringAttribute("texture");
		if( (stringData != null) && (stringData.length > 0) ) texture = stringData[0];
    	floatData = data.getFloatAttribute("tiling");
    	if( (floatData != null) && (floatData.length > 0) ) tiling = (int)floatData[0];
    	
    	if(texture != null)
    		if(isBaseLayer)
    			level.addSplatBaseLayer(texture, tiling);
    		else
    			level.addSplatDetailLayer(mask, texture, tiling);
    			
    	else
    		System.err.println("Error parsing a terrain detail layer.");
    }
	
	private static EDMission parseMission(BracketedTextParser parser, EDLevel level) {
		if( (parser == null) || (level == null) ) {System.err.println("No input provided to parse mission."); return null;}
		StringBuffer block = null; String[] stringData = null;
		EDMission mission = new EDMission("building");
		
		try {
			do {
				block = parser.readBlock();
				if(block != null) {
					
			    	if(block.toString().trim().equalsIgnoreCase("header")) {
			    		AttributeSet data = BracketedTextParser.parseAttributeSet(parser.readBlock());
			        	stringData = data.getStringAttribute("name");
			        	if( (stringData != null) && (stringData.length > 0) ) mission.setName(stringData[0]);
			        	stringData = data.getStringAttribute("hudDisplay");
			        	if( (stringData != null) && (stringData.length > 0) && (stringData[0].equalsIgnoreCase("true")) ) mission.setShowOnHUD(true);
			    	}
			    	
			    	if(block.toString().trim().equalsIgnoreCase("completionevent")) {
			        	GameEvent completedEvent = parseEvent( BracketedTextParser.parseAttributeSet(parser.readBlock()) );
			        	mission.setCompletedEvent(completedEvent);
			    	}
			    	
			    	if(block.toString().trim().equalsIgnoreCase("objectiveset")) {
			    		parseObjectiveSet(new BracketedTextParser(parser.readBlock()), mission);
			    	}
			    	
				}
			} while (block != null);
		} catch (IOException e) {
			System.err.println("Error parsing mission: " + e.toString());
		}
		
		return mission;
	}
	
    public static void parseObjectiveSet(BracketedTextParser parser, EDMission mission) {
    	StringBuffer block = null;
    	
		try {
			do {
				block = parser.readBlock();
				if(block != null) {
			    	
			    	if(block.toString().trim().equalsIgnoreCase("objective")) {
			    		EDObjective objective = parseObjective(new BracketedTextParser(parser.readBlock()));
			    		if(objective != null) mission.addObjective(objective);
			    	}
			    	
				}
			} while (block != null);
		} catch (IOException e) {
			System.err.println("Error parsing objective set: " + e.toString());
		}
    }
    
    public static EDObjective parseObjective(BracketedTextParser parser) {
    	if(parser == null) {System.err.println("No input provided to parse objective."); return null;}
		StringBuffer block = null;
		EDObjective objective = null; GameEvent completedEvent = null, progressedEvent = null;
		
		try {
			do {
				block = parser.readBlock();
				if(block != null) {
					if(block.toString().trim().equalsIgnoreCase("header")) {
			    		objective = parseObjectiveHeader(BracketedTextParser.parseAttributeSet(parser.readBlock()));
			    	}
					if(block.toString().trim().equalsIgnoreCase("completionevent")) {
			        	completedEvent = parseEvent( BracketedTextParser.parseAttributeSet(parser.readBlock()) );
			    	}
					if(block.toString().trim().equalsIgnoreCase("progressionevent")) {
			        	progressedEvent = parseEvent( BracketedTextParser.parseAttributeSet(parser.readBlock()) );
			    	}
				}
			} while (block != null);
		} catch (IOException e) {
			System.err.println("Error parsing objectives: " + e.toString());
		}
		
		if(objective != null) {
			if(completedEvent != null) objective.setCompletedEvent(completedEvent);
			if(progressedEvent != null) objective.setProgressedEvent(progressedEvent);
		}
		return objective;
    }
  
    private static EDObjective parseObjectiveHeader(AttributeSet data) {
    	String[] stringData = null; float[] floatData = null;
    	EDObjective objective = null;
    	String type = null, hudText = null, label = null;
    	int repetitions = 1;
    	
    	stringData = data.getStringAttribute("label");
    	if((stringData != null) && (stringData.length > 0) ) label = stringData[0];
    	stringData = data.getStringAttribute("type");
    	if((stringData != null) && (stringData.length > 0) ) type = stringData[0];
    	stringData = data.getStringAttribute("hudText");
    	if((stringData != null) && (stringData.length > 0) ) hudText = stringData[0];
    	floatData = data.getFloatAttribute("repetitions");
    	if( (floatData != null) && (floatData.length > 0) ) repetitions = (int)floatData[0];
    	
    	if( 	type.equalsIgnoreCase("eventoccurred") ||
    			type.equalsIgnoreCase("entitydestroyed") ||
    			type.equalsIgnoreCase("locationreached")) {
    		
    		if(type.equalsIgnoreCase("eventoccurred")) {
    			String requiredEvent = null;
    			stringData = data.getStringAttribute("requiredEvent");
	        	if( (stringData != null) && (stringData.length > 0) ) requiredEvent = stringData[0];
	        	if(requiredEvent != null) objective = new EventOccurredObjective(new GameEvent(requiredEvent, null, null) );
    		} else if(type.equalsIgnoreCase("entitydestroyed")) {
    			String entityName = null;
    			stringData = data.getStringAttribute("entityName");
	        	if( (stringData != null) && (stringData.length > 0) ) entityName = stringData[0];
	        	if(entityName != null) objective = new EntityDestroyedObjective(entityName);
    		}
    	}
    	
    	if(objective != null) {
    		objective.setLabel(label);
    		objective.setRepetitions(repetitions);
    		objective.setStatusText(hudText);
    	}
    	
    	return objective;
    }
    
    private static GameEvent parseEvent(AttributeSet data) {
    	String[] stringData = null;
    		String type = null;
        	stringData = data.getStringAttribute("type");
        	if( (stringData != null) && (stringData.length > 0) ) type = stringData[0];
        	String[] parameters = data.getStringAttribute("parameters");
        	
        	return new GameEvent(type, parameters, null);
    }
    /* Populates static arrays with level data.*/
    public static void parseLevelList(String path) {

    	ArrayList<String> levels = null;
    	
		try {
			BracketedTextParser reader = new BracketedTextParser();
			reader.openSourceFile(path);
			
			StringBuffer block;
			AttributeSet data = null;
			String[] sa = null;
			String levelName = null, levelPath = null;
			
			try {
				do {
					block = reader.readBlock();
					if(block != null) {
						if(block.toString().trim().equalsIgnoreCase("level")) {
							data = BracketedTextParser.parseAttributeSet(reader.readBlock());
							if(data != null) {
								levelName = null; levelPath = null;
								sa = data.getStringAttribute("name");
								if( (sa != null) && (sa.length > 0) ) levelName = sa[0];
								sa = data.getStringAttribute("path");
								if( (sa != null) && (sa.length > 0) ) levelPath = sa[0];
								
								if( (levelName != null) && (levelPath != null) ) {
									if(levels == null) levels = new ArrayList<String>();
									levels.add(levelName); levels.add(levelPath);
								}
								
							}
						}
					}
				} while (block != null);
			} catch (IOException e) {
				System.err.println("Error parsing BFT block: " + e.toString());
			}
			
			reader.closeSourceFile();
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
		
		levelNames = new String[levels.size() / 2]; levelPaths = new String[levels.size() / 2];
		for(int i = 0; i < levelNames.length; i++) {
			levelNames[i] = levels.get(i * 2);
			levelPaths[i] = levels.get( (i * 2) + 1);
		}
	}
    
}
