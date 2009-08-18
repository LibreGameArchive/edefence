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

import com.calefay.exodusDefence.EDGameModel.TurretUpgrade;
import com.calefay.utils.GROMEObjectInstance;
import com.calefay.utils.GROMEParser;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameEventHandler;
import com.calefay.utils.GameEventScript;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.calefay.utils.GameEventHandler.GameEventListener;
import com.calefay.utils.GameEventScript.GameEventScriptInstance;

/* Controller class which reads scripts and based on them, acts on the game model.*/
public class EDScriptController {

	private EDGameModel gameModel = null;
	private GameEventHandler eventHandler = null;
	private GameEventListener scriptListener = null;
	private EDGenericInterface genericInterface = null;
	private GameResourcePack levelResources = null;
	private RemoveableEntityManager scriptManager = null;
	
	private HashMap<String, GameEventScriptInstance> labeledScripts = null;
	
	public EDScriptController(	EDGameModel model, 
								EDGenericInterface genericInterface, 
								GameEventHandler eventHandler) {
		this.gameModel = model;
		this.genericInterface = genericInterface;
		this.eventHandler = eventHandler;
		
		scriptManager = new RemoveableEntityManager();
		scriptListener = eventHandler.addListener("scriptEvents", 500);	// TODO: Use enum!
		
		labeledScripts = new HashMap<String, GameEventScriptInstance>();
	}
	
	public void update(float interpolation) {
		scriptManager.updateEntities(interpolation);
		checkGameplayEvents();
	}
	
	protected void checkGameplayEvents() {
		// FIXME: Lots of casts that don't check type or catch the possible exception.
		GameEvent e = scriptListener.getNextEvent();
		while(e != null) {
			if(e.getEventType().equals("_SCRIPT_SetActiveTurret")) {
				String[] args = (String[])e.getEventInitiator();
				int i = Integer.parseInt(args[0]);
				genericInterface.setActiveEntity(i - 1);
			} else if(e.getEventType().equals("_SCRIPT_Narrative")) {
				String[] args = (String[])e.getEventInitiator();
				genericInterface.addNarrative(args);
			} else if(e.getEventType().equals("_SCRIPT_spawnMissiles")) {
				String[] args = (String[])e.getEventInitiator();
				gameModel.spawnMissiles(args);
			} else if(e.getEventType().equals("_SCRIPT_damageEntity")) {
				String[] args = (String[])e.getEventInitiator();
				if( (args != null) && (args.length == 3) ) {
					float damageAmount = Float.parseFloat(args[2]);	// TODO: Catch the potential number format exception
					gameModel.applyDamage(args[0], args[1], damageAmount);
				}
			} else if(e.getEventType().equals("_SCRIPT_addUpgrade")) {
				String[] args = (String[])e.getEventInitiator();
				if( (args != null) && (args.length > 0) ) scriptAddUpgrade(args[0]);
			} else if(e.getEventType().equals("_SCRIPT_spawnAircraft")) {
				String[] args = (String[])e.getEventInitiator();
				gameModel.spawnAircraft(args);
			} else if(e.getEventType().equals("_SCRIPT_spawnDamageableScenery")) {
				String[] args = (String[])e.getEventInitiator();
				gameModel.spawnDamageableScenery(args);
			} else if(e.getEventType().equals("_SCRIPT_spawnPlayableAircraft")) {
				String[] args = (String[])e.getEventInitiator();
				gameModel.spawnPlayableAircraft(args);
			} else if(e.getEventType().equalsIgnoreCase("runscript")) {
				String[] args = (String[])e.getEventInitiator(); 
				if( (args != null) && (args.length > 0) ) {
					if(args.length > 1) addLabeledScriptInstance(args[0], args[1]); else addScriptInstance(args[0]);
				}
			} else if(e.getEventType().equals("ScriptTerminated")) {
				String label = (String)e.getEventInitiator();
				GameEventScriptInstance inst = (GameEventScriptInstance)e.getEventTarget();
				if( (label != null) && (inst != null) && (labeledScripts.get(label) == inst)) {labeledScripts.remove(label);}
			} else if(e.getEventType().equals("KillScript")) {
				String[] args = (String[])e.getEventInitiator();
				if( (args != null) && (args.length > 0) ) {
					GameEventScriptInstance inst = labeledScripts.get(args[0]);
					if(inst != null) {inst.deactivate();}
				}
			} else if(e.getEventType().equals("ApplyCurrentUpgrade")) {
				genericInterface.applyCurrentUpgrade();
			} else if(e.getEventType().equals("RocketLaunch")) {
				gameModel.launchRocket();
			} else if(e.getEventType().equals("LevelComplete")) {
				genericInterface.completeLevel();
			} else if(e.getEventType().equals("LevelFailed")) {
				genericInterface.doLevelFailedCheck();
			}
			//for(EDMission os : missions.values()) {os.checkEvent(gameEvent);}	// TODO: Consider removing this and giving the ObjectiveSet it's own listener on this queue.
 
			e = scriptListener.getNextEvent();
		}
	}
	
