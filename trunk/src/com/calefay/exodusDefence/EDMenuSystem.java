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
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.calefay.simpleUI.UICheckBox;
import com.calefay.simpleUI.UIConfirmBox;
import com.calefay.simpleUI.UIContainer;
import com.calefay.simpleUI.UIContainerBackground;
import com.calefay.simpleUI.UIMenuButton;
import com.calefay.simpleUI.UISelector;
import com.calefay.simpleUI.UITextContainer;
import com.calefay.simpleUI.UITextureContainer;
import com.calefay.utils.AttributeSet;
import com.calefay.utils.BracketedTextParser;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.TextCanvas2D;
import com.calefay.utils.GameEventHandler.GameEventListener;
import com.jme.image.Texture;
import com.jme.input.MouseInput;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial.LightCombineMode;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

public class EDMenuSystem {
	// TODO: Make it possible to have the quad size change without scaling the texture by changing the uv coords (as being power of 2 they have a lot of blank space in the textures).
	// TODO:Quite a lot of code in here is ugly and tightly coupled to a particular UI file. Improve it.
	public enum UIMode {MAINMENU, PAUSEMENU, GAMEPLAYHUD, GAMEPLAYNOHUD};
	
	private static DisplaySystem display = DisplaySystem.getDisplaySystem();
	private static final ColorRGBA BACKGROUND_BLUE_50 = new ColorRGBA(0.0f, 0.0f, 0.8f, 0.5f);
	private static final ColorRGBA BACKGROUND_DKBLUE_80 = new ColorRGBA(0.0f, 0.0f, 0.3f, 0.8f);
	public static enum HUDAttachPoint {TOPLEFT, TOPRIGHT, BOTTOMLEFT, BOTTOMRIGHT, CENTRELEFT, CENTRERIGHT};
	
	private final int CREDITSYSIZE = 512, CREDITSNUMBER = 4;
	
	private static final float AUTORESTARTTIME = 30.0f;
	
	private Node rootNode = null;
	
	private UIContainer inGameUI = null, topLeftHUD = null, topRightHUD = null,
							bottomLeftHUD = null, bottomRightHUD,
							centreLeftHUD, centreRightHUD = null;
	
	private UIContainer baseContainer;
	private UIContainer screenCentreContainer;
	private UIContainer mainMenuContainer, bannerContainer, gameMenu, pauseMenu; 
	private UIContainer creditsMenu, settingsMenu, extrasContainer = null;
	private UIContainer currentMenu = null, currentPopup = null, mainMenu = null;
	
	private HashMap<String, UIContainer> uiElements = null;	// TODO: Potentially remove this and just search for children of the root node.
	
	private ArrayList<UIContainer> extras = null;
	private EDPreferences currentSettings = null;
	private MouseInput mouse = null;
	private boolean leftButtonDown = false;
	
	private int currentExtra = 0;
	private float extrasTimer = 0;
	private float restartTimer = 0;
	private static final float EXTRASDISPLAYTIME = 8.0f;
	private Vector3f creditsOffset = null;
	
	private GameEventListener uiInternalListener = null;
	
	private GameResourcePack uiResources = null;
	private GameResourcePack extrasResources = null;
	
	private HashMap<String, Integer> highestValidBitDepths = null;
	private HashMap<String, Integer> lowestValidRefreshRates = null;
	
	public EDMenuSystem(Node root, EDPreferences initialSettings) {
		rootNode = root;
		initUI(initialSettings);
		refreshGameSettings(); refreshGraphicsSettings();
	}
	
