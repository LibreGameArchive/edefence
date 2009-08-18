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
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.calefay.exodusDefence.EDMenuSystem.HUDAttachPoint;
import com.calefay.exodusDefence.EDMenuSystem.UIMode;
import com.calefay.exodusDefence.edAI.EDAIController;
import com.calefay.exodusDefence.edControls.EDAircraftInputHandler;
import com.calefay.exodusDefence.edControls.EDTurretInputHandler;
import com.calefay.exodusDefence.edControls.TurretCameraHandler;
import com.calefay.exodusDefence.entities.EDAircraft;
import com.calefay.exodusDefence.entities.EDCombatAircraft;
import com.calefay.exodusDefence.entities.EDTurret;
import com.calefay.exodusDefence.mission.EDMission;
import com.calefay.exodusDefence.radar.TargetAcquirer.RadarMode;
import com.calefay.simpleUI.EDUIPlayerStatus;
import com.calefay.simpleUI.UIChangeableTextContainer;
import com.calefay.simpleUI.UITextureContainer;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.calefay.utils.GameEventHandler.GameEventListener;
import com.jme.bounding.BoundingBox;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial.CullHint;
import com.jme.scene.Spatial.LightCombineMode;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.ZBufferState;
import com.jme.system.DisplaySystem;
import com.jmex.terrain.TerrainBlock;

public class EDGenericInterface {
	public enum EDMenuState {MAINMENU, PAUSEMENU, GAMEPLAY};
	
	private static final float PAUSELOCKTIME = 1.0f;
	private static final int MAXNARRATIVES = 3;
	private static final float FSTEP = (1.0f /(MAXNARRATIVES + 1));
	private static final int NARRATIVEPADDING = 4;
	
	private RemoveableEntityManager entityManager = null;
	
	private GameResourcePack hudResources = null;
	
	private int currentLevelNumber = 0, currentTutorialNumber = 0;
	private boolean inTutorial = false;
	
	private EDPlayer player = null;
	private ArrayList<PlayableEntity> playableEntities = null;
	private HashMap<PlayableEntity, EDUIPlayerStatus> statusIcons = null;
	private int currentPlayerEntity = 0;
	
	private GameEventListener uiListener = null;
	
	private boolean invertY = false;
	private boolean narrativeBackgrounds = true;
	
	private Quad crosshair = null;
	private Quad lockonIndicator = null;
	
	private TurretCameraHandler camHandler = null;
	private EDTurretInputHandler input = null;
	private EDAircraftInputHandler airInput = null;
	
	private EDPreferences menuSettings = null;
	public EDMenuSystem menu = null;		// TODO: Make private.
	public EDMenuState menuState = EDMenuState.MAINMENU;	// TODO: Make private.
	private float pauseLock = 0; // Time delay before you can pause again to allow easy release.
	
	private UITextureContainer upgradeIcon = null;
	private TextureState upgradeTextureState = null;
	
	private EDGameModel gameModel = null;
	private EDControllerHandler entityController = null;
	
	private ArrayList<EDNarrative> narrativeList = null;
	
	// Rather not have to import settings - this should be the class that sets up the display etc.
	// Rather not have to import the menuRoot - perhaps the game should have a root and the menu, and up to the display handler how they are attached.
	public EDGenericInterface(  EDGameModel gameModel,
								EDControllerHandler entityController,
								EDAudioEventProcessor audioProcessor,
								String resourcePackPath, 
								EDPreferences settings, 
								Node menuRoot,
								boolean showTitleBar) {
		this.gameModel = gameModel;
		this.entityController = entityController;
		
		player = new EDPlayer();
		gameModel.addPlayer(player);
		
		menuSettings = settings;
		menu = new EDMenuSystem(menuRoot, menuSettings);
		uiListener = GameWorldInfo.getGameWorldInfo().getEventHandler().addListener("uievents", 500);		// TODO: Use enum!
		entityManager = new RemoveableEntityManager();
		
		invertY = false; narrativeBackgrounds = true;
		
		playableEntities = new ArrayList<PlayableEntity>();
		statusIcons = new HashMap<PlayableEntity, EDUIPlayerStatus>();
		
		hudResources = new GameResourcePack();
		hudResources.parseResourcePack(resourcePackPath);
		hudResources.loadPending();
		hudResources.createTextureStates(DisplaySystem.getDisplaySystem().getRenderer());
		
		setupHUD();
		
		narrativeList = new ArrayList<EDNarrative>();
		
		KeyBindingManager.getKeyBindingManager().set("pause", KeyInput.KEY_ESCAPE);
		KeyBindingManager.getKeyBindingManager().set("APPLYUPGRADE", KeyInput.KEY_RETURN);

		KeyBindingManager.getKeyBindingManager().set("GUN1", KeyInput.KEY_1);
		KeyBindingManager.getKeyBindingManager().set("GUN2", KeyInput.KEY_2);
		KeyBindingManager.getKeyBindingManager().set("GUN3", KeyInput.KEY_3);
		KeyBindingManager.getKeyBindingManager().set("GUN4", KeyInput.KEY_4);
	}
	
