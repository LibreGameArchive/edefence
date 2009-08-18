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


import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import com.calefay.effects.ManagedParticleEffect;
import com.calefay.exodusDefence.entities.EDCombatAircraft;
import com.calefay.exodusDefence.entities.EDDamageableScenery;
import com.calefay.exodusDefence.entities.EDEvacRocket;
import com.calefay.exodusDefence.entities.EDTurret;
import com.calefay.exodusDefence.mission.EDMission;
import com.calefay.exodusDefence.radar.EDTriangleZoneRadar;
import com.calefay.exodusDefence.weapons.BaseBeamWeapon;
import com.calefay.exodusDefence.weapons.BaseProjectile;
import com.calefay.exodusDefence.weapons.EDSeekingMissile;
import com.calefay.exodusDefence.weapons.MissileProjectile;
import com.calefay.exodusDefence.weapons.TracerProjectile;
import com.calefay.utils.AttributeSet;
import com.calefay.utils.GROMEObjectInstance;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.calefay.utils.GameEventHandler.GameEventListener;
import com.jme.bounding.BoundingBox;
import com.jme.bounding.CollisionTreeManager;
import com.jme.bounding.OrientedBoundingBox;
import com.jme.bounding.CollisionTree.Type;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.scene.Geometry;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.Spatial.CullHint;
import com.jmex.terrain.TerrainBlock;
import com.jmex.terrain.util.RawHeightMap;

public class EDGameModel {
	public static enum TurretUpgrade {REPAIR, DOUBLE, LAUNCHER, LASER, PLASMA};
	
	private Random rand = null;
	
	private Node gameWorldRoot = null;
	private EDGenericInterface genericInterface = null;
	private EDControllerHandler entityController = null;
	
	private TerrainBlock tb = null;
	private EDMap currentMap = null;
	
	private RemoveableEntityManager edManager = null;
	
	private GameResourcePack gameResources = null;
	private GameResourcePack levelResources = null;
	private EDTemplateSet entityTemplates = null;
	private EDTemplateSet sceneryTemplates = null;
	
	private EDLevel currentLevel = null;
	
	private HashMap<String, EDMission> missions = null;
	
	private HashMap<String, EDEntityMap> factionMaps = null;
	
	private ArrayList<Spatial> scenerySpatials = null;
	
	private EDEvacRocket rocket;
	
	private GameEventListener gameplayListener = null;
	
	private EDPlayer thePlayer = null;
	
	public EDGameModel(EDControllerHandler entityController, GameResourcePack resources) {
		EntityRegister.createEntityRegister();
		
		this.entityController = entityController;
		
		gameWorldRoot = new Node("gameWorldRoot node");
	    
	    rand = new Random();
	    
	    this.gameResources = resources;
	    
		entityTemplates = new EDTemplateSet();
		entityTemplates.loadTemplate("data/scripts/entitytemplates.txt");
		sceneryTemplates = new EDTemplateSet();
		sceneryTemplates.loadTemplate("data/scripts/scenerytemplates.txt");
		
		edManager = new RemoveableEntityManager();		
		
		factionMaps = new HashMap<String, EDEntityMap>();
		missions = new HashMap<String, EDMission>();
		defaultListener = GameWorldInfo.getGameWorldInfo().getEventHandler().addListener("default", 500);
		gameplayListener = GameWorldInfo.getGameWorldInfo().getEventHandler().addListener("gameplayEvents", 500);	// TODO: Use enum!
		
	    Node missileNode = (Node)gameResources.getSpatial("missile");
	    MissileProjectile.setMissileMesh( (TriMesh)missileNode.getChild(0));
	    MissileProjectile.setMissileTexture( gameResources.getTextureState("gunship"));
		TracerProjectile.setupMasterMesh(gameResources.getTextureState("tracer"));
		BaseProjectile.setBounds( new Vector3f(-1500, -250, -1500), new Vector3f(1500f, 500f, 1500f));
		
		CollisionTreeManager.getInstance().setTreeType(Type.OBB);
		//CollisionTreeManager.getInstance().setDoSort(true);
		
		/* ************************* DEBUG ONLY!!! *************************
        DebugMethods.addText(100);
        DebugMethods.addText(120);
        DebugMethods.addText(200);
        DebugMethods.addText(220);
        DebugMethods.addText(240);
		// *************************DEBUG ONLY ENDS************************/
	}
	