	public void initUI(EDPreferences userSettings) {
		// TODO: Consider not redrawing the whole thing every frame.
        uiResources = new GameResourcePack();
        extrasResources = new GameResourcePack();
		currentSettings = userSettings;
		
		uiElements = new HashMap<String, UIContainer>();
		
		uiResources.parseResourcePack("data/scripts/uiresources.txt");
		extrasResources.parseResourcePack("data/scripts/uiextrasresources.txt");
		uiResources.loadPending(); uiResources.createTextureStates(DisplaySystem.getDisplaySystem().getRenderer());
		extrasResources.loadPending(); extrasResources.createTextureStates(DisplaySystem.getDisplaySystem().getRenderer());
		
		uiInternalListener = GameWorldInfo.getGameWorldInfo().getEventHandler().addListener("uiInternal", 500);
        
        mouse = MouseInput.get();
        mouse.setCursorVisible(true);
        leftButtonDown = false;
        
		Node uiBaseNode = new Node("UIBaseNode");
		uiBaseNode.setRenderQueueMode(Renderer.QUEUE_ORTHO);
		uiBaseNode.setLocalTranslation( 0, 0, 0);
		
		BlendState as = display.getRenderer().createBlendState();
		as.setBlendEnabled(true);
		as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
		as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
		as.setTestEnabled(true);
		as.setTestFunction(BlendState.TestFunction.GreaterThan);

		parseMenuFile("data/scripts/menusystem.txt");	// TODO: Get a root container or container list returned and attach it to the miniscreencontainer instead of defining the miniscreen in the file.
		
		baseContainer = new UIContainer("baseContainer", 0, 0, 100, 640, 480);
		screenCentreContainer = new UIContainer("screenCentreContainer", 0, 0, 100, 0, 0);	 // 0 size so only use as attach point for absolute positioning.
		baseContainer.attachChild(screenCentreContainer);
		screenCentreContainer.setPosition(0.5f, 0.5f);
		
		UIContainer miniScreenContainer = uiElements.get("miniScreenContainer");
		screenCentreContainer.attachChild(miniScreenContainer);
		mainMenuContainer = new UIContainer("mainMenuContainer", 0, 0, 90, 640, 480);
		miniScreenContainer.attachChild(mainMenuContainer);
		
		bannerContainer = new UIContainer("bannerContainer", 0, 0, 100, 640, 128); mainMenuContainer.attachChild(bannerContainer);
		new UITextureContainer("backdropleft", uiResources.getTextureState("backdropLeft"), 64, 128, 0, 480 - 128, 90, bannerContainer);
		new UITextureContainer("backdropmid", uiResources.getTextureState("backdropMid"), 512, 128, 64, 480 - 128, 90, bannerContainer);
		new UITextureContainer("backdropright", uiResources.getTextureState("backdropRight"), 64, 128, 512 + 64, 480 - 128, 90, bannerContainer);
		bannerContainer.setActive(!userSettings.showWebView());
		
		UITextContainer version = new UITextContainer("version", "Version: 1.1", 128, 16, 10, 16, 10, mainMenuContainer);
		version.getDisplayNode().setLocalScale(1.0f);
		
		extrasContainer = uiElements.get("extrasContainer");
		addExtras();
		
		gameMenu = uiElements.get("gameMenuContainer");
		currentMenu = gameMenu;
		mainMenu = gameMenu;
		
		creditsMenu = uiElements.get("creditsMenuContainer");
		buildCredits();
		
		pauseMenu = uiElements.get("pauseMenuContainer");
		
		settingsMenu = uiElements.get("settingsMenuContainer");
		
		inGameUI = new UIContainer("inGameUIContainer", 0, 0, 100, 640, 480);
		baseContainer.attachChild(inGameUI);
		inGameUI.resize(1.0f, 1.0f);	// Float (proportional) sizing so that it will automatically resize with the base container.
		inGameUI.setActive(false);
		
		bottomLeftHUD = new UIContainer("bottomLeftHUD", 0, 0, 95, 320, 128);
		inGameUI.attachChild(bottomLeftHUD);
		bottomLeftHUD.setPosition(0, 0);
		bottomRightHUD = new UIContainer("bottomRightHUD", 0, 0, 95, 320, 128);
		inGameUI.attachChild(bottomRightHUD);
		bottomRightHUD.setPosition(1.0f, 0);
		
		topLeftHUD = new UIContainer("topLeftHUD", 0, 0, 95, 256, 32);
		inGameUI.attachChild(topLeftHUD);
		topLeftHUD.setPosition(0.0f, 1.0f);
		topRightHUD = new UIContainer("topRightHUD", 0, 0, 95, 320, 128);
		inGameUI.attachChild(topRightHUD);
		topRightHUD.setPosition(1.0f, 1.0f);
		
		centreLeftHUD = new UIContainer("centreLeftHUD", 00, 0, 95, 640, 320);
		centreLeftHUD.setPosition(0, 0.5f);
		inGameUI.attachChild(centreLeftHUD);
		centreRightHUD = new UIContainer("centreRightHUD", 0, 0, 95, 640, 320);
		inGameUI.attachChild(centreRightHUD);
		centreRightHUD.setPosition(1.0f, 0.5f);

		uiBaseNode.setLightCombineMode(LightCombineMode.Off);
		uiBaseNode.attachChild( baseContainer.getDisplayNode() );
        rootNode.attachChild(uiBaseNode);
        realignComponents();
        
        activateWindowBackgrounds(false);
	}
	
	public void realignComponents() {
		baseContainer.resize(currentSettings.getWidth(), currentSettings.getHeight());
		
		bannerContainer.setActive(!currentSettings.showWebView() || currentSettings.getFullscreen());
		settingsMenu.getChild("resolutiontext").setActive(!currentSettings.showWebView());
		settingsMenu.getChild("resolutionselector").setActive(!currentSettings.showWebView());
	}
	