	public void initializeLevel(EDLevel level, Camera cam, TerrainBlock tb) {	// FIXME: don't like camera and terrainblock as parameters.
		currentPlayerEntity = -1;	// Make sure it is considered a change when the first turret is assigned (or it will do nothing if it's 0).
		
		buildCrosshair();
		
		camHandler = new TurretCameraHandler(GameWorldInfo.getGameWorldInfo().getRootNode(), cam);
		camHandler.setTerrainAvoidance(tb);
		input = new EDTurretInputHandler(null, camHandler);
		input.setInvertY(invertY);
		airInput = new EDAircraftInputHandler(null, camHandler);
		airInput.setInvertY(invertY);
		airInput.setEnabled(false);
		
		if(level.missions != null) for(EDMission mission : level.missions) {
			if(mission.showOnHUD()) {
				int ySize = (mission.getHUDObjectiveCount() * 15) + 40;
				UIChangeableTextContainer container = new UIChangeableTextContainer(mission.getName() + "MissionPanel", 128, ySize, 20, -ySize - 20, 50);
				menu.addBackground(container);
				menu.attachToHUD(container, HUDAttachPoint.TOPLEFT);
				mission.setHUDPanel(container);
			}
		}
	}
	
	public void update(float interpolation) {		
		if(pauseLock > 0f) pauseLock -= interpolation;
		if(pauseLock < 0f) pauseLock = 0f;
		
		checkControlKeys();
		
		if(menuState == EDMenuState.GAMEPLAY) updateGameplay(interpolation);
		if( (menuState == EDMenuState.MAINMENU) || (menuState == EDMenuState.PAUSEMENU)) updateUI(interpolation);
	}
	
	public void updateGameplay(float interpolation) {
		input.update(interpolation);
		airInput.update(interpolation);
		camHandler.update();
		entityManager.updateEntities(interpolation);
		
		// ------------------- Very hacky bit begins --------------
		if( (currentPlayerEntity >= 0) && (currentPlayerEntity < playableEntities.size()) ) {
			// // FIXME: Switch the change of appearence to event based - optional event queue field on EDTurretRadar
			PlayableEntity entity = playableEntities.get(currentPlayerEntity);
			if(!entity.isActive()) currentPlayerEntity = -1;
			if( (entity != null) && (entity.getRadar() != null) && 
				( (entity.getRadar().getMode() == RadarMode.LOCKED) || (entity.getRadar().getMode() == RadarMode.LOCKING) ) ) {
				if(entity.getRadar().getMode() == RadarMode.LOCKING) lockonIndicator.setSolidColor(ColorRGBA.green);
				if(entity.getRadar().getMode() == RadarMode.LOCKED) lockonIndicator.setSolidColor(ColorRGBA.red);
				lockonIndicator.setCullHint(CullHint.Never);
				Vector3f trackPos = entity.getRadar().getTrackingPosition();
				if(trackPos != null) lockonIndicator.getLocalTranslation().set( DisplaySystem.getDisplaySystem().getScreenCoordinates(trackPos)); 
				lockonIndicator.updateWorldVectors();
			} else {lockonIndicator.setCullHint(CullHint.Always);}
		}
		// ------------------- Very hacky bit ends --------------
		
		// TODO: intermittently check and NULL (not remove) inactive elements from the playable entities array so as not to hold references. Also remove the inactive statusicon
		
		if(player.upgradeUpdated) updateUpgradeIcon();
		
		checkPlayerWithinBounds();
		
		checkGameKeys();
	}
	