	public void setGenericInterface(EDGenericInterface edInterface) {this.genericInterface = edInterface;}
	
	public Node getGameWorldRoot() {return gameWorldRoot;}
	
	public void initializeLevel(EDLevel newLevel, GameResourcePack resources) {
		if(newLevel == null) {System.err.println("Error: Could not initialize new level - null parameter provided."); return;}
		this.currentLevel = newLevel;
		
		for(String faction : currentLevel.getFactions()) {
			factionMaps.put(faction, new EDEntityMap());
		}
		
		if(scenerySpatials == null) scenerySpatials = new ArrayList<Spatial>(); scenerySpatials.clear();
		
		this.levelResources = resources;

		buildTerrain(currentLevel);
		TracerProjectile.setGravity(0.049f);
		
		if((currentLevel.projectileBoundsMin != null) && (currentLevel.projectileBoundsMax != null) ) BaseProjectile.setBounds( currentLevel.projectileBoundsMin, currentLevel.projectileBoundsMax);
		
		currentMap = new EDMap(tb);
		
		if(currentLevel.missions != null) for(EDMission mission : currentLevel.missions) {
			missions.put(mission.getName(), mission);
			edManager.add(mission);
		}
				
	}
		
	public void finalizeLevelSetup() {
		gameWorldRoot.setModelBound(new OrientedBoundingBox()); gameWorldRoot.updateModelBound();
		//CollisionTreeManager.getInstance().generateCollisionTree(CollisionTree.OBB_TREE, tb, false);
		CollisionTreeManager.getInstance().generateCollisionTree(Type.OBB, gameWorldRoot, false);
		
		for(Spatial s : scenerySpatials) s.lock();
		
		gameWorldRoot.updateGeometricState(0.0f, true);
		gameWorldRoot.updateRenderState();
	}
	
	protected void checkGameplayEvents() {
		
		GameEvent gameEvent = gameplayListener.getNextEvent();
		while(gameEvent != null) {
			if(gameEvent.getEventType().equalsIgnoreCase("entitydestroyed")) {
				if(gameEvent.getEventInitiator() instanceof PlayableEntity) {
					removePlayableEntity( (PlayableEntity)gameEvent.getEventInitiator() );
				}
			}  else if(gameEvent.getEventType().equals("AddUpgrade")) {
				TurretUpgrade upgrade = (TurretUpgrade)gameEvent.getEventInitiator();
				thePlayer.addAvailableUpgrade(upgrade);
			}
			
			for(EDMission os : missions.values()) {os.checkEvent(gameEvent);}	// TODO: Consider removing this and giving the ObjectiveSet it's own listener on this queue.
 
			gameEvent = gameplayListener.getNextEvent();
		}
	}
	
	public void updateGameplay(float interpolation) {
		edManager.updateEntities(interpolation);
		
		processEntityEvents(interpolation);
		checkGameplayEvents();
		
		gameWorldRoot.updateGeometricState(interpolation, true);	// FIXME: Is this necessary if everything updates itself when needed? Is this more or less effecient?
	}	
	
	// TODO: Replace this with an event type which sends a launch acrion to the rocket via GameActionable.
	public void launchRocket() {
		// TODO: If the rocket is dead it should get cleaned up and nulled.
		if( (rocket != null) && (!rocket.isLaunching()) && (!rocket.isDead()) ) {
			rocket.addParticleTrail("rocketblast.jme", "evacrocket-blowback.jme");
			rocket.initiateLaunch();
		}
	}
	
	public TerrainBlock getTerrain() {return tb;}
	