	public void activateMenu(UIContainer menu) {
		currentMenu.setActive(false);
		menu.setActive(true);
		currentMenu = menu;
		currentPopup = null;
		extrasContainer.setActive((currentMenu == mainMenu) && (currentMenu != pauseMenu));
	}
	
	public void popupMenu(UIContainer popup) {
		currentMenu.setSuspended(true);
		popup.setActive(true);
		currentPopup = popup;
	}

	public void updateUI(float tpf) {
		int mouseX = mouse.getXAbsolute();
		int mouseY = mouse.getYAbsolute();
		boolean leftClick = false;
		
		if(extrasContainer.isActive()) {updateExtras(tpf);}
		
		leftClick = false;
		if(!mouse.isButtonDown(0)) {
			leftButtonDown = false;
		} else if (!leftButtonDown) {
			leftButtonDown = true;
			leftClick = true;
		}
		
		baseContainer.checkMouseActions(mouseX, mouseY, leftClick);
		processEvents();
		
		if(restartTimer > 0) {	// Has to come after processEvents in case yes was clicked in the same update.
			restartTimer -= tpf;
			if(restartTimer <= 0) {
				doLevelRestart();
			}
		}
	}
	
	public void processEvents() {
		GameEvent gameEvent = uiInternalListener.getNextEvent();
		while(gameEvent != null) {
			UIContainer initiator = (UIContainer)gameEvent.getEventInitiator();
			String eventType = gameEvent.getEventType();
			String[] params = ((gameEvent.getEventTarget() != null) && (gameEvent.getEventTarget() instanceof String[])) ? (String[])gameEvent.getEventTarget() : null;
			
			if(eventType.equals("uiClick")) {
				handleClick(initiator.getName());
			} else if(eventType.equals("activateMenu")) {	
				if( (params != null) && (params.length > 0)) {activateMenu(uiElements.get(params[0]));}
			}  else if(eventType.equals("mainMenu")) {	
				activateMenu(mainMenu);
			} else if(eventType.equals("popup")) {	
				if( (params != null) && (params.length > 0)) {popupMenu(uiElements.get(params[0]));}
			} else if( eventType.equals("startLevel") || eventType.equals("startTutorial")) {
				int level = 1;
				if( (params != null) && (params.length > 0)) level = Integer.parseInt(params[0]);
				newGame(level, eventType.equals("startTutorial"));
			} else if(eventType.equals("quitToDesktop")) {
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UIExitGame", "uievents", null, null);
			} else if(eventType.equals("restartLevel")) {
				doLevelRestart();
			} else if(eventType.equals("endGame")) {
				endGame();
			} else if(eventType.equals("applySettings")) {
				refreshGraphicsSettings();
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UIGraphicsApply", "uievents", null, null);
				
				refreshGameSettings();
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UIGameSettingsApply", "uievents", null, null);
				
				getSoundSettings();
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UISoundSettingsApply", "uievents", null, null);
				
				activateMenu(mainMenu);
			}
			
			if(eventType.equals("menuBack")) {}
			if(eventType.equals("popupClose")) {activateMenu(currentMenu);}

			if(eventType.equals("LevelFailedRestartCheck")) {popupMenu(uiElements.get("gameOverConfirm")); restartTimer = AUTORESTARTTIME;}
			
			gameEvent = uiInternalListener.getNextEvent();
		}
	}
	
	public void handleClick(String buttonName) {
		if(buttonName.equals("credits")) {activateMenu(creditsMenu); creditsOffset.y = 0.75f;}
		else if(buttonName.equals("controls")) {activateMenu(uiElements.get("controlsMenuContainer")); creditsOffset.y = 0f;}
		else if(buttonName.equals("resume")) {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UIResume", "uievents", null, null);
			resumeGameplay();
		}
	}
	
	private void newGame(Integer i, boolean isTutorial) {
		if(isTutorial) {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UINewTutorial", "uievents", i - 1, null);
		} else {
			currentSettings.setLevelSelection(i - 1);	// No longer used?
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UINewGame", "uievents", i - 1, null);
		}
		mouse.setCursorVisible(false);
		activateWindowBackgrounds(true);
		
		activateMenu(inGameUI);
		mainMenu = pauseMenu;
		extrasContainer.setActive(false);
		mainMenuContainer.setActive(false);
	}
	
	/** To be called by the controlling system when the menu needs to be displayed. 
	 *  Most of the work will already be done in the controlling program by simply rendering the right node and calling the UI's update method.
	 */ 
	public void activateMenuSystem() {
		mouse.setCursorVisible(true);
	}
	