	private void checkPlayerWithinBounds() {
		if( (currentPlayerEntity >= playableEntities.size()) || (currentPlayerEntity < 0) ) return;
		
		Vector3f boundsMin = gameModel.getCurrentLevel().playerBoundsMin; Vector3f boundsMax = gameModel.getCurrentLevel().playerBoundsMax;
		if( (boundsMin == null) || (boundsMax == null) ) return;
		GameEntity p = playableEntities.get(currentPlayerEntity);
		if(!(p instanceof EDCombatAircraft)) return;
		EDCombatAircraft a = (EDCombatAircraft)p;
		Vector3f playerPos = a.getAircraftNode().getLocalTranslation();	// This is tacky and temporary.
		
		if(playerPos.x < boundsMin.x) playerPos.x = boundsMin.x;
		if(playerPos.y < boundsMin.y) playerPos.y = boundsMin.y;
		if(playerPos.z < boundsMin.z) playerPos.z = boundsMin.z;
		if(playerPos.x > boundsMax.x) playerPos.x = boundsMax.x;
		if(playerPos.y > boundsMax.y) playerPos.y = boundsMax.y;
		if(playerPos.z > boundsMax.z) playerPos.z = boundsMax.z;
	}
	
	private void updateUI(float interpolation) {
		menu.updateUI(interpolation);
		GameEvent uiEvent = uiListener.getNextEvent();
		
		while (uiEvent != null) {
			if(uiEvent.getEventType() == "UIExitGame") {GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("quittodesktop", "SYSTEM", null, null);}
			else if(uiEvent.getEventType() == "UINewGame") {
				inTutorial = false;
				String s = null;
				if(uiEvent.getEventInitiator() != null) {
					Integer n = (Integer)uiEvent.getEventInitiator();
					s = getLevelPath(n);
					setLevelNumber(n);
				} else s = getCurrentLevelPath();
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("NewGame",  "SYSTEM", s, null);
			} else if(uiEvent.getEventType() == "UINewTutorial") {
				inTutorial = true;
				String s = null;
				if(uiEvent.getEventInitiator() != null) s = getTutorialPath( (Integer)uiEvent.getEventInitiator() );
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("NewGame",  "SYSTEM", s, null);
			} else if(uiEvent.getEventType() == "UIResume") {
				airInput.setFirstFrame(true); input.setFirstFrame(true);
				menuState = EDMenuState.GAMEPLAY;
				pauseLock = PAUSELOCKTIME;
			} else if(uiEvent.getEventType() == "UIRestartLevel") {
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("RestartLevel",  "SYSTEM", getCurrentLevelPath(), null);
			} else if(uiEvent.getEventType() == "UIEndGame") {
				setLevelNumber(menu.getSettings().getLevelSelection());
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("EndGame",  "SYSTEM", getCurrentLevelPath(), null);
			} else if(uiEvent.getEventType() == "UIGraphicsApply") {
				narrativeBackgrounds = menu.getSettings().narrativeBackgrounds;
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("DisplayReset",  "SYSTEM", null, null);
			} else if(uiEvent.getEventType() == "UIGameSettingsApply") {
				invertY = menu.getSettings().getInvertY();
				if(input != null) input.setInvertY(invertY);
				setLevelNumber( menu.getSettings().getLevelSelection());
				if(menu.getSettings().loggingEnabled()) Logger.getLogger("com.jme").setLevel(Level.ALL); else Logger.getLogger("com.jme").setLevel(Level.OFF);
			} else if(uiEvent.getEventType() == "UISoundSettingsApply") {
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("SoundSettingsApply", "SYSTEM", null, null);
			}
			
			uiEvent = uiListener.getNextEvent();
		}

	}
	