	private void buildTerrain(EDLevel level) {
		if(level.getMapPath() == null) {System.err.println("ERROR: No valid map supplied."); return;}
        URL mapURL = getClass().getClassLoader().getResource(level.getMapPath());
        if(mapURL == null) {System.err.println("ERROR: No valid map supplied."); return;}
        int heightMapSize = level.getMapSize();
        if(heightMapSize <= 0) {System.err.println("ERROR: Map size is invalid."); return;}
        if( (level.splatBaseLayer == null) || (level.splatBaseLayer.texture == null) ) {
        	System.err.println("ERROR: No terrain base texture found."); return;
        }
        
        RawHeightMap heightMap = new RawHeightMap(mapURL, heightMapSize,
                RawHeightMap.FORMAT_16BITLE, false);

        /* Y Scale is based on GROME export - it gives the height range of the input, which is then normalized.
         * Scale is (maxheight - minheight) / 65535
         * Terrain y offset is minheight. This is necessary to ensure that objects are positioned correctly relative to the terrain.*/
        float heightScale = ( (level.getMapMaxHeight() - level.getMapMinHeight()) / 65535f);
        Vector3f terrainScale = new Vector3f(level.getTerrainScale(), heightScale, level.getTerrainScale());
        heightMap.setHeightScale(0.25f);	// TODO: Check exactly what this does/if it's necessary.

        tb = new TerrainBlock("Terrain", heightMapSize,
                terrainScale, heightMap.getHeightMap(), new Vector3f(0, 0, 0));
        gameWorldRoot.attachChild(tb);
        tb.setCullHint(CullHint.Always);

        tb.setLocalTranslation(	-(heightMapSize - 1) * level.getTerrainScale() / 2,  
        						level.getMapMinHeight(), 
        						-(heightMapSize - 1) * level.getTerrainScale() / 2);
        tb.setModelBound(new BoundingBox());
        tb.updateModelBound();	// FIXME: We're not getting terrain collisions because it's not attached to root node.
        
        // TODO: Move this setBounds to a more logical place
        BaseProjectile.setBounds( new Vector3f( -(heightMapSize - 1) * level.getTerrainScale() / 2, 
        										-50, 
        										-(heightMapSize - 1) * level.getTerrainScale() / 2), 
        						  new Vector3f( (heightMapSize - 1) * level.getTerrainScale() / 2, 
        								  		500f, 
        								  		(heightMapSize - 1) * level.getTerrainScale() / 2));
        
	}

	@Deprecated
	private void removePlayableEntity(PlayableEntity entity) {
		entityController.removeTurretAIController(entity);
	}
	
	public void addPlayer(EDPlayer player) {this.thePlayer = player;}
	
	private void spawnTurret(String name, AttributeSet template, Vector3f pos, boolean playable, 
							 String parentFaction, String aiTargetFaction, String missileTargetFaction) {
		// STRUCTURE: Script system should be a controller which updates the view by adding status icons and the model by adding the turret.
		if(template == null) {System.err.println("spawnTurret: No template provided."); return;}
		EDTurret turret = EDFactory.buildTurret(name, template, gameResources, edManager, entityTemplates);
		gameWorldRoot.attachChild(turret.getStructureNode());
		turret.setPosition(pos);
		
		EDTriangleZoneRadar radar = new EDTriangleZoneRadar(turret.getTurretNode(), factionMaps.get(missileTargetFaction));
		turret.addTargetAcquisition(radar);
		edManager.add(turret); edManager.add(radar);
		
		if(playable) {	// TODO: Should prob have a default icon if one is not specified - adding a playable with no icon is not good.
			String[] strData = template.getStringAttribute("statusIcon");
			if((strData != null) && (strData.length > 0)) genericInterface.addStatusIcon(turret, strData[0]);
		}
		
		EntityRegister.getEntityRegister().registerGeometryEntity(turret.getStructureNode(), turret);
		turret.setFaction(parentFaction); addFactionTarget(turret);
		
		entityController.addTurretAIController(turret, factionMaps.get(aiTargetFaction), currentLevel.turretRange);
	}
	
	private void setupRocket(String name, Vector3f position, Quaternion rotation, String faction) {
		rocket = new EDEvacRocket( name, (Node)gameResources.getSpatial("evacrocket") );
		rocket.getRocketNode().setModelBound(new BoundingBox());
		rocket.getRocketNode().updateModelBound();
		edManager.add(rocket);
		
		rocket.getRocketNode().getLocalTranslation().set(position);
		rocket.getRocketNode().getLocalRotation().set(rotation);
		
		rocket.getRocketNode().getChild("Hull").setRenderState(gameResources.getTextureState("rocket"));
		
		if(faction != null) rocket.setFaction(faction); 
		addFactionTarget(rocket);
		gameWorldRoot.attachChild(rocket.getRocketNode());
		rocket.getRocketNode().updateRenderState();
	}
	
