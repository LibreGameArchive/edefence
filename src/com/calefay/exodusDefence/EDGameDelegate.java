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

import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.calefay.exodusDefence.EDFader.FadeMode;
import com.calefay.exodusDefence.EDGenericInterface.EDMenuState;
import com.calefay.exodusDefence.EDPreferences.SettingLMH;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameEventHandler;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.Node;
import com.jme.scene.state.CullState;
import com.jme.scene.state.ZBufferState;
import com.jme.scene.state.CullState.Face;
import com.jme.scene.state.RenderState.StateType;
import com.jme.system.DisplaySystem;
import com.jme.util.Timer;
import com.jme.util.resource.MultiFormatResourceLocator;
import com.jme.util.resource.ResourceLocatorTool;

public class EDGameDelegate {
	
	public final float MAXVIEWDISTANCE = 3000f;	// 1000f	// TODO: Make this a level parameter? Would require resetting the camera.
	
	private DisplaySystem display = null;
	
	private EDDisplaySystem visualManager = null;
	private EDGameModel model = null;
	private GameWorldInfo gameInfo = null;
	private EDGenericInterface genericInterface = null;
	private EDScriptController scriptController = null;
	private EDAudioEventProcessor audioProcessor = null;
	private EDControllerHandler entityController = null;
	private EDParticleManager particleManager = null;
	protected EDPreferences menuSettings = null;
	
	private GameResourcePack gameResources = null;
	private GameResourcePack levelResources = null;
	
	private Node menuRoot = null;
	private Node rootNode = null;
	private Node skyBoxRoot = null; 		// Skybox and gameWorldRoot are attached to this.
	
	private Camera cam = null;
	private Timer timer = null;
	
	public EDGameDelegate(boolean isWebView, int width, int height, int bpp, int refreshRate) {
		display = DisplaySystem.getDisplaySystem();
		menuSettings = new EDPreferences(isWebView, width, height, bpp, refreshRate, false, SettingLMH.LOW, SettingLMH.LOW);
	}
	