	public String getCurrentLevelPath() {
		return inTutorial ? EDLevel.tutorialPaths[currentTutorialNumber] : EDLevel.levelPaths[currentLevelNumber];
	}
	
	public void setLevelNumber(int n) {
		if( (n >= 0) && (n < EDLevel.levelPaths.length) ) currentLevelNumber = n;
	}

	public String getLevelPath(int levelNumber) {
		if( (levelNumber < 0) || (EDLevel.levelPaths == null) || (levelNumber >= EDLevel.levelPaths.length)) {
			System.err.println("ERROR: Could not initialize level, missing data.");
			return null;
		} else return EDLevel.levelPaths[levelNumber];
	}
	
	public String getTutorialPath(int tutorialNumber) {
		if( (tutorialNumber < 0) || (EDLevel.tutorialPaths == null) || (tutorialNumber >= EDLevel.tutorialPaths.length)) {
			System.err.println("ERROR: Could not initialize tutorial, missing data.");
			return null;
		} else return EDLevel.tutorialPaths[tutorialNumber];
	}
	
	public void completeLevel() {		
		if(inTutorial) {
			currentTutorialNumber++; if(currentTutorialNumber >= EDLevel.tutorialPaths.length) currentTutorialNumber = 0;
		} else {
			currentLevelNumber++; if(currentLevelNumber >= EDLevel.levelPaths.length) currentLevelNumber = 0;
		}
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("RestartLevel",  "SYSTEM", getCurrentLevelPath(), null);
	}

	public void doLevelFailedCheck() {	// Most of this could be done directly from the menu system.
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("LevelFailedRestartCheck",  "uiInternal", null, null);
		menu.activateMenuSystem();
		menu.setMode(UIMode.PAUSEMENU);
		menuState = EDMenuState.PAUSEMENU;
		pauseLock = PAUSELOCKTIME;
	}
	
	public void addStatusIcon(PlayableEntity entity, String iconLabel) {
		int number = 1;
		for(number = 1; ( (number <= 4) && (number <= playableEntities.size()) ); number++ ) {
			PlayableEntity e = playableEntities.get(number - 1);
			if( (e == null) || (e.isDead())) {
				statusIcons.remove(e);
				break;
			}
		}
		if(number <= playableEntities.size()) playableEntities.set(number - 1, entity); else playableEntities.add(entity);
		
		EDUIPlayerStatus icon = new EDUIPlayerStatus("StatusIcon" + number, entity,
				hudResources.getTexture(iconLabel),hudResources.getTexture("turret" + number),
				hudResources.getTextureState("healthbar"),hudResources.getTextureState("heatbar"),
				128, 128, 10 + ( (number - 1) * 140), 10, 90);
		menu.attachToHUD(icon, HUDAttachPoint.BOTTOMLEFT);
		statusIcons.put(entity, icon);
		entityManager.add(icon);
	}
	
	public void setActiveEntity(int newEntityIndex) {
		if( (newEntityIndex < 0) || (newEntityIndex == currentPlayerEntity) || (newEntityIndex >= playableEntities.size()) ) return;
		PlayableEntity newActiveEntity = playableEntities.get(newEntityIndex);
		if( (newActiveEntity == null) || (!newActiveEntity.isActive()) ) return;
		
		camHandler.setTrackNode(newActiveEntity.getCameraTrackNode());
		attachCrosshair(newActiveEntity.getCameraTrackNode());
 
		// FIXME: THIS next section needs to work for both types but currently doesn't.
		if((currentPlayerEntity >= 0) && (currentPlayerEntity < playableEntities.size())) {
			PlayableEntity oldActiveEntity = playableEntities.get(currentPlayerEntity);
			
			EDUIPlayerStatus icon = statusIcons.get(oldActiveEntity);
			if(icon != null) icon.setTransparency(0.5f);
			
			EDAIController ai = entityController.getEntityController(oldActiveEntity); //entityAI.get(oldActiveEntity);
			if(ai != null) ai.setSuspend(false);
		}
		statusIcons.get(newActiveEntity).setTransparency(1.0f);
		
		EDAIController ai = entityController.getEntityController(newActiveEntity); //entityAI.get(newActiveEntity);
		if(ai != null) ai.setSuspend(true);
		
		switch(newActiveEntity.getControlsType()) {
		case FIXEDWING:			
			airInput.setHandledAircraft( (EDAircraft)newActiveEntity);
			airInput.setIsHelicopterHandler(false);
			airInput.setEnabled(true); airInput.setFirstFrame(true); input.setEnabled(false);
			break;
		case HELICOPTER:			
			airInput.setHandledAircraft( (EDAircraft)newActiveEntity);
			airInput.setIsHelicopterHandler(true);
			airInput.setEnabled(true); airInput.setFirstFrame(true); input.setEnabled(false);
			break;
		case TURRET: 
			input.setHandledTurret( (EDTurret)newActiveEntity );
			airInput.setEnabled(false); input.setEnabled(true);
			break;
		}
		setupFireKeys(newActiveEntity);
		currentPlayerEntity = newEntityIndex;
		
	}
	