	public EDLevel getCurrentLevel() {return currentLevel;}
	/**************** Upgrade Related Methods ***********************/
	/* Applies the current available upgrade to the supplied entity, then clears the available upgrade.*/
	public void applyUpgrade(PlayableEntity entity, EDPlayer player) {
		if((player == null) || (entity == null) || (player.getAvailableUpgradeType() == null)) return;
		
		switch(player.getAvailableUpgradeType()) {
		case REPAIR: 
			if(entity.isDead()) {
				givePrimaryWeapons(entity, entityTemplates.getTemplate("aacannon"), null);
				entity.stripUpgrades();	// Needed to reduce default 2 barrel to single. Maybe think of a better approach such as not giving 2 by default.
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("EntityReactivated", "gameplayEvents", null, null);	// This is just so that objectives can respond to it.
			}
			entity.repair(500.0f);	// FIXME: Repair has a hardcoded hitpoint value.
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("EntityRepaired", "gameplayEvents", null, null);	// This is just so that objectives can respond to it.
			break;
		case DOUBLE: if(!entity.isDead()) {entity.upgradeDouble();} break;
		case LAUNCHER: 
			if(!entity.isDead()) {
				giveSecondaryWeapon(entity, entityTemplates.getTemplate("turretlauncher"));
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("ApplyUpgrade", "gameplayEvents", null, null);	// This is just so that objectives can respond to it.
			}
			break;
		case LASER: 
			if(!entity.isDead()) {
				givePrimaryWeapons(entity, entityTemplates.getTemplate("laser"), null);
			} 
			break;
		case PLASMA: 
			if(!entity.isDead()) {
				givePrimaryWeapons(entity, entityTemplates.getTemplate("plasma"), null);
			} 
			break;
		}
		player.applyCurrentUpgrade();
	}
	
	private void givePrimaryWeapons(PlayableEntity entity, AttributeSet barrel1Template, AttributeSet barrel2Template ) {
		entity.setPrimaryGun(barrel1Template, barrel2Template, gameResources, edManager);
	}
	
	private void giveSecondaryWeapon(PlayableEntity entity, AttributeSet template) {
		entity.setSecondaryGun(template, gameResources, edManager);
	}

	/*************** Upgrade Related Methods End ********************/
	
    /* Just a helper method for the scripted scenery spawn - remove once scripted template spawns are in*/
    private void spawnDamageableScenery(String name, 
			  String template,
			  Vector3f position, 
			  Vector3f scale,
			  Quaternion rotation,
			  String faction) {
    	String[] factions = new String[] {faction};
    	String[] flags = new String[] {"scenery", "damageable"};
    	spawnSceneObject(name, template, position, scale, rotation, factions, flags);
    }
    
    protected void spawnSceneObject(String name, 
    							  String template,
    							  Vector3f position, 
    							  Vector3f scale,
    							  Quaternion rotation,
    							  String[] factions,
    							  String[] flags) {
    	if(template == null) return;
    	if(name == null) name = template;

    	if(GROMEObjectInstance.flagPresent("particles", flags)) { // FIXME: Temp implementation only
    		Node n = new Node(name); gameWorldRoot.attachChild(n); 
    		n.getLocalTranslation().set(position); 
    		if(rotation != null) n.getLocalRotation().set(rotation); 
    		if(scale != null) n.getLocalScale().set(scale); 
    		n.updateWorldVectors();
    		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("addDamageSmoke",  "visualEffects", null, n);
    	} else if( GROMEObjectInstance.flagPresent("scenery", flags) ) {
    		if(sceneryTemplates.getTemplate(template) == null) System.err.println("Could not find template for: " + template);
    		Spatial s = EDFactory.buildSceneryInstance(name, sceneryTemplates.getTemplate(template), levelResources);
    		if(s == null) 
    			System.err.println("Error: Could not build scene object from template: " + template);
    		if(s != null) {
    			gameWorldRoot.attachChild(s);
                s.setLocalTranslation(position);
                if(rotation != null) s.setLocalRotation(rotation);
                if(scale != null) s.setLocalScale(scale);
                s.updateWorldVectors();
                s.updateRenderState();
                scenerySpatials.add(s);

               if(GROMEObjectInstance.flagPresent("damageable", flags)) {
            	   EDDamageableScenery d = new EDDamageableScenery(name, s, 10f);
            	   if( (factions != null) && (factions.length > 0) ) {d.setFaction(factions[0]); addFactionTarget(d);}
               }
    		}
    	}
    }
    