	/** To be called when leaving the pause menu to resume gameplay - be that by clicking resume or pressing the pause key again.*/
	public void resumeGameplay() {
		if(currentPopup != null) {currentPopup.setActive(false);}
		mouse.setCursorVisible(false);
		activateMenu(inGameUI);
	}
	
	public void doLevelRestart() {
		restartTimer = 0;
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UIRestartLevel", "uievents", null, null);
		resumeGameplay();
	}
	
	private void endGame() {
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("UIEndGame", "uievents", null, null);
		activateMenu(gameMenu);
		inGameUI.setActive(false);
		extrasContainer.setActive(true);
		mainMenuContainer.setActive(true);
		mainMenu = gameMenu;
		activateWindowBackgrounds(false);
	}
	
	public void addExtras() {
		extrasTimer = 0;
		currentExtra = 0;
		
		extras = new ArrayList<UIContainer>();
		UITextureContainer extra = new UITextureContainer("extra1", extrasResources.getTextureState("extra1"), 256, 256, 32, 0, 90, extrasContainer);
		extras.add(extra);
		extra = new UITextureContainer("extra2", extrasResources.getTextureState("extra2"), 256, 256, 32, 0, 90, extrasContainer);
		extras.add(extra);
		extra.setActive(false);
		extra = new UITextureContainer("extra3", extrasResources.getTextureState("extra3"), 128, 256, 96, 0, 90, extrasContainer);
		extras.add(extra);
		extra.setActive(false);
	}
	
	private void activateWindowBackgrounds(boolean activate) {
		UIContainer settingsMnuBkg = uiElements.get("settingsMenuContainerContainerBackground");	// FIXME: Hardcoded name for in game container background toggle.
		settingsMnuBkg.setActive(activate);
	}
	
	public void attachToHUD(UIContainer container, HUDAttachPoint attach) {
		// TODO: Possibly remove this altogether and replace with the possibility of using percentage + offset positioning on containers.
		if(container == null) return;
		switch(attach) {
			case TOPLEFT: topLeftHUD.attachChild(container); break;
			case TOPRIGHT: topRightHUD.attachChild(container); break;
			case BOTTOMLEFT: bottomLeftHUD.attachChild(container); break;
			case BOTTOMRIGHT: bottomRightHUD.attachChild(container); break;
			case CENTRELEFT: centreLeftHUD.attachChild(container); break;
			case CENTRERIGHT: centreRightHUD.attachChild(container); break;
		}
	}

	/* Allows exertnal classes to add backgrounds using the menu system's look&feel.*/
	public UIContainerBackground addBackground(UIContainer c) { 	// TODO: Remove this and allow externally defined HUD elements.
		return new UIContainerBackground(uiResources.getTextureState("windowcorner"), BACKGROUND_BLUE_50, c);
	}
	
	public void updateExtras(float tpf) {
		extrasTimer += tpf;
		
		if(extrasTimer >= EXTRASDISPLAYTIME) {
			extrasTimer = 0;
			extras.get(currentExtra).setActive(false);
			currentExtra += 1;
			if(currentExtra >= extras.size()) currentExtra = 0;
			extras.get(currentExtra).setActive(true);
		}
		
		// TODO: THE BELOW IS COMPLETELY HACKED RUBBISH FOR FADING!!
		float fadeTime = EXTRASDISPLAYTIME - extrasTimer;
		if(fadeTime >= (EXTRASDISPLAYTIME - 1.0f)) fadeTime = EXTRASDISPLAYTIME - fadeTime;
		if(fadeTime <= 1.0f) {
			UITextureContainer c = (UITextureContainer)extras.get(currentExtra); 	// TODO: DON'T DO THIS!!!!!
			c.setTransparency(fadeTime);
		}
		
		// TODO: THE BELOW IS COMPLETELY HACKED RUBBISH FOR CREDITS TRANSITIONS!
		if( (extrasTimer == 0) && (currentMenu == creditsMenu)) {
			creditsOffset.y -= (1 / (float)CREDITSNUMBER);
			if(creditsOffset.y < 0.25f) creditsOffset.y = 0.75f;
		}
	}
	
	public void showErrorWarning() {
		popupMenu(uiElements.get("errorConfirm"));
	}
	