	private void setupHUD() {		
		upgradeIcon = new UITextureContainer("upgradeIcon", 128, 128, -150, -150, 10, null);
		upgradeTextureState = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
		upgradeTextureState.setEnabled(true);
		upgradeIcon.setTransparency(0.5f);
		upgradeIcon.setActive(false);
		upgradeIcon.setTextureState(upgradeTextureState);
		menu.attachToHUD(upgradeIcon, HUDAttachPoint.TOPRIGHT);	
	}
	
	private void checkGameKeys() {
		int newTurret = -1;
		if (KeyBindingManager.getKeyBindingManager().isValidCommand("GUN1")) {newTurret = 0;}
		if (KeyBindingManager.getKeyBindingManager().isValidCommand("GUN2")) {newTurret = 1;}
		if (KeyBindingManager.getKeyBindingManager().isValidCommand("GUN3")) {newTurret = 2;}
		if (KeyBindingManager.getKeyBindingManager().isValidCommand("GUN4")) {newTurret = 3;}
		
		if (KeyBindingManager.getKeyBindingManager().isValidCommand("APPLYUPGRADE")) {applyCurrentUpgrade();}

		if( (newTurret > -1) && (newTurret != currentPlayerEntity) && (newTurret < playableEntities.size()) ) {
			setActiveEntity(newTurret);
		}
	}
	
	private void checkControlKeys() {
		
		if (KeyBindingManager.getKeyBindingManager().isValidCommand("pause") && (pauseLock == 0) ) {
			switch(menuState) {
			case MAINMENU:
				// TODO: Get this to go to previous menu/exit game.
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("quittodesktop", "SYSTEM", this, null);
				pauseLock = PAUSELOCKTIME;
				break;
			case PAUSEMENU:
				menu.resumeGameplay();
				menuState = EDMenuState.GAMEPLAY;
				pauseLock = PAUSELOCKTIME;
				break;
			case GAMEPLAY:
				menu.activateMenuSystem();
				menu.setMode(UIMode.PAUSEMENU);
				menuState = EDMenuState.PAUSEMENU;
				pauseLock = PAUSELOCKTIME;
				break;
			}
		}
			
	}
	
	public void setupFireKeys(PlayableEntity entity) {
		switch(entity.getControlsType()) {
		case FIXEDWING:
		case HELICOPTER:
			airInput.clearFireKeys();
			if(entity.getPrimaryGunBarrel1() != null) {
				airInput.addFireKey(entity.getPrimaryGunBarrel1(), KeyInput.KEY_SPACE);
				airInput.addFireMouseButton(entity.getPrimaryGunBarrel1(), 0);
			}
			if(entity.getPrimaryGunBarrel1() != null) {
				airInput.addFireKey(entity.getPrimaryGunBarrel2(), KeyInput.KEY_SPACE);
				airInput.addFireMouseButton(entity.getPrimaryGunBarrel2(), 0);
			}
			
			break;
		case TURRET:
			input.clearFireKeys();
			if(entity.getPrimaryGunBarrel1() != null) {
				input.addFireKey(entity.getPrimaryGunBarrel1(), KeyInput.KEY_SPACE);
				input.addFireMouseButton(entity.getPrimaryGunBarrel1(), 0);
			}
			if(entity.getPrimaryGunBarrel2() != null) {
				input.addFireKey(entity.getPrimaryGunBarrel2(), KeyInput.KEY_SPACE);
				input.addFireMouseButton(entity.getPrimaryGunBarrel2(), 0);
			}
			if(entity.getSecondaryGun() != null) {
				input.addFireKey(entity.getSecondaryGun(), KeyInput.KEY_LCONTROL);
				input.addFireMouseButton(entity.getSecondaryGun(), 1);
			}
			break;
		}
		
	}
	
