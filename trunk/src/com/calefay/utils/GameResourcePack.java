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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

import com.jme.image.Texture;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;
import com.jme.util.resource.ResourceLocatorTool;

public class GameResourcePack implements Runnable {
	public static enum GameResourceType {SPATIAL, SCRIPT, TEXTURE,SOUND, CORE};
	private float MAXWAITTIME = 10000f;	// Maximum time to wait on a thread.
	
	private HashMap<String, Spatial> spatials = null;
	private HashMap<String, Texture> textures = null;
	private HashMap<String, TextureState> textureStates = null;
	private HashMap<String, RenderState> renderStates = null;
	private HashMap<String, GameEventScript> scripts = null;
	private HashMap<String, URL> sounds = null;
	private ArrayList<PendingLoad> pendingLoads = null;
	
	private boolean locked = false;
	private Thread loadingThread = null;
	
	public GameResourcePack() {
		pendingLoads = new ArrayList<PendingLoad>();
		locked = false;
	}
	
	public void run() {
		locked = true;
		
		while( (loadingThread == Thread.currentThread()) && (pendingLoads.size() > 0) ) {
			loadNextPending();
		}
		
		locked = false;
		loadingThread = null;
	}
	
	public void addPendingLoad(String label, String path, GameResourceType type) {
		if(locked) return;
		if( (label  == null) || (path == null) || (type == null) ) return;
		pendingLoads.add( new PendingLoad(label, path, type));
	}
	
	/* Creates a new thread which will load resources pending. Access to most methods will be locked until the thread completes.
	 * NOTE: Texture loading may not be threadsafe (due to TextureManager) - and they still need preloading anyway*/
	public void loadPendingThreaded() {
		if(loadingThread == null) {
			locked = true;
			loadingThread = new Thread(this);
			loadingThread.start();
		}
	}
	
	/* Loads all files in the pending queue. Does not create a new thread.*/
	public void loadPending() {
		while(pendingLoads.size() > 0) {
			loadNextPending();
		}
	}
	
	/** This method assumes that there is at least one pending in there - no check.*/
	private void loadNextPending() {
		PendingLoad toLoad = pendingLoads.remove(0);
		switch(toLoad.type) {
		case SPATIAL: loadSpatial(toLoad.label, toLoad.path); break;
		case TEXTURE: loadTexture(toLoad.label, toLoad.path); break;
		case SOUND: addSound(toLoad.label, toLoad.path); break;
		case SCRIPT: loadScript(toLoad.path); break;
		case CORE: loadCore(toLoad.label); break;
		}
	}
	
	public boolean isLocked() {return locked;}
	
	private void loadCore(String label) {

	}
	