	protected void initGame(GameEventHandler eventHandler) {
		Logger.getLogger("com.jme").setLevel(Level.OFF);
		
	    try {
	        MultiFormatResourceLocator texture_locator = new MultiFormatResourceLocator(ResourceLocatorTool.class.getResource("/data/textures/"), ".jpg", ".png", ".tga");
	        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, texture_locator);
	        texture_locator.setTrySpecifiedFormatFirst(true);
	        MultiFormatResourceLocator core_locator = new MultiFormatResourceLocator(ResourceLocatorTool.class.getResource("/data/"), ".edj");
	        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, core_locator);
	        core_locator.setTrySpecifiedFormatFirst(true);
	    } catch (URISyntaxException e) {
	        e.printStackTrace();
	    }
	    
	    try {
	        MultiFormatResourceLocator model_locator = new MultiFormatResourceLocator(ResourceLocatorTool.class.getResource("/data/models/"), ".jme",".ms3d", ".3ds");
	        model_locator.setTrySpecifiedFormatFirst(true);
	        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_MODEL, model_locator);
	    } catch (URISyntaxException e) {
	        e.printStackTrace();
	    }
	    
	    try {
	        MultiFormatResourceLocator particle_locator = new MultiFormatResourceLocator(ResourceLocatorTool.class.getResource("/data/effects/"), ".jme");
	        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_PARTICLE, particle_locator);
	    } catch (URISyntaxException e) {
	        e.printStackTrace();
	    }
	    
	    try {
	        MultiFormatResourceLocator sound_locator = new MultiFormatResourceLocator(ResourceLocatorTool.class.getResource("/data/sounds/"), ".ogg", ".wav");
	        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_AUDIO, sound_locator);
	        sound_locator.setTrySpecifiedFormatFirst(true);
	        MultiFormatResourceLocator music_locator = new MultiFormatResourceLocator(ResourceLocatorTool.class.getResource("/data/music/"), ".ogg", ".wav");
	        ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_AUDIO, music_locator);
	        music_locator.setTrySpecifiedFormatFirst(true);
	    } catch (URISyntaxException e) {
	        e.printStackTrace();
	    }
	    
	    initialize(eventHandler);
	}

	private void initialize(GameEventHandler eventHandler) {
		rootNode = new Node("System root node");
	    menuRoot = new Node("Menu root node");
		skyBoxRoot = new Node("skyBox root Node");
	    
		visualManager = new EDDisplaySystem();
	    visualManager.displayLoading(true, rootNode);
	    
	    gameResources = new GameResourcePack();
		gameResources.parseResourcePack("data/scripts/gameresources.txt");
		gameResources.loadPending();
		gameResources.createTextureStates(display.getRenderer());
		
	    gameInfo = new GameWorldInfo( null, null);
	    gameInfo.setEventHandler(eventHandler);
	    
		entityController = new EDControllerHandler();
	    model = new EDGameModel(entityController, gameResources);
	    gameInfo.setRootNode(model.getGameWorldRoot());
	    rootNode.attachChild(model.getGameWorldRoot());
	    EDLevel.parseLevelList("data/scripts/levels.txt");	// TODO: Logically this could be considered part of the UI
		genericInterface = new EDGenericInterface(model, entityController, audioProcessor, "data/scripts/hudresources.txt", menuSettings, menuRoot, true);
		scriptController = new EDScriptController(model, genericInterface, eventHandler);
		audioProcessor = null;
		model.setGenericInterface(genericInterface); // FIXME: REALLY don't like this mutual referencing.
		particleManager = new EDParticleManager(GameWorldInfo.getGameWorldInfo().getEventHandler());
		particleManager.setCamera(cam);
		gameInfo.setParticleManager(particleManager);
		
        ZBufferState zbuf = display.getRenderer().createZBufferState();
        zbuf.setEnabled(true);
        zbuf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        model.getGameWorldRoot().setRenderState(zbuf);
        
		CullState cs = display.getRenderer().createCullState();
		cs.setCullFace(Face.Back);
		model.getGameWorldRoot().setRenderState(cs);
		
		rootNode.updateRenderState();
		
		//audioProcessor.fadeInMusic(3.0f, 0.5f);
		eventHandler.addEvent("FadeInMusic", "audio", (Float)3.0f, (Float)0.5f);
		visualManager.displayLoading(false, rootNode); visualManager.fadeOut(0.5f);
		visualManager.startFade(FadeMode.FADING_IN, menuRoot, 0.5f);
	}
	
	protected void initSystem() {
		cam = display.getRenderer().createCamera(menuSettings.getWidth(), menuSettings.getHeight());
		cam.setFrustumPerspective(45.0f, (float)menuSettings.getWidth() / (float)menuSettings.getHeight(), 1, MAXVIEWDISTANCE);
		Vector3f loc = new Vector3f(0.0f, 0.0f, 25.0f);
		Vector3f left = new Vector3f(-1.0f, 0.0f, 0.0f);
		Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
		Vector3f dir = new Vector3f(0.0f, 0f, -1.0f);
		cam.setFrame(loc, left, up, dir);
		cam.update();

		display.getRenderer().setCamera(cam);
		
		timer = Timer.getTimer();
	}

	protected void reinit() {	// Check applet does some camera perspective change. Also you can get the camera from the renderer.
		cam.setFrustumPerspective(45.0f, (float)menuSettings.getWidth() / (float)menuSettings.getHeight(), 1, MAXVIEWDISTANCE);
		cam.update();
		
		genericInterface.menu.realignComponents();
		
		visualManager.fader.setSize((float)menuSettings.getWidth(), (float)menuSettings.getHeight());
	}
	
	protected void render(float interpolation) {
		display.getRenderer().clearBuffers();
		switch(genericInterface.menuState) {
		  case GAMEPLAY: renderGameplay(interpolation); break;
		  case MAINMENU: renderMainMenu(interpolation); break;
		  case PAUSEMENU: renderGameplay(interpolation); break;
		}
	}

	protected void renderGameplay(float interpolation) {
		display.getRenderer().draw(skyBoxRoot);
		visualManager.renderGameplay(interpolation, rootNode);
		display.getRenderer().draw(menuRoot);
	}
	
	protected void renderMainMenu(float interpolation) {
		display.getRenderer().draw(menuRoot);
	}

	protected void update(float interpolation) {		
		timer.update();
		interpolation = timer.getTimePerFrame();
		
		if(interpolation > 0.2f) interpolation = 0.2f;
		
		visualManager.update(interpolation);
		
		if(audioProcessor != null) audioProcessor.update(interpolation);
		genericInterface.update(interpolation);
		if(genericInterface.menuState == EDMenuState.GAMEPLAY) {
			model.updateGameplay(interpolation);
			scriptController.update(interpolation);
			particleManager.update(interpolation);
			entityController.update(interpolation);
			
			visualManager.updateGameplay(interpolation, cam);
		}
		
	}
	
	/*************************************** None system, flow control methods below *************************************/
	
	protected void handleSystemEvent(GameEvent e) {
		if(e.getEventType().equalsIgnoreCase("newgame")) {
			if( e.getEventInitiator() instanceof String) {
				initializeLevel( (String)e.getEventInitiator());}
			else {
				System.err.println("Error: Could not initialize level - invalid path.");}
		} else if(e.getEventType().equalsIgnoreCase("restartlevel")) {
			cleanupLevel();
			if( e.getEventInitiator() instanceof String) {
				initializeLevel( (String)e.getEventInitiator());}
			else {
				System.err.println("Error: Could not initialize level - invalid path.");}
		} else if(e.getEventType().equalsIgnoreCase("endgame")) {
			visualManager.fadeOut(0.5f);
			cleanupLevel();
			//audioProcessor.fadeInMusic(3.0f, 1.0f);
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("FadeInMusic", "audio", (Float)3.0f, (Float)1.0f);
			visualManager.fader.startFade(FadeMode.FADING_IN, menuRoot, 1.0f);
			genericInterface.menuState = EDMenuState.MAINMENU;
		} else if(e.getEventType().equalsIgnoreCase("SoundSettingsApply")) {
			if( ((audioProcessor == null) && (menuSettings.getSoundEnabled()) ) 
				|| ((audioProcessor != null) && (audioProcessor.isEnabled() != menuSettings.getSoundEnabled()))) {
				if(menuSettings.getSoundEnabled()) {
					audioProcessor = new EDAudioEventProcessor(1000, cam);
					if(genericInterface.menuState == EDMenuState.MAINMENU) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("FadeInMusic", "audio", (Float)3.0f, (Float)1.0f);
				} else {
					if(audioProcessor != null) audioProcessor.cleanup(); audioProcessor = null;
				}	
			}
		}
	}
	
	private void initializeLevel(String levelPath) {
		EDLevel currentLevel = EDLevel.loadLevel(levelPath);
		if(currentLevel == null) {System.err.println("Error loading level: " + levelPath); return;}
		
		//audioProcessor.fadeOutMusic(3.0f);
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("FadeOutMusic", "audio", (Float)3.0f, null);
		visualManager.fadeOut(0.5f);
		visualManager.displayLoading(true, rootNode);
		
		levelResources = new GameResourcePack();
		levelResources.parseResourcePack(currentLevel.getResourceFilePath());
		levelResources.loadPending();
		levelResources.createTextureStates(display.getRenderer());
		levelResources.buildPresetRenderStates(display.getRenderer());
		levelResources.parsePresets(currentLevel.getResourceFilePath());
		
		model.initializeLevel(currentLevel, levelResources);
		genericInterface.initializeLevel(currentLevel, cam, model.getTerrain());
		scriptController.initializeLevel(currentLevel, levelResources);
		particleManager.addWeatherEffect(currentLevel.getWeatherType());
		
		if(currentLevel.initialCameraPos != null) {cam.setLocation(currentLevel.initialCameraPos); cam.update();}
		
		model.finalizeLevelSetup();
		
		genericInterface.menuState = EDMenuState.GAMEPLAY;
		
		visualManager.initializeLevel(currentLevel, model, levelResources, skyBoxRoot, cam);
		visualManager.fadeOut(0.5f); visualManager.displayLoading(false, rootNode); 
		visualManager.fader.startFade(FadeMode.FADING_IN, menuRoot, 3.0f);
		
		skyBoxRoot.updateGeometricState(0, true);
		rootNode.updateGeometricState(0, true);
	}
	
	/* Used to recover from an exception. Does not attempt to diagnose - just blows everything away and recreates, one step from restarting the app.*/
	protected void fullRecover(GameEventHandler eventHandler) {
		cleanup();
		
		display = DisplaySystem.getDisplaySystem();
		menuSettings = new EDPreferences(false, 800, 600, 24, 60, false, SettingLMH.LOW, SettingLMH.LOW);
		
		initSystem();
		initialize(eventHandler);
		
		genericInterface.menu.showErrorWarning();
	}
	/******************************** Cleanup methods below ******************************/
	private void cleanupLevel() {
		model.cleanupLevel();
		genericInterface.cleanupLevel();
		scriptController.cleanupLevel();
		entityController.cleanupLevel();
		visualManager.cleanupLevel();
		
		GameWorldInfo.getGameWorldInfo().getEventHandler().flush();
		if(audioProcessor != null) audioProcessor.flush();
		
		skyBoxRoot.updateGeometricState(0, true);
		rootNode.clearRenderState(StateType.Light);
	    rootNode.clearRenderState(StateType.Fog);
		
		if(particleManager != null) particleManager.flush();
		
		if(levelResources != null) levelResources.cleanup(); levelResources = null;
		
		System.gc();
	}
	
	protected void cleanup() {
		if(model != null) model.cleanup(); model = null;
		if(genericInterface != null) genericInterface.cleanup(); genericInterface = null;
		if(scriptController != null) scriptController.cleanup(); scriptController = null;
		if(entityController != null) entityController.cleanup(); entityController = null;
		if(audioProcessor != null) audioProcessor.cleanup(); audioProcessor = null;
		if(particleManager != null) particleManager.cleanup(); particleManager = null;
		if(visualManager != null) visualManager.cleanup(); visualManager = null;
		
		if(levelResources != null) levelResources.cleanup(); levelResources = null;
		if(gameResources != null) gameResources.cleanup(); gameResources = null;
		
		cam = null;
		menuSettings = null;
		display = null;
		
		GameWorldInfo.setDefault(null); gameInfo = null;
	}
	
}