	public void applyCurrentUpgrade() {
		if((currentPlayerEntity >= 0) && (currentPlayerEntity < playableEntities.size())) applyCurrentUpgrade(playableEntities.get(currentPlayerEntity));
	}
	
	public void applyCurrentUpgrade(PlayableEntity entity) {
		upgradeIcon.setActive(false);
		//player.applyCurrentUpgrade(entity);
		gameModel.applyUpgrade(entity, player);
		setupFireKeys(playableEntities.get(currentPlayerEntity));	// FIXME: **** THIS NEEDS DOING but won't work here are the upgrade is applied via an event.
	}
	
	private void buildCrosshair() {	// TODO: Take a look at this whole method.
		crosshair = new Quad("Targetcrosshair", 9.0f, 9.0f);
		Quaternion q = new Quaternion();
		q.fromAngleNormalAxis(FastMath.PI, Vector3f.UNIT_Y);
		crosshair.getLocalRotation().multLocal(q);
		crosshair.setModelBound(new BoundingBox());
		crosshair.updateModelBound();
		
		crosshair.setRenderState(hudResources.getTextureState("crosshair"));
		
		BlendState as = DisplaySystem.getDisplaySystem().getRenderer().createBlendState();
		as.setBlendEnabled(true);
		as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
		as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
		as.setTestEnabled(true);
		as.setTestFunction(BlendState.TestFunction.GreaterThan);
		crosshair.setRenderState(as);
		
		ZBufferState zs = DisplaySystem.getDisplaySystem().getRenderer().createZBufferState();
		zs.setFunction(ZBufferState.TestFunction.Always);
		zs.setWritable(false);
		crosshair.setRenderState(zs);
		
		//crosshair.setCullMode(SceneElement.CULL_NEVER);
		crosshair.setIsCollidable(false);
		
		crosshair.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		crosshair.setLightCombineMode(LightCombineMode.Off);
		crosshair.setSolidColor(new ColorRGBA(0, 0.7f, 0, 0.6f));
		
		crosshair.updateRenderState();
		
		lockonIndicator = new Quad("lockonIndicator", 20.0f, 20.0f);

		lockonIndicator.setModelBound(new BoundingBox());
		lockonIndicator.updateModelBound();
		
		lockonIndicator.setRenderState(hudResources.getTextureState("lockonindicator"));
		lockonIndicator.setRenderState(as);
		lockonIndicator.setRenderState(zs);
		lockonIndicator.setIsCollidable(false);
		lockonIndicator.setRenderQueueMode(Renderer.QUEUE_ORTHO);
		lockonIndicator.setLightCombineMode(LightCombineMode.Off);
		lockonIndicator.setSolidColor(ColorRGBA.red);
		lockonIndicator.updateRenderState();
		
		GameWorldInfo.getGameWorldInfo().getRootNode().attachChild(lockonIndicator);	// FIXME: Prob don't attach here and def do cleanup.
		
	}
	 
	public void updateUpgradeIcon() {
		if(player.getAvailableUpgradeType() == null) return;
		player.upgradeUpdated = false;	// ********** tacky!
		switch(player.getAvailableUpgradeType()) {
		case REPAIR: upgradeTextureState.setTexture(hudResources.getTexture("repair")); break;
		case DOUBLE: upgradeTextureState.setTexture(hudResources.getTexture("double")); break;
		case LAUNCHER: upgradeTextureState.setTexture(hudResources.getTexture("launcher")); break;
		case LASER: upgradeTextureState.setTexture(hudResources.getTexture("laser")); break;
		case PLASMA: upgradeTextureState.setTexture(hudResources.getTexture("plasma")); break;
		}
		
		upgradeIcon.setActive(true);
	}
	