    protected void spawnMOB(String name, 
			  String template,
			  Vector3f position, 
			  Vector3f scale,
			  Quaternion rotation,
			  String[] factions,
			  String[] flags) {
    	if(template == null) return;
    	if(name == null) name = template;
    	
    	boolean playable = GROMEObjectInstance.flagPresent("playable", flags);
    	// Migrate this all to EDFactory so we can just pass in the template without needing to differentiate.
    	if( template.equalsIgnoreCase("fixedturret") && (factions != null) && (factions.length > 2) ) {	// FIXME: Hardcoded templates for active/playable stuff	
    		spawnTurret(name, entityTemplates.getTemplate(template), position, playable, factions[0], factions[1], factions[2]);
    	} else if( GROMEObjectInstance.flagPresent("EDEvacRocket", flags) ) {
    		String f = null;
    		if( (factions != null) && (factions.length > 0) ) f = factions[0];
    		setupRocket(name, position, rotation, f);
    	} else if( (factions != null) && (factions.length > 1) ) {
    		spawnAircraft(name, entityTemplates.getTemplate(template), position, playable, factions[0], factions[1] );
    	}
    }
    	
    /**************** EX-EDEventProcessor stuff starts **************/
    
    private GameEventListener defaultListener = null;
	
	private TurretUpgrade[] fighterUpgrades = {TurretUpgrade.REPAIR, TurretUpgrade.REPAIR, TurretUpgrade.LAUNCHER, TurretUpgrade.PLASMA};
	private TurretUpgrade[] gunshipUpgrades = {TurretUpgrade.REPAIR, TurretUpgrade.REPAIR, TurretUpgrade.REPAIR, TurretUpgrade.REPAIR};

	public void addFactionTarget(GameEntity target) {
		EDEntityMap fMap = factionMaps.get(target.getFaction());
		if(fMap != null) fMap.addEntity(target); else System.out.println("[Add] No faction map found for " + target.getFaction());
	}
	
	public void removeFactionTarget(GameEntity target) {
		EDEntityMap fMap = factionMaps.get(target.getFaction());
		if(fMap != null) fMap.removeEntity(target); else System.out.println("[Remove] No faction map found for " + target.getFaction());
	}
	
	public void processEntityEvents(float interpolation) {
		GameEvent gameEvent = defaultListener.getNextEvent();
		while(gameEvent != null) {
			handleEntityEvent(gameEvent, interpolation);
			gameEvent = defaultListener.getNextEvent();
		}
	}
	