	public void refreshGraphicsSettings() {
		// TODO: Check for nulls getting returned as we are searching by name.
		UISelector resolutionSelector = (UISelector)settingsMenu.getChild("resolutionselector");
		EDScreenMode mode = (EDScreenMode)resolutionSelector.getCurrentChoiceObject();
		currentSettings.setResolution(mode.getWidth(), mode.getHeight());
				
		UICheckBox fullScreenCheck = (UICheckBox)settingsMenu.getChild("fullscreen");
		currentSettings.setFullscreen(fullScreenCheck.getStatus());
		UICheckBox nbkgCheck = (UICheckBox)settingsMenu.getChild("narrativebackgrounds");
		currentSettings.narrativeBackgrounds = nbkgCheck.getStatus();
		if(currentSettings.getFullscreen()) currentSettings.setBitsPerPixel(32); else currentSettings.setBitsPerPixel(24);
		String resolutionLabel = resolutionSelector.getCurrentChoiceText();
		if(resolutionLabel != null) {
			Integer bpp = highestValidBitDepths.get(resolutionLabel);
			if(bpp != null) {currentSettings.setBitsPerPixel(bpp);}
			
			Integer hz = lowestValidRefreshRates.get(resolutionLabel);
			if(hz != null) {currentSettings.setRefreshRate(hz);} else currentSettings.setRefreshRate(60);
		}

	}
	
	public void refreshGameSettings() {
		UICheckBox invertMouseCheck = (UICheckBox)settingsMenu.getChild("inverty");
		currentSettings.setInvertY(invertMouseCheck.getStatus());
		UICheckBox loggingCheck = (UICheckBox)settingsMenu.getChild("enablelogging");
		currentSettings.setLoggingEnabled(loggingCheck.getStatus());
	}

	public void getSoundSettings() {
		UICheckBox enableSoundCheck = (UICheckBox)settingsMenu.getChild("enablesound");
		currentSettings.setSoundEnabled(enableSoundCheck.getStatus());
	}
	
	public EDPreferences getSettings() {return currentSettings;}
	
	public void setMode(UIMode u) {	// TODO: Can't remember what this is for. Couldn't it be done in activateMenuSystem/resumeGameplay?
		switch (u) {
		case PAUSEMENU: 
			activateMenu(pauseMenu); 
			inGameUI.setActive(true); 
			break;
		case GAMEPLAYHUD: break;
		case GAMEPLAYNOHUD: break;
		}
	}
	
	private  void buildCredits() {
		TextCanvas2D tc = new TextCanvas2D(256, CREDITSYSIZE);	// FIXME: Sort out sizing
		int sectionOffset = 0;
		
		tc.setFont("Sans PLAIN 12", 12f);
		tc.writeText("Designed and written by", 0, sectionOffset + 12);
		tc.setFont("Sans PLAIN 18", 18f);
		tc.writeText("James Waddington", 0, sectionOffset + 32);
		
		tc.setFont("Sans PLAIN 12", 12f);
		tc.writeText("contact@calefay.com", 0, sectionOffset + 46);
		
		tc.setFont("Sans PLAIN 10", 10f);
		tc.writeText("None of the work credited below was created", 0, sectionOffset + 64);
		tc.writeText("specifically for this game.", 0, sectionOffset + 76);
		tc.writeText("Thankyou to all for making their work available.", 0, sectionOffset + 88);
		tc.writeText("Special thanks to the jME community.", 0, sectionOffset + 100);
		
		sectionOffset += CREDITSYSIZE / CREDITSNUMBER;
		tc.setFont("Sans PLAIN 18", 18f);
		tc.writeText("Models and Textures", 0, sectionOffset + 18);
		tc.setFont("Sans PLAIN 12", 12f);
		tc.writeText("3drt.com", 0, sectionOffset + 38);
		tc.writeText("loopix-project.com", 0, sectionOffset + 52);
		tc.writeText("psionic3d.co.uk", 0, sectionOffset + 66);
		tc.writeText("DarkPhoenixX", 0, sectionOffset + 80);
		tc.writeText("turbosquid.com: macas, rigz, Johnnybuzt", 0, sectionOffset + 94);

		sectionOffset += CREDITSYSIZE / CREDITSNUMBER;
		tc.setFont("Sans PLAIN 18", 18f);
		tc.writeText("Music", 0, sectionOffset + 18);
		tc.setFont("Sans PLAIN 12", 12f);
		tc.writeText("Tumbling Motion", 0, sectionOffset + 38);
		tc.writeText("by http://www.freesoundtrackmusic.com", 0, sectionOffset + 52);
		tc.setFont("Sans PLAIN 18", 18f);
		tc.writeText("Sounds", 0, sectionOffset + 80);
		tc.setFont("Sans PLAIN 12", 12f);
		tc.writeText("Soundsnap.com: Stuart Duffield,", 0, sectionOffset + 100);
		tc.writeText("Radio Mall, BLASTWAVEFX", 0, sectionOffset + 113);
		tc.writeText("Soundgram Post, Mutanto", 0, sectionOffset + 126);
		
		sectionOffset += CREDITSYSIZE / CREDITSNUMBER;
		tc.setFont("Sans PLAIN 12", 12f);
		tc.writeText("Change vehicle:", 0, sectionOffset + 12); tc.writeText("Keys 1 to 4", 100, sectionOffset + 12);
		tc.writeText("Aim/Steer:", 0, sectionOffset + 30);tc.writeText("Mouse or W,A,S,D keys", 100, sectionOffset + 30);
		tc.writeText("Fire primary:", 0, sectionOffset + 48);tc.writeText("Left Mouse/Space", 100, sectionOffset + 48);
		tc.writeText("Fire secondary:", 0, sectionOffset + 66); tc.writeText("Right Mouse/Ctrl", 100, sectionOffset + 66);
		tc.writeText("Use upgrade:", 0, sectionOffset + 86); tc.writeText("Enter", 100, sectionOffset + 86);
		tc.writeText("Aircraft Throttle:", 0, sectionOffset + 102); tc.writeText("Mouse wheel", 100, sectionOffset + 102);
		tc.writeText("Pause:", 0, sectionOffset + 120); tc.writeText("Escape", 100, sectionOffset + 120);
		
		Texture tx = tc.createTexture();
		tx.setScale( new Vector3f(1.0f, 0.25f, 1.0f) );
		creditsOffset = new Vector3f(0, 0.75f, 0);
		tx.setTranslation(creditsOffset);

		TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
		ts.setTexture(tx);
		ts.setEnabled(true);
		
		new UITextureContainer("credits", ts, 256, 128, 0, 0, 11, creditsMenu);
		new UITextureContainer("controls", ts, 256, 128, 0, 32, 11, uiElements.get("controlsMenuContainer"));
		
	}
	