	/* Created a new instance of the specified script and sets it running. Returns a reference to the new instance.*/
	private GameEventScriptInstance addScriptInstance(String scriptName) {
		GameEventScript script = levelResources.getScript(scriptName);
		GameEventScriptInstance instance = null;
		if(script != null) {
			instance = script.getScriptInstance(eventHandler);
			instance.initializeScript();
			scriptManager.add(instance);
		}
		return instance;
	}
	
	/* Creates a new instance of the specified script and sets it running. The new instance can then be referenced by the supplied label.*/
	private void addLabeledScriptInstance(String scriptName, String label) {
		GameEventScriptInstance instance = addScriptInstance(scriptName);
		if(instance != null) {
			instance.setLabel(label); 
			labeledScripts.put(instance.getLabel(), instance);
		}
	}

	private void scriptAddUpgrade(String upgradeName) {
		TurretUpgrade upgrade = null;
		if(upgradeName.equalsIgnoreCase("double")) upgrade = TurretUpgrade.DOUBLE;
		else if(upgradeName.equalsIgnoreCase("laser")) upgrade = TurretUpgrade.LASER;
		else if(upgradeName.equalsIgnoreCase("launcher")) upgrade = TurretUpgrade.LAUNCHER;
		else if(upgradeName.equalsIgnoreCase("plasma")) upgrade = TurretUpgrade.PLASMA;
		else if(upgradeName.equalsIgnoreCase("repair")) upgrade = TurretUpgrade.REPAIR;
		if(upgrade != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("AddUpgrade","gameplayEvents", upgrade, null);
	}
	
	protected void loadGROMEScene(String path) {
		ArrayList<GROMEObjectInstance> instances = GROMEParser.loadGROMEScene(path);
		
		if(instances == null) return;
		
		for(GROMEObjectInstance i : instances) {
			if(GROMEObjectInstance.flagPresent("MOB", i.getFlags()))
				gameModel.spawnMOB(i.getName(), i.getTemplate(), i.getPosition(), i.getScale(), i.getRotation(), i.getFactions(), i.getFlags());
			else
				gameModel.spawnSceneObject(i.getName(), i.getTemplate(), i.getPosition(), i.getScale(), i.getRotation(), i.getFactions(), i.getFlags());
		}
    	
		instances.clear(); instances = null;
	}
	
	public void initializeLevel(EDLevel level, GameResourcePack levelResources) {
		this.levelResources = levelResources;
		addScriptInstance("main");
		
		if(level.getSceneryFilePath() != null) loadGROMEScene(level.getSceneryFilePath());
		if(level.getFoliageFilePath() != null) loadGROMEScene(level.getFoliageFilePath());
	}

	public void cleanupLevel() {
		if(scriptManager != null) scriptManager.clearEntities();
		if(labeledScripts != null) labeledScripts.clear();
		//scriptManager.printEntityList();
	}

	public void cleanup() {
		eventHandler.removeListener("scriptEvents"); scriptListener = null;
		
		if(scriptManager != null) scriptManager.cleanup(); scriptManager = null;
		if(labeledScripts != null) labeledScripts.clear(); labeledScripts = null;
		gameModel = null;
		eventHandler = null;
		genericInterface = null;
		levelResources = null;
	}

}