	private void handleEntityEvent(GameEvent e, float interpolation) {
		
		if(e.getEventType().equals("BeamHit")) {
			BaseBeamWeapon l = (BaseBeamWeapon)e.getEventInitiator();
			GameEvent action = new GameEvent("damage", null, new Float(l.getDamagePerSecond() * interpolation));
			Geometry tg = (Geometry)e.getEventTarget();		// TODO: Should be in a try..catch and handle error.
			EntityRegister.getEntityRegister().actUpon(tg, action);
		} else if (e.getEventType().equals("ProjectileHit")) {
			projectileHit(e);
		} else if (e.getEventType().equals("PointsMissileHit")) {
			projectileHit(e);
			BaseProjectile p = (BaseProjectile)e.getEventInitiator();
			if(p != null) {
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("GroundBlast", "visualEffects", p.getWorldTranslation(), null);
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("MissileGroundDetonation", "audio", p.getWorldTranslation(), null);
			}
			removeBaddieMissile(e);
		} else if (e.getEventType().equals("SmallMissileHit")) {
			projectileHit(e);
			BaseProjectile projectile = (BaseProjectile)e.getEventInitiator();
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("Explosion", "visualEffects", projectile.getWorldTranslation(), null);
		} else if(e.getEventType().equals("RemoveBaddieTarget")) {
			removeBaddieMissile(e);
		} else if(e.getEventType().equals("MissileDetonation")) {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(e, "visualEffects");
		} else if(e.getEventType().equals("MissileDestroyed")) {
			MissileProjectile mp = (MissileProjectile)e.getEventInitiator();
			if(mp != null) {
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("Explosion", "visualEffects", mp.getWorldTranslation(), null);
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("MissileDestroyed", "audio", mp.getWorldTranslation(), null);
			}
		} else if(e.getEventType().equals("PointsMissileDestroyed")) {
			EDSeekingMissile m = (EDSeekingMissile)e.getEventInitiator();
			if(m != null) {
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("Explosion", "visualEffects", m.getPosition(), null);
				removeFactionTarget(m);
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("MissileDestroyed", "audio", m.getPosition(), null);
			}
		} else if(e.getEventType().equals("RocketDestroyed")) {
			GameEntity rocket = (GameEntity)e.getEventInitiator();
			if(e != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("HugeExplosion", "visualEffects", rocket.getPosition(), null);
		} else if(e.getEventType().equals("EDGunshipDestroyed")) {
			EDCombatAircraft t = (EDCombatAircraft)e.getEventInitiator();
			if( (t != null) ) {
				TurretUpgrade upgrade = gunshipUpgrades[rand.nextInt( gunshipUpgrades.length)];
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("AddUpgrade","gameplayEvents", upgrade, null);
				handleAircraftDestroyed(t);
			}
		} else if(e.getEventType().equals("EDAlienFighterDestroyed")) {
			EDCombatAircraft t = (EDCombatAircraft)e.getEventInitiator();
			if( (t != null) ) {
				TurretUpgrade upgrade = fighterUpgrades[rand.nextInt( fighterUpgrades.length)];
				GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("AddUpgrade","gameplayEvents", upgrade, null);
				handleAircraftDestroyed(t);
			}
			
		} else if(e.getEventType().equals("EDAircraftDestroyed")) {
			handleAircraftDestroyed((EDCombatAircraft)e.getEventInitiator());			
		} else if(e.getEventType().equals("Explosion")) {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(e, "visualEffects");
		}
		
	}

	public void handleAircraftDestroyed(EDCombatAircraft t) {
		if( (t != null) ) {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("Explosion", "visualEffects", t.getPosition(), null);
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("MissileDestroyed", "audio", t.getPosition(), null);
			removeFactionTarget(t);
		}
	}
	
	public void removeBaddieMissile(GameEvent e) {
		GameEntity baddie = (GameEntity)e.getEventInitiator();
		removeFactionTarget(baddie);
	}
	
	private void spawnMissile(String parentFaction, String targetFaction) {
		// TODO: This will need completely reworking
		// TODO: Remove the constants
		GameEntity target = factionMaps.get(targetFaction).getRandomEntity();
		if(target != null) {
			Vector3f spawnPoint = currentMap.getRandomEdgeLocation(20.0f);
			SimpleTracking simpleTracker = new SimpleTracking(40f);
			Quaternion q = new Quaternion();
			EDSeekingMissile incomingMissile = new EDSeekingMissile("Seeker", spawnPoint, q, target,
																	simpleTracker, 0.0f, 30.0f, 150.0f, 0);
			incomingMissile.setMissileSize(4.0f);
			incomingMissile.setHitEventType("PointsMissileHit");
			incomingMissile.setDestroyedEventType("PointsMissileDestroyed");
			incomingMissile.setDeactivatingEventType("RemoveBaddieTarget");
			ManagedParticleEffect particleTrail = GameWorldInfo.getGameWorldInfo().getParticleManager().getManagedParticleEffect
				("MissileThrusterParticles", new Vector3f(0, 0, -3.0f), 0.0f, "rocket-slowc-80.jme", null);
			//edManager.add(particleTrail);
			//particleTrail.setLocalTranslation(0, 0, -3.0f);
			//particleTrail.setWorldOrLocalSpace(0, false);
			incomingMissile.addParticleTrail(particleTrail);
			particleTrail.getParticleNode().updateGeometricState(0, true);	// Ideally get rid of this as it is done on creation, but the parent node is not passed in.
			incomingMissile.setFaction(parentFaction); addFactionTarget(incomingMissile);
			EntityRegister.getEntityRegister().registerGeometryEntity(incomingMissile.getProjectileNode(), incomingMissile); // Inconsistent - rocket and aircraft do this themselves.
			edManager.add(incomingMissile);
		}

	}
	
	/* Arguments: number to spawn, parent faction, target faction*/
	public void spawnMissiles(String[] args) {
		if( (args == null) || (args.length < 3) ) return;
		int n = Integer.parseInt(args[0]);
		for(int i = 0; i < n; i++) {
			spawnMissile(args[1], args[2]);
		}
	}
	
	public void spawnDamageableScenery(String[] args) {
		if(args.length < 12) return;
		Vector3f loc = null, scale = null;
		Quaternion rot = new Quaternion();

		try {			
			loc = new Vector3f(Float.parseFloat(args[3]), Float.parseFloat(args[4]), Float.parseFloat(args[5]));
			rot.fromAngles(Float.parseFloat(args[6]), Float.parseFloat(args[7]), Float.parseFloat(args[8]));
			scale = new Vector3f(Float.parseFloat(args[9]), Float.parseFloat(args[10]), Float.parseFloat(args[11]));
		} catch (NumberFormatException e) {System.err.println("Error: Could not spawn scenery - specification error.");}
		
		spawnDamageableScenery(args[0], args[1], loc, scale, rot, args[2]);
	}
	
	public void applyDamage(String entityFaction, String entityName, float damageAmount) {
		if( (entityName == null) || (entityFaction == null)) return;
		EDEntityMap map = factionMaps.get(entityFaction);
		if(map == null) return;
		GameEntity target = map.getNamedEntity(entityName);
		if(target == null) return;
		GameEvent action = new GameEvent("damage", null, damageAmount );
		target.handleAction(action);
	}
	
	private void projectileHit(GameEvent e) {
		Geometry tg = (Geometry)e.getEventTarget();		// TODO: Should be in a try..catch and handle error.
		BaseProjectile projectile = (BaseProjectile)e.getEventInitiator();
		GameEvent action = new GameEvent("damage", null, projectile.getDamage() );
		EntityRegister.getEntityRegister().actUpon(tg, action);
	}

	public void spawnAircraft(String[] args) {	// Might be good to make this deal with missing factions.
		if(args.length >= 4) spawnAircraft(args[0], args[1], null, false, args[2], args[3]);
	}

	public void spawnPlayableAircraft(String[] args) {
		if(args.length < 4) return;
		Vector3f loc = null;
		if(args.length >= 7) {loc = new Vector3f(Float.parseFloat(args[4]), Float.parseFloat(args[5]), Float.parseFloat(args[6]));}

		if(args.length >= 4) spawnAircraft(args[0], args[1], loc, true, args[2], args[3]);
	}
	
	public void spawnAircraft(String name, String templateLabel, Vector3f pos, boolean playable, String parentFaction, String targetFaction) {
		if(pos == null) pos = currentMap.getRandomEdgeLocation(30.0f);
		spawnAircraft(name, entityTemplates.getTemplate(templateLabel), pos, playable, parentFaction, targetFaction);
	}
	
	private EDCombatAircraft spawnAircraft(String name, AttributeSet template, Vector3f pos, boolean playable, String parentFaction, String targetFaction) {

		if(template == null) {System.err.println("spawnAircraft: No template provided."); return null;}
		
		EDCombatAircraft aircraft = EDFactory.buildAircraft(name, 
				template, gameResources, edManager, entityTemplates);
		
		if(aircraft == null) {System.err.println("spawnAircraft: Error creating aircraft."); return null;}
		aircraft.setFaction(parentFaction); addFactionTarget(aircraft);
		
		EntityRegister.getEntityRegister().registerGeometryEntity(aircraft.getAircraftNode(), aircraft);
		
		GameWorldInfo.getGameWorldInfo().getRootNode().attachChild(aircraft.getAircraftNode());
		aircraft.getAircraftNode().setLocalTranslation(pos);
		aircraft.setStickThrottle(1.0f);

		edManager.add(aircraft);
		
		EDTriangleZoneRadar radar = new EDTriangleZoneRadar(aircraft.getAircraftNode(), factionMaps.get(targetFaction));
		edManager.add(radar);
		aircraft.setTargetting(radar);
		
		String[] s = template.getStringAttribute("aircraftType");
		boolean isHelicopter = false; if( (s != null) && (s.length > 0) && (s[0].equalsIgnoreCase("helicopter"))) isHelicopter = true; 
		entityController.addAircraftAIController(aircraft, isHelicopter, currentMap, factionMaps.get(targetFaction));
		
		if(playable) {
			s = template.getStringAttribute("statusIcon"); // TODO: Should prob have a default icon if one is not specified - adding a playable with no icon is not good.	
			if((s != null) && (s.length > 0)) genericInterface.addStatusIcon(aircraft, s[0]);
		}	
		
		aircraft.getAircraftNode().updateRenderState();
		return aircraft;
	}
		
	public void printTargetArrays() {
		if(factionMaps != null) {
			for(EDEntityMap map : factionMaps.values()) map.listEntities();
		}
	}
	
    /**************** EX-EDEventProcessor stuff ends **************/
		
	private void cleanupTerrain() {
		tb.removeFromParent(); tb = null;
	}
    
	public void cleanupLevel() {
		if(levelResources != null) levelResources.cleanup(); levelResources = null;
		if(currentLevel != null) currentLevel.cleanup(); currentLevel = null;
		
		if(scenerySpatials != null); scenerySpatials.clear();
		
		//CollisionTreeManager.getInstance().removeCollisionTree(tb);
		CollisionTreeManager.getInstance().removeCollisionTree(gameWorldRoot);
		
		defaultListener.flushEvents();
		
		if(factionMaps != null) {
			for(EDEntityMap map : factionMaps.values()) map.flush();
			factionMaps.clear();
		}
		
		edManager.clearEntities();
		EntityRegister.getEntityRegister().flush();
		
		if(missions != null) {
			for(EDMission mission : missions.values()) mission.cleanup();
			missions.clear();
		}
		
		if(currentMap != null) currentMap.cleanup(); currentMap = null;
		cleanupTerrain();
		
		if(rocket != null) rocket.cleanup(); rocket = null;
		
		// TODO: Scene objects cleanup.
		// TODO: Check that the rocket has removed itself (should go with the removeable object manager).
		// TODO: Check what is attached to the root node.
		
		gameWorldRoot.updateGeometricState(0, true);
		//DebugMethods.listNodeChildren(gameWorldRoot);
		//DebugMethods.listNodeChildren(menuRoot);
		//edManager.printEntityList();
		
		gameWorldRoot.detachAllChildren();
	}
	
	public void cleanup() {
		entityController = null;
		
		if(scenerySpatials != null) scenerySpatials.clear(); scenerySpatials = null;
		
		if(defaultListener != null) defaultListener.flushEvents();
		
		if(thePlayer != null) thePlayer.cleanup(); thePlayer = null;
		if(factionMaps != null) {
			for(EDEntityMap map : factionMaps.values()) map.flush();
			factionMaps.clear();
			factionMaps = null;
		}
		if(missions != null) {
			for(EDMission mission : missions.values()) mission.cleanup();
			missions.clear();
		}
		missions = null;
		
		if(edManager != null) edManager.cleanup(); edManager = null;
		
		if(EntityRegister.getEntityRegister() != null) EntityRegister.getEntityRegister().cleanup();
		
		rand = null;
		if(currentMap != null) currentMap.cleanup(); currentMap = null;
		genericInterface = null;
		
		tb = null;
		if(rocket != null) rocket.cleanup(); rocket = null;
		
		if(entityTemplates != null) entityTemplates.cleanup(); entityTemplates = null;
		if(sceneryTemplates != null) sceneryTemplates.cleanup(); sceneryTemplates = null;
		
		GameWorldInfo.getGameWorldInfo().getEventHandler().removeListener("default"); defaultListener = null;
		GameWorldInfo.getGameWorldInfo().getEventHandler().removeListener("gameplayEvents"); gameplayListener = null;
		if(levelResources != null) levelResources.cleanup(); levelResources = null;
		if(gameResources != null) gameResources.cleanup(); gameResources = null;
		if(currentLevel != null) currentLevel.cleanup(); currentLevel = null;
		
		gameWorldRoot.removeFromParent(); gameWorldRoot = null;
	}
	

}