	private void loadTexture(String label, String path) {		
		Texture tx = TextureManager.loadTexture(
				ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, path),
				Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear);
		addTexture(label, tx);
		// Note can be preloaded through a texturestate (but to make that need to be in OpenGL thread).
	}
	
	private void loadSpatial(String label, String path) {
		if((label == null) || (path == null)) return;
		Spatial n = null;
		String extension = getFileExtension(path).trim().toLowerCase();
		if(extension.equals(".jme")) n = ModelImporter.locateLoadJME(path);
		if(extension.equals(".ms3d")) n = ModelImporter.locateLoadMS3D(path);
		if(extension.equals(".3ds")) n = ModelImporter.locateLoad3DS(path);
		if(n != null) addSpatial(label, n);
	}
	
	private void addTexture(String label, Texture t) {		
		if((label != null) && (t != null) ) {
			if(textures == null) textures = new HashMap<String, Texture>();
			textures.put(label, t);
		}
	}
	
	/* Placeholder only - just stores the file path not the actual sound.*/
	private void addSound(String label, String soundPath) {		
		if((label != null) && (soundPath != null) ) {
			if(sounds == null) sounds = new HashMap<String, URL>();
			sounds.put(label, ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_AUDIO, soundPath));
		}
	}
	
	private void addScript(String label, GameEventScript script) {
		if((label != null) && (script != null) ) {
			if(scripts == null) scripts = new HashMap<String, GameEventScript>();
			script.sortScript();
			scripts.put(label, script);
		}
	}
	
	private void addSpatial(String label, Spatial s) {
		if((label != null) && (s != null) ) {
			if(spatials == null) spatials = new HashMap<String, Spatial>();
			spatials.put(label, s);
		}
	}
	
	public Spatial getSpatial(String label) {
		if(locked || (spatials == null)) return null;
		return spatials.get(label);
	}
	
	public Texture getTexture(String label) {
		if(locked || (textures == null)) return null;
		return textures.get(label);
	}
	
	public URL getSound(String label) {
		if(locked || (sounds == null)) return null;
		return sounds.get(label);
	}
	
	public GameEventScript getScript(String label) {
		if(locked || (scripts == null) || (label == null) ) return null;
		return scripts.get(label);
	}
	
	/* Creates a TextureState for each currently loaded texture. For convenience - the TextureStates can then be retrieved using the same label.
	 * Note this should only be called from the OpenGL thread, and will not do anything if the pack is currently locked.*/
	public void createTextureStates(Renderer renderer) {
		if(locked || (renderer == null) || (textures == null) ) return;
		if(textureStates == null) textureStates = new HashMap<String, TextureState>();
		for(String key : textures.keySet()) {
			Texture t = getTexture(key);
			if(t != null) {
				TextureState ts = renderer.createTextureState();
				ts.setEnabled(true);
				ts.setTexture(t);
				textureStates.put(key, ts);
			}
		}
	}
	
	public TextureState getTextureState(String label) {
		if(textureStates == null) return null;
		return textureStates.get(label);
	}
	
	public RenderState getRenderState(String label) {
		if(renderStates == null) return null;
		return renderStates.get(label);
	}
	
	/* Reads a pack of resources to load from a properly formatted text file. Does not actually load the resources yet, they are put in the pending queue.*/
	public void parseResourcePack(String scriptFile) {
		try {
			BracketedTextParser reader = new BracketedTextParser();
			reader.openSourceFile(scriptFile);
			
			StringBuffer block = null;
			do {
				block = reader.readBlock();
				if( (block != null) && (block.toString().trim().equalsIgnoreCase("resource")) ) {
					parseResource( BracketedTextParser.parseAttributeSet(reader.readBlock()));
					}
			} while(block != null);
			
			reader.closeSourceFile();
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
	}
	
	private void parseResource(AttributeSet data) {
		if (data == null) return;
		String[] attr = null; String s = null;
		GameResourceType type = null; String label = null; String path = null;
		
		if(data.hasAttribute("type")) {
			attr = data.getStringAttribute("type");
			if(attr.length == 1) {
				s = attr[0].trim().toLowerCase();
				if(s.equals("texture")) type = GameResourceType.TEXTURE;
				if(s.equals("spatial")) type = GameResourceType.SPATIAL;
				if(s.equals("script")) type = GameResourceType.SCRIPT;
				if(s.equals("sound")) type = GameResourceType.SOUND;
				if(s.equals("core")) type = GameResourceType.CORE;
			}
		}
		if(data.hasAttribute("label")) {
			attr = data.getStringAttribute("label");
			if(attr.length == 1) label = attr[0].trim();
		}
		if(data.hasAttribute("path")) {
			attr = data.getStringAttribute("path");
			if(attr.length == 1) path = attr[0];
		}
		if( (type != null) && (label != null) && (path != null) ) {
			addPendingLoad(label, path, type);
		}
	}
	
	/* Applies textures and alphaStates to meshes. Currently only one texture to the spatial.
	 * Since it is processed as it is parsed not stored, doesn't have to be an attribute set, could be structured data
	 * referencing individual node children by name for example.
	 * NOTE: Remember to callbuildPresetRenderStates before this if any alphaStates are needed
	 * 		 Also make sure the resources have actually been loaded with loadPending().*/
	public void parsePresets(String scriptFile) {
		try {
			BracketedTextParser reader = new BracketedTextParser();
			reader.openSourceFile(scriptFile);
			
			StringBuffer block = null;
			do {
				block = reader.readBlock();
				if( (block != null) && (block.toString().trim().equalsIgnoreCase("preset")) ) {
					parsePreset( BracketedTextParser.parseAttributeSet(reader.readBlock()));
					}
			} while(block != null);
			
			reader.closeSourceFile();
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
	}
	
	private void parsePreset(AttributeSet data) {
		if (data == null) return;
		String[] attr = null;
		Spatial mesh = null;
		TextureState ts = null;
		
		attr = data.getStringAttribute("mesh");
		if( (attr != null) && (attr.length == 1) ) mesh = getSpatial(attr[0].trim());
		if(mesh == null) return;
		
		attr = data.getStringAttribute("texture");
		if( (attr != null) && (attr.length == 1) ) {
			ts = getTextureState(attr[0].trim());
			if(ts != null) mesh.setRenderState(ts);
		}
		
		attr = data.getStringAttribute("alpha");
		if( (attr != null) && (attr.length == 1) ) {
			if(attr[0].trim().equalsIgnoreCase("blend")) {
				RenderState as = renderStates.get("alphablend");
				if(as != null) mesh.setRenderState(as);
				mesh.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT); // Not sure master mesh is the right place for this.
			}
			if(attr[0].trim().equalsIgnoreCase("kill")) {
				RenderState as = renderStates.get("alphakill");
				if(as != null) mesh.setRenderState(as);
			}
		}
		
		mesh.updateRenderState();
	}
	
	private void loadScript(String path) {
		if(path == null) {System.err.println("Error loading script - path was null."); return;}
		GameEventScript script = null;

		try {
			BracketedTextParser reader = new BracketedTextParser();
			try{reader.openSourceFile(path);} catch (FileNotFoundException e) {
				System.err.println("Error: Resourcepack could not load script - file not found. " + e);
				return;
			} 
			
			StringBuffer block = null;
			do {
				block = reader.readBlock();
				if( (block != null) && (block.toString().trim().equalsIgnoreCase("gamescript")) ) {
					script = parseScript( new BracketedTextParser(reader.readBlock()) );
					if( (script != null) && (tempScriptLabel != null) ) addScript(tempScriptLabel, script);
					}
			} while(block != null);
			
			reader.closeSourceFile();
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
	}
	
	private static String tempScriptLabel = null;
	
	private static GameEventScript parseScript(BracketedTextParser parser) {
		GameEventScript script = new GameEventScript();
		if(parser == null) {System.err.println("No input provided to parse script."); return script;}
		StringBuffer block;
		
		tempScriptLabel = null;
		
		try {
			do {
				block = parser.readBlock();
				if(block != null) {
					if(block.toString().trim().equalsIgnoreCase("header")) {
			    		AttributeSet data = BracketedTextParser.parseAttributeSet(parser.readBlock());
			    		String[] attr = (data == null) ? null : data.getStringAttribute("label");
			    		if( (attr != null) && (attr.length > 0) ) tempScriptLabel = attr[0];
			    	}
			    	if(block.toString().trim().equalsIgnoreCase("scriptevent")) {
			    		parseScriptEvent(BracketedTextParser.parseAttributeSet(parser.readBlock()), script);
			    	}
				}
			} while (block != null);
		} catch (IOException e) {
			System.err.println("Error parsing Game Event Script: " + e.toString());
		}
		if(tempScriptLabel == null) System.err.println("Error adding script resource: No label.");
		return script;
	}
	
    private static void parseScriptEvent(AttributeSet data, GameEventScript loadingScript) {
    	float[] attrTime = null;  float[] attrDuplicates = null; float[] attrDuplicateFreq = null;
    	String[] attrType = null, attrParameters = null, attrQueue = null;
    	int duplicates = 1; float duplicateFreq = 0;
    	
    	if(data.hasAttribute("time")) {attrTime = data.getFloatAttribute("time");}
    	if(data.hasAttribute("type")) {attrType = data.getStringAttribute("type");}
    	if(data.hasAttribute("queue")) {attrQueue = data.getStringAttribute("queue");}
    	if(data.hasAttribute("parameters")) {attrParameters = data.getStringAttribute("parameters");}
    	
    	if(data.hasAttribute("duplicate_frequency")) {
    		attrDuplicateFreq = data.getFloatAttribute("duplicate_frequency");
    		if( (attrDuplicateFreq != null) && (attrDuplicateFreq.length == 1) && (attrDuplicateFreq[0] >= 0) )
    			duplicateFreq = attrDuplicateFreq[0];
    	}
    	if(data.hasAttribute("duplicate_number")) {
    		attrDuplicates = data.getFloatAttribute("duplicate_number");
    		if( (attrDuplicates != null) && (attrDuplicates.length == 1) && (attrDuplicates[0] >= 1) )
    			duplicates = (int)attrDuplicates[0];
    	}
    	
    	
    	if( (attrTime != null) && (attrTime.length == 1) && (attrTime[0] >= 0) &&
    		(attrType != null) && (attrType.length == 1) &&
    		(attrQueue != null) && (attrQueue.length == 1)) {
    		for(int i = 0; i < duplicates; i++) {
    			GameScriptEvent event = 
    				new GameScriptEvent(attrTime[0] + (duplicateFreq * i), 
    						new GameEvent(attrType[0], attrParameters, null), attrQueue[0]);
        		loadingScript.addScriptEvent(event);
    		}
    	} else {
    		System.out.println("Failed to parse script event data: " + data);
    	}
    }
	
	public void buildPresetRenderStates(Renderer renderer) {
		if( locked || (renderer == null) ) return;
		
		if(renderStates == null) renderStates = new HashMap<String, RenderState>();
		
		BlendState as = renderer.createBlendState();
		as.setBlendEnabled( false );
		as.setTestEnabled( true );
		as.setTestFunction(BlendState.TestFunction.GreaterThan);
		as.setReference( 0.5f );
		as.setEnabled( true );
		renderStates.put("alphakill", as);
		
		as = renderer.createBlendState();
		as.setBlendEnabled(true);
		as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
		as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
		as.setTestEnabled(true);
		as.setTestFunction(BlendState.TestFunction.GreaterThan);
		renderStates.put("alphablend", as);
	}
	
    private String getFileExtension(String resourceName) {
        File f = new File(resourceName);
        String name = f.getPath();
        int dot = name.lastIndexOf('.');
        if (dot < 0) {
            return null;
        } else {
            return resourceName.substring(dot, resourceName.length());
        }
    }
	
	public void cleanup() {
		loadingThread = null;
		float waitTime = MAXWAITTIME;
		while(locked && (waitTime > 0) ) {
			waitTime -= 100f;								// Wait for the thread to stop running, within reason.
			try{Thread.sleep(100);} catch(Exception e) {}
		}
		
		if(textures != null) textures.clear(); textures = null;
		if(spatials != null) spatials.clear(); spatials = null;
		if(sounds != null) sounds.clear(); sounds = null;
		if(scripts != null) {
			for(GameEventScript s : scripts.values()) {s.cleanup();}
			scripts.clear();
			scripts = null;
		}
		if(renderStates != null) renderStates.clear();
		if(textureStates != null) {
			for( TextureState ts : textureStates.values() ) {ts.deleteAll(true);}
			textureStates.clear();
			textureStates = null;
		}
			
	}
	
	private class PendingLoad {
		public String label = null;
		public String path = null;
		public GameResourceType type = null;
		
		public PendingLoad(String label, String path, GameResourceType type) {
			this.label = label; this.path = path; this.type = type;
		}
	}
}