	public void cleanup() {
		rootNode = null;
		
		/* FIXME: 	ALL Containers (and probably some of the other references) need to have cleanup invoked.
		 			Currently only the baseContainer does, but most containers are not attached to it at any given moment.*/
		baseContainer.cleanup();
		inGameUI = null; 
		topLeftHUD = null; topRightHUD = null; 
		bottomLeftHUD = null; bottomRightHUD = null;
		centreLeftHUD = null; centreRightHUD = null;
		
		uiElements.clear(); uiElements = null;
		
		mainMenuContainer = null; gameMenu = null;
		settingsMenu = null; extrasContainer = null;
		
		extras.clear();
		currentSettings = null;
		mouse = null;
		GameWorldInfo.getGameWorldInfo().getEventHandler().removeListener("uiInternal");
		uiInternalListener = null;
		
		uiResources.cleanup();
		extrasResources.cleanup();
		uiResources = null;
		extrasResources = null;
	}
	
	/********************************* Parsing methods follow *******************************************/
	
	public void parseMenuFile(String menuFile) {
		try {
			BracketedTextParser reader = new BracketedTextParser();
			reader.openSourceFile(menuFile);
			
			StringBuffer block = null;
			do {
				block = reader.readBlock();
				if( (block != null) && (block.toString().trim().equalsIgnoreCase("container")) ) {
					parseContainer(new BracketedTextParser(reader.readBlock()));
				}
			} while(block != null);
			
			reader.closeSourceFile();
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
	}
	
	public UIContainer parseContainer(BracketedTextParser parser) {
		UIContainer container = null;
		try {
			StringBuffer block = null;
			do {
				block = parser.readBlock();
				if( (block != null) && (block.toString().trim().equalsIgnoreCase("container")) ) {
					UIContainer child = parseContainer(new BracketedTextParser(parser.readBlock()));
					if( (container != null) && (child != null) ) {container.attachChild(child);}	// This will force the header to be ahead of any children in the declaration. Might be worth adding the children to a temporary array to allow freedom in sequence. 
				}
				if( (block != null) && (block.toString().trim().equalsIgnoreCase("header")) ) {
					container = parseContainerHeader(BracketedTextParser.parseAttributeSet(parser.readBlock()));
					if(container != null) uiElements.put(container.getName(), container);
				}
			} while(block != null);
			
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
		return container;
	}
	
	private UIContainer parseContainerHeader(AttributeSet data) {
		if (data == null) return null;
		String[] attr = null;
		float[] floataAttr = null;
		String name = null, type = null;
		int x= 0, y = 0, z = 10, width = 10, height = 10;
		String clickEventType = null; String[] clickEventParameters = null; 
		boolean active = true;
		ColorRGBA bkCol = null; TextureState bkImg = null;
		
		if(data.hasAttribute("name")) {
			attr = data.getStringAttribute("name");
			if(attr.length == 1) {name = attr[0];}
		}
		if(data.hasAttribute("type")) {
			attr = data.getStringAttribute("type");
			if(attr.length == 1) {type = attr[0].trim().toLowerCase();}
		}
		if(data.hasAttribute("xywh")) {
			floataAttr = data.getFloatAttribute("xywh");
			if(floataAttr.length == 4) {
				x = (int)floataAttr[0]; y = (int)floataAttr[1]; width = (int)floataAttr[2]; height = (int)floataAttr[3];
				}
		}
		if(data.hasAttribute("z")) {
			floataAttr = data.getFloatAttribute("z");
			if(floataAttr.length == 1) {z = (int)floataAttr[0];}
		}
		
		if(data.hasAttribute("active")) {
			attr = data.getStringAttribute("active");
			if( (attr.length == 1) && (!attr[0].equalsIgnoreCase("true")) ) {active = false;}
		}
		
		if(data.hasAttribute("backgroundColor")) {
			floataAttr = data.getFloatAttribute("backgroundColor");
			if(floataAttr.length == 4) {
				bkCol = new ColorRGBA(floataAttr[0], floataAttr[1], floataAttr[2], floataAttr[3]);
			}
		}
		
		if(data.hasAttribute("backgroundImg")) {
			attr = data.getStringAttribute("backgroundImg");
			if(attr.length == 1) {bkImg = uiResources.getTextureState(attr[0]);}
		}
		
		if(data.hasAttribute("clickEvent")) {
			attr = data.getStringAttribute("clickEvent");
			if(attr.length == 1) {clickEventType = attr[0];}
		}
		
		clickEventParameters = data.getStringAttribute("clickParameters");
		
		if(type != null) {
			UIContainer element = null;
			type = type.trim();
			if(type.equalsIgnoreCase("container")) {
				element = new UIContainer(name, x, y, z, width, height);
			} else if(type.equalsIgnoreCase("confirmbox")) {
				element = buildConfirmBox(name, x, y, z, width, height, data);
			} else if(type.equalsIgnoreCase("button")) {
				element = buildButton(name, x, y, z, width, height, data);
			} else if(type.equalsIgnoreCase("image")) {
				element = buildImage(name, x, y, z, width, height, data);
			} else if(type.equalsIgnoreCase("text")) {
				element = buildText(name, x, y, z, width, height, data);
			}  else if(type.equalsIgnoreCase("checkbox")) {
				element = buildCheckBox(name, x, y, z, width, height, data);
			} else if(type.equalsIgnoreCase("resolutionselector")) {
					element = buildResolutionSelector(name, x, y, z, width, height, data);
			}
			
			if(bkCol != null) {
				UIContainerBackground background = new UIContainerBackground(bkImg, bkCol, element);
				uiElements.put(background.getName(), background);	// FIXME: Don't want to put the uiElement here, rather have the backgrounds a part of the container so they can resize and be easily accessed.
			}
		
			element.setActive(active);
			if(clickEventType != null) {
				element.setClickEventType(clickEventType);
				element.setClickEventParameters(clickEventParameters);
			}
			
			return element; 
		}
		
		return null;
	}
	
	private UITextureContainer buildImage(String name, int x, int y, int z, int width, int height, AttributeSet data) {
		TextureState image = null;
		
		if(data.hasAttribute("image")) {
			String[] attr = data.getStringAttribute("image");
			if(attr.length == 1) {image = uiResources.getTextureState(attr[0]);}
		}
		
		return new UITextureContainer(name, image, width, height, x, y, z, null);
	}
	

	private UITextContainer buildText(String name, int x, int y, int z, int width, int height, AttributeSet data) {
		String[] text = null;
		String font = null; float fontSize = 0;
		text = data.getStringAttribute("text");
		
		String[] attr = data.getStringAttribute("font");
		if( (attr != null) && (attr.length == 1)) {font = attr[0];}
		float[] floatAttr = data.getFloatAttribute("fontsize");
		if( (floatAttr != null) && (floatAttr.length == 1) ) {fontSize = floatAttr[0];}
		
		if( (font != null) && (fontSize > 0)) return new UITextContainer(name, width, height, x, y, z, null, font, fontSize, text);
		return new UITextContainer(name, width, height, x, y, z, null, text);
	}
	
	private UIMenuButton buildButton(String name, int x, int y, int z, int width, int height, AttributeSet data) {
		TextureState image = null;
		
		if(data.hasAttribute("image")) {
			String[] attr = data.getStringAttribute("image");
			if(attr.length == 1) {image = uiResources.getTextureState(attr[0]);}
		}
		
		return new UIMenuButton(name, image, width, height, x, y, z, null);
	}
	
	private UICheckBox buildCheckBox(String name, int x, int y, int z, int width, int height, AttributeSet data) {
		TextureState image = null;
		boolean checked = false;
		
		if(data.hasAttribute("image")) {
			String[] attr = data.getStringAttribute("image");
			if(attr.length == 1) {image = uiResources.getTextureState(attr[0]);}
		}
		
		if(data.hasAttribute("checked")) {
			String[] attr = data.getStringAttribute("checked");
			if( (attr.length == 1) && (attr[0].equalsIgnoreCase("true")) ) {checked = true;}
		}
		
		return new UICheckBox(name, checked, image, width, height, x, y, z, null);
	}
	
	private UIConfirmBox buildConfirmBox(String name, int x, int y, int z, int width, int height, AttributeSet data) {
		if (data == null) return null;
		String[] attr = null;
		String[] text = null;
		TextureState yesImg = null, noImg = null;
		String yesEvent = null, noEvent = null;
		Object yesParameters = null, noParameters = null;
		
		if(data.hasAttribute("text")) {
			text = data.getStringAttribute("text");
		}
		
		if(data.hasAttribute("yesImg")) {
			attr = data.getStringAttribute("yesImg");
			if(attr.length == 1) {yesImg = uiResources.getTextureState(attr[0]);}
		}
		
		if(data.hasAttribute("noImg")) {
			attr = data.getStringAttribute("noImg");
			if(attr.length == 1) {noImg = uiResources.getTextureState(attr[0]);}
		}
		
		if(data.hasAttribute("yesEvent")) {
			attr = data.getStringAttribute("yesEvent");
			if(attr.length == 1) {yesEvent = attr[0];}
		}
		
		if(data.hasAttribute("noEvent")) {
			attr = data.getStringAttribute("noEvent");
			if(attr.length == 1) {noEvent = attr[0];}
		}
		
		yesParameters = data.getStringAttribute("yesParameters");
		noParameters = data.getStringAttribute("noParameters");
		
		
		UIConfirmBox confirm = new UIConfirmBox(name, yesImg, noImg, x, y, z, width, height, text);
		if(yesEvent != null) {confirm.setYesEvent(yesEvent, yesParameters);}
		if(noEvent != null) {confirm.setNoEvent(noEvent, noParameters);}
		
		return confirm;
	}
	
	private UISelector buildResolutionSelector(String name, int x, int y, int z, int width, int height, AttributeSet data) {
		TextureState image = null;
		if(data.hasAttribute("image")) {
			String[] attr = data.getStringAttribute("image");
			if(attr.length == 1) {image = uiResources.getTextureState(attr[0]);}
		}
		
		UISelector resolutionSelector = new UISelector("resolutionselector", image, null, 150, 16, 192, 44, 10);
		// TODO: This, better.
		DisplayMode[] allDisplayModes = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayModes();
		highestValidBitDepths = new HashMap<String, Integer>();
		lowestValidRefreshRates = new HashMap<String, Integer>();
		ArrayList<String> availableResolutions = new ArrayList<String>();
		ArrayList<EDScreenMode> availableEDScreenModes = new ArrayList<EDScreenMode>();
		for(DisplayMode mode : allDisplayModes) {
			String label = mode.getWidth() + " x " + mode.getHeight();
			if(mode.getWidth() < 640) continue;
			if( !availableResolutions.contains(label) ) {
				availableResolutions.add(label);
				availableEDScreenModes.add( new EDScreenMode(mode.getWidth(), mode.getHeight()));
			}
			Integer bpp = highestValidBitDepths.get(label);
			if( (bpp == null) || (bpp < mode.getBitDepth()) )  highestValidBitDepths.put(label, (Integer)mode.getBitDepth());
			Integer hz = lowestValidRefreshRates.get(label);
			if( (hz == null) || (hz > mode.getRefreshRate()) ) lowestValidRefreshRates.put(label, (Integer)mode.getRefreshRate());
		}
		resolutionSelector.replaceOption(0, availableResolutions.get(0), availableEDScreenModes.get(0));
		for(int i = 1; i < availableResolutions.size(); i++) {
			resolutionSelector.addOption(availableResolutions.get(i), availableEDScreenModes.get(i));
		}
		resolutionSelector.setCurrentChoice(0);
		resolutionSelector.addBackground(BACKGROUND_DKBLUE_80);	// FIXME: Hardcoded background.
		
		return resolutionSelector;
	}
}