	private void attachCrosshair(Node parentNode) {
		crosshair.removeFromParent();
		parentNode.attachChild(crosshair);
		crosshair.setLocalTranslation(0, 0, 100.0f);
	}
	
	public void addNarrative(String[] args) {
		if(args.length < 2) return;
		float t = Float.parseFloat(args[0]);
		TextureState ts = null;
		ts = hudResources.getTextureState(args[1]);	// TODO: Perhaps script the character rather than assuming a texture with the same name is present.
		String[] text = new String[args.length - 2];
		System.arraycopy(args, 2, text, 0, args.length - 2);	// Don't much like this but hey.
		EDNarrative container = new EDNarrative(t, text, 98, ts, DisplaySystem.getDisplaySystem().getRenderer());	// Should go near the back as otherwise it can interfere with clicking the UI in pause.
		if(narrativeBackgrounds) container.setBackgroundContainer(menu.addBackground(container));
		
		menu.attachToHUD(container, HUDAttachPoint.BOTTOMLEFT);
		entityManager.add(container);
		
		if(narrativeList.size() >= MAXNARRATIVES) {
			EDNarrative bumped = narrativeList.remove(0);
			bumped.deactivate();
		}
		
		narrativeList.add(container);
		
		int cumulativeY = 0; int ns = narrativeList.size();
		for(int i = ns - 1; i >= 0; i-- ) {
			EDNarrative n = narrativeList.get(i);
			if(n.isActive()) {
				float x = 1.0f - ( (ns - i - 1) * FSTEP );
				n.setFadeoutModifier( x);
				n.setOffset(-cumulativeY);
				cumulativeY += n.getYSize() + NARRATIVEPADDING;
			}
		}
		
	}
	
	public void cleanupLevel() {
		upgradeIcon.setActive(false);
		
		player.reset();
		
		if(playableEntities != null) playableEntities.clear();
		if(statusIcons != null) statusIcons.clear();
		
		if(camHandler != null) camHandler.cleanup(); camHandler = null;
		if(input != null) input.cleanup(); input = null;
		if(airInput != null) airInput.cleanup(); airInput = null;
		
		if(crosshair != null) crosshair.removeFromParent(); crosshair = null;
		if(lockonIndicator != null) lockonIndicator.removeFromParent(); lockonIndicator = null;
		
		if(entityManager != null) entityManager.clearEntities();
		
		if(narrativeList != null) {
			for(EDNarrative n : narrativeList) if(n != null) n.cleanup();
			narrativeList.clear();
		}
	}
	
	public void cleanup() {
		gameModel = null;
		entityController = null;
		
		if(playableEntities != null) playableEntities.clear(); playableEntities = null;
		if(statusIcons != null) statusIcons.clear(); statusIcons = null;
		
		if(hudResources != null) hudResources.cleanup(); hudResources = null;
		if(crosshair != null) crosshair.removeFromParent(); crosshair = null;
		if(lockonIndicator != null) lockonIndicator.removeFromParent(); lockonIndicator = null;
		if(upgradeIcon != null) upgradeIcon.cleanup(); upgradeIcon = null;
		if(upgradeTextureState != null) upgradeTextureState.deleteAll(); upgradeTextureState = null;
		if(camHandler != null) camHandler.cleanup(); camHandler = null;
		if(input != null) input.cleanup(); input = null;
		if(airInput != null) airInput.cleanup(); airInput = null;
		
		if(menu != null) menu.cleanup(); menu = null;
		menuSettings = null;
		
		if(entityManager != null) entityManager.cleanup(); entityManager = null;
		
		if(narrativeList != null) {
			for(EDNarrative n : narrativeList) if(n != null) n.cleanup();
			narrativeList.clear();
		} 
		narrativeList = null;
		
		player = null;	// Probably should be cleaned up here - currently done in game model.
		
		GameWorldInfo.getGameWorldInfo().getEventHandler().removeListener("uievents"); uiListener = null;
	}
}
