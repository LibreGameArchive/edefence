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


import com.calefay.effects.ManagedParticleEffect;
import com.calefay.exodusDefence.PlayableEntity.ControlsType;
import com.calefay.exodusDefence.entities.EDCombatAircraft;
import com.calefay.exodusDefence.entities.EDTurret;
import com.calefay.exodusDefence.radar.TargetAcquirer;
import com.calefay.exodusDefence.weapons.EDBeamGun;
import com.calefay.exodusDefence.weapons.EDBeamWeapon;
import com.calefay.exodusDefence.weapons.EDMissileLauncher;
import com.calefay.exodusDefence.weapons.EDProjectileGun;
import com.calefay.exodusDefence.weapons.Gun;
import com.calefay.utils.AttributeSet;
import com.calefay.utils.GameEntity;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.jme.animation.SpatialTransformer;
import com.jme.bounding.BoundingBox;
import com.jme.bounding.OrientedBoundingBox;
import com.jme.image.Texture;
import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.SharedMesh;
import com.jme.scene.Spatial;
import com.jme.scene.TriMesh;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;


public class EDFactory {
	
	public static Spatial buildSceneryInstance(String name, AttributeSet template, GameResourcePack resources) {
		if( (template == null) || (resources == null) ) return null;
		
		String[] s = template.getStringAttribute("templateType");
		if( (s == null) || (s.length < 1) || (!s[0].equals("scenery")) ) return null;
		
		// TODO: Consider whether TriMesh is optimal as often there may only be one instance.
		TriMesh tm = extractTriMesh("mesh", template, resources, 0);

		if(tm != null) {
			SharedMesh mesh = new SharedMesh(name, tm);
			mesh.setModelBound(new OrientedBoundingBox());
			mesh.updateModelBound();
			
			s = template.getStringAttribute("texture");
			if( (s != null) && (s.length > 0) ) {
				TextureState ts = resources.getTextureState(s[0]);
				if(ts != null) mesh.setRenderState(ts);
				
				if(template.hasAttribute("alpha")) {
					RenderState as = resources.getRenderState("alphakill");
					if(as != null) mesh.setRenderState(as); else System.err.println("Error: buildSceneryInstance - RenderStates not created.");
				}
				
				mesh.updateRenderState();
			}
    		return mesh;
		}
		
		return null;
	}
	
	public static Gun buildGun(String name, Node parent, GameEntity parentenEntity, AttributeSet template,
			GameResourcePack resources, RemoveableEntityManager entityManager,
			TargetAcquirer targeter) {
		if(template == null) return null;
		String[] s = template.getStringAttribute("templateType");
		if( (s == null) || (s.length < 1) || (!s[0].equals("weapon")) ) return null; 
		
		s = template.getStringAttribute("weaponType");
		if( (s == null) || (s.length < 1) ) return null;
		
		Gun g = null;
		
		if(s[0].equals("projectile")) {
			g = buildProjectileGun(name, parent, template, entityManager);
		}
		if(s[0].equals("launcher")) {
			g = buildMissilePod(name, parent, parentenEntity, template, entityManager, targeter);
		}
		if(s[0].equals("beam")) {
			g = buildBeamGun(name, parent, template, resources, entityManager);
		}
		return g;
	}
	
	/* Creates and returns a beam gun based on the supplied template. Adds it to the entity manager before returning.*/
	private static EDBeamGun buildBeamGun(String name, Node parent, AttributeSet template, 
			GameResourcePack resources, RemoveableEntityManager entityManager) {
		if(template == null) return null;
		
		float damage = 0; float maxTemp = 0; float beamWidth = 0.1f;
		float[] f = null; String[] s = null;
		
		f = template.getFloatAttribute("damagePerSecond"); if(f != null) damage = f[0];
		f = template.getFloatAttribute("maxTemperature"); if(f != null) maxTemp = f[0];
		f = template.getFloatAttribute("beamWidth"); if(f != null) beamWidth = f[0];
		
		Texture beamTx = null; Texture flashTx = null;
		if( (resources != null) ) {
			s = template.getStringAttribute("beamTexture");
			if( (s != null) && (s.length > 0) ) beamTx = resources.getTexture(s[0]);
			s = template.getStringAttribute("flashTexture");
			if( (s != null) && (s.length > 0) ) flashTx = resources.getTexture(s[0]);
		}

		EDBeamWeapon bw = new EDBeamWeapon( name + "Weapon", parent, damage, beamTx, flashTx);
		EDBeamGun bg = new EDBeamGun( name + "Gun", bw, maxTemp, parent);		
		bg.setBeamWidth(beamWidth);

		s = template.getStringAttribute("fireEvent");
		if( (s != null) && (s.length > 0)) {bg.setFireEventType(s[0]);}
		
		s = template.getStringAttribute("hitEffect");
		if( (s != null) && (s.length > 0)) {
			ManagedParticleEffect hitEffect = GameWorldInfo.getGameWorldInfo().getParticleManager().getManagedParticleEffect("hitEffect", null, 0f, s[0], null);
			//entityManager.add(hitEffect);
			bw.addHitEffect(hitEffect);
		}

		entityManager.add(bg);
		
		return bg;
	}
	
	/* Creates and returns a projectile gun based on the supplied template. Adds it to the entity manager before returning.*/
	private static EDProjectileGun buildProjectileGun(String name, Node parent, 
			AttributeSet template, RemoveableEntityManager entityManager) {
		if(template == null) return null;
		
		float damage = 0; float reload = 1f; float burstLength = 1; float tracerFrequency = 1;
		float[] f = null;
		
		f = template.getFloatAttribute("damagePerRound"); if( (f != null) && (f.length > 0) ) damage = f[0];
		f = template.getFloatAttribute("reloadTime"); if( (f != null)  && (f.length > 0)) reload = f[0];
		f = template.getFloatAttribute("burstLength"); if( (f != null)  && (f.length > 0)) burstLength = f[0];
		f = template.getFloatAttribute("tracerFrequency"); if( (f != null)  && (f.length > 0)) tracerFrequency = f[0];
		
		EDProjectileGun gun = new EDProjectileGun( name + "Gun", reload, damage, parent, entityManager);
		gun.setGunStats(reload, damage, (int)burstLength, (int)tracerFrequency);

		entityManager.add(gun);
		
		return gun;
	}
	
	/* Creates and returns a missile pod based on the supplied template. Adds it to the entity manager before returning.*/
	private static EDMissileLauncher buildMissilePod(String name, Node parent, GameEntity parentEntity,
			AttributeSet template, RemoveableEntityManager entityManager,
			TargetAcquirer targeter) {
		if(template == null) return null;
		
		float damage = 0; float fireDelay = 1f;
		float[] f = null;
		
		f = template.getFloatAttribute("damagePerRound"); if( (f != null) && (f.length > 0) ) damage = f[0];
		f = template.getFloatAttribute("fireDelay"); if( (f != null)  && (f.length > 0)) fireDelay = f[0];
		
		/* TODO: The way positions are stored is nasty and this is a nasty way of setting them up.
		   Might be better to have a small internal class for each type of weapon with it's own data structures so we can use fully complex blocks for data not just attributesets.
		   Also allow to parse out the data in advance and not have to instantiate stuff like this for every call, just once per template.*/
		Vector3f[] hardpoints = null;
		f = template.getFloatAttribute("hardpointPositions"); if( (f != null)  && (f.length >= 3)) {
			hardpoints = new Vector3f[f.length / 3];
			for(int i = 0; (i + 2) < f.length; i += 3) {
				hardpoints[i / 3] = new Vector3f( f[i], f[i+1], f[i+2]);
			}
		}
		// TODO: Make templates for the missiles themselves - currently their stats are hardcoded in the launcher class.
		EDMissileLauncher launcher = new EDMissileLauncher("GunshipLauncher1", parentEntity, fireDelay, damage, parent, entityManager, hardpoints);
		
		launcher.setTargetting(targeter);
		launcher.reload();
		entityManager.add(launcher);
		
		return launcher;
	}
	
	public static EDTurret buildTurret(String name,
			 AttributeSet template,
			 GameResourcePack resources,
			 RemoveableEntityManager entityManager,
			 EDTemplateSet weaponTemplates) {
		if(template == null) {System.err.println("Error: Could not spawn turret, no template provided."); return null;}
		
		EDTurret turret = new EDTurret(name);
		float[] f = null; String[] s = null;
		
		float hitPoints = 1.0f;
		f = template.getFloatAttribute("hitPoints"); if( (f != null) && (f.length > 0) ) hitPoints = f[0];
		turret.setStats(hitPoints);
		
		s = template.getStringAttribute("primaryWeapon");
		AttributeSet weapon1 = null, weapon2 = null;
		if( (s != null) && (s.length > 0) ) {weapon1 = weaponTemplates.getTemplate(s[0]);}
		if( (s != null) && (s.length > 1) ) {weapon2 = weaponTemplates.getTemplate(s[1]);}
		Vector3f[] firePoints = null;
		f = template.getFloatAttribute("firePoints"); if( (f != null)  && (f.length >= 3)) {
			firePoints = new Vector3f[f.length / 3];
			for(int i = 0; (i + 2) < f.length; i += 3) {
				firePoints[i / 3] = new Vector3f( f[i], f[i+1], f[i+2]);
			}
			turret.setFirepoints(firePoints);
		}
		turret.setPrimaryGun(weapon1, weapon2, resources, entityManager);
		
		/*** Visuals section begins ***/
		TriMesh masterMesh = extractTriMesh("structureMesh", template, resources, 0);
		if(masterMesh != null) {
			SharedMesh structureMesh = new SharedMesh(name + "StructureMesh", masterMesh);
			turret.getStructureNode().attachChild(structureMesh);
			turret.getStructureNode().setModelBound(new BoundingBox()); turret.getStructureNode().updateModelBound();
		}
		masterMesh = extractTriMesh("turretMesh", template, resources, 0);
		if(masterMesh != null) {
			SharedMesh turretMesh = new SharedMesh(name + "TurretMesh", masterMesh);
			turret.getTurretNode().attachChild(turretMesh);
			turret.getTurretNode().setModelBound(new BoundingBox()); turret.getTurretNode().updateModelBound();
		}
		masterMesh = extractTriMesh("mountingMesh", template, resources, 0);
		if(masterMesh != null) {
			SharedMesh mountingMesh = new SharedMesh(name + "MountingMesh", masterMesh);
			turret.getMountingNode().attachChild(mountingMesh);
			turret.getMountingNode().setModelBound(new BoundingBox()); turret.getMountingNode().updateModelBound();
		}
		masterMesh = extractTriMesh("barrelMesh", template, resources, 0);
		if(masterMesh != null) {
			SharedMesh barrel1Mesh = new SharedMesh(name + "Barrel1Mesh", masterMesh);
			SharedMesh barrel2Mesh = new SharedMesh(name + "Barrel2Mesh", masterMesh);
			turret.getBarrelNode1().attachChild(barrel1Mesh); turret.getBarrelNode2().attachChild(barrel2Mesh);
			turret.getBarrelNode1().setModelBound(new BoundingBox()); turret.getBarrelNode1().updateModelBound();
			turret.getBarrelNode2().setModelBound(new BoundingBox()); turret.getBarrelNode2().updateModelBound();
		}
		
		turret.getTurretNode().getLocalTranslation().set(new Vector3f(0, 3.3f, 0));
		turret.getMountingNode().attachChild(turret.getBarrelNode1());
		turret.getMountingNode().getLocalTranslation().set(new Vector3f(0, 0.75f, 0.8f));
		turret.getTurretNode().attachChild(turret.getMountingNode());
		turret.getBarrelNode1().getLocalTranslation().set(new Vector3f(0.0f, 0.0f, 0.7f));
		turret.getStructureNode().attachChild(turret.getTurretNode());
		turret.getStructureNode().updateWorldVectors();
		turret.getStructureNode().updateWorldBound();
		
		s = template.getStringAttribute("texture");
		if( (s != null) && (s.length > 0) ) {
			TextureState ts = resources.getTextureState(s[0]);
			if(ts != null) turret.getStructureNode().setRenderState(ts);
			turret.getStructureNode().updateRenderState();
		}
		/*** Visuals section ends ***/
		
		return turret;
	}
	
	public static EDCombatAircraft buildAircraft(String name,
			 AttributeSet template,
			 GameResourcePack resources,
			 RemoveableEntityManager entityManager,
			 EDTemplateSet weaponTemplates) {
		EDCombatAircraft aircraft = null;
		
		if(template == null) {System.err.println("Error: Could not spawn aircraft, no template provided."); return null;}
		
		String[] s = template.getStringAttribute("templateType");
		if( (s == null) || (s.length < 1) || (!s[0].equals("aircraft")) ) {System.err.println("Error: Could not spawn aircraft, template type was not aircraft."); return null; }
		
		s = template.getStringAttribute("aircraftType");
		if( (s == null) || (s.length < 1) ) {System.err.println("Error: Could not spawn aircraft, no aircraft type provided."); return null; }
		
		if(s[0].equals("fixedwing")) {
			aircraft = buildFixedWingAircraft(name, template, resources, entityManager, weaponTemplates);
		}
		if(s[0].equals("helicopter")) {
			aircraft = buildHelicopter(name, template, resources, entityManager, weaponTemplates);
		}
		
		return aircraft;
	}
	
	private static EDCombatAircraft buildFixedWingAircraft(String name,
			 AttributeSet template,
			 GameResourcePack resources,
			 RemoveableEntityManager entityManager,
			 EDTemplateSet weaponTemplates) {
		EDCombatAircraft aircraft = setupAircraft(name, template, resources, entityManager, weaponTemplates);
				
		TriMesh mesh = extractTriMesh("hullMesh", template, resources, 0);
		if(mesh != null) {
			aircraft.getAircraftNode().attachChild(new SharedMesh(name + "HullMesh", mesh));
			aircraft.getAircraftNode().setModelBound(new OrientedBoundingBox());
			aircraft.getAircraftNode().updateModelBound();
		}
		
		String[] s = template.getStringAttribute("texture");
		if( (s != null) && (s.length > 0) ) {
			TextureState ts = resources.getTextureState(s[0]);
			if(ts != null) aircraft.getAircraftNode().setRenderState(ts);
		}
		
		return aircraft;
	}
	
	private static EDCombatAircraft buildHelicopter(String name,
			 AttributeSet template,
			 GameResourcePack resources,
			 RemoveableEntityManager entityManager,
			 EDTemplateSet weaponTemplates) {
		EDCombatAircraft aircraft = setupAircraft(name, template, resources, entityManager, weaponTemplates);
		aircraft.setThrustVector(Vector3f.UNIT_Y);
		aircraft.setControlsType(ControlsType.HELICOPTER);

		float[] f = null; String[] s = null;
		
		s = template.getStringAttribute("texture");
		TextureState hullTS = null, canopyTS = null, rotorTS = null;
		if( (s != null) && (s.length > 0) ) {hullTS = resources.getTextureState(s[0]);}
		if( (s != null) && (s.length > 1) ) {rotorTS = resources.getTextureState(s[1]);}
		if( (s != null) && (s.length > 2) ) {canopyTS = resources.getTextureState(s[2]);}
		
		SharedMesh hullMesh = null, canopyMesh = null, mainRotorMesh = null, tailRotorMesh = null;		
		TriMesh mesh = extractTriMesh("hullMesh", template, resources, "hull");
		if(mesh != null) {
			hullMesh = new SharedMesh(name + "HullMesh", mesh);
			if(hullTS != null) hullMesh.setRenderState(hullTS);
			aircraft.getAircraftNode().attachChild(hullMesh);
		}
		mesh = extractTriMesh("canopyMesh", template, resources, "canopy");	// FIXME: Should be saved on it's own as an optional TriMesh so no child 1 hardcoded nonsense.	
		if(mesh != null) {
			canopyMesh = new SharedMesh(name + "CanopyMesh", mesh);
			if(canopyTS != null) canopyMesh.setRenderState(canopyTS);
			
			/************************* UGLY TEMPORARY APACHE STUFF!!! ******************************/
			resources.buildPresetRenderStates(DisplaySystem.getDisplaySystem().getRenderer());
			RenderState as = resources.getRenderState("alphablend");
			if(as != null) canopyMesh.setRenderState(as); canopyMesh.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT); canopyMesh.updateRenderState();
			/************************* UGLY TEMPORARY APACHE STUFF ENDS!!! ******************************/
			
			aircraft.getAircraftNode().attachChild(canopyMesh);
		}
		mesh = extractTriMesh("mainRotorMesh", template, resources, 0);
		if(mesh != null) {
			mainRotorMesh = new SharedMesh(name + "MainRotorMesh", mesh);
			if(rotorTS != null) mainRotorMesh.setRenderState(rotorTS);
			aircraft.getAircraftNode().attachChild(mainRotorMesh);
			Vector3f mainRotorTrans = null;
			f = template.getFloatAttribute("mainRotorTrans"); 
			if( (f != null)  && (f.length >= 3)) {mainRotorTrans = new Vector3f( f[0], f[1], f[2]);}
			if(mainRotorTrans != null) mainRotorMesh.setLocalTranslation(mainRotorTrans);
			
			/************************* UGLY TEMPORARY APACHE STUFF!!! ******************************/
			RenderState as = resources.getRenderState("alphablend");
			if(as != null) mainRotorMesh.setRenderState(as); mainRotorMesh.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT); mainRotorMesh.updateRenderState();
			/************************* UGLY TEMPORARY APACHE STUFF ENDS!!! ******************************/
			
			addSpinController(mainRotorMesh, Vector3f.UNIT_Y, 5.0f);
		}
		mesh = extractTriMesh("tailRotorMesh", template, resources, 0);
		if(mesh != null) {
			tailRotorMesh = new SharedMesh(name + "TailRotorMesh", mesh);
			if(rotorTS != null) tailRotorMesh.setRenderState(rotorTS);
			aircraft.getAircraftNode().attachChild(tailRotorMesh);
			Vector3f tailRotorTrans = null;
			f = template.getFloatAttribute("tailRotorTrans"); 
			if( (f != null)  && (f.length >= 3)) {tailRotorTrans = new Vector3f( f[0], f[1], f[2]);}
			if(tailRotorTrans != null) tailRotorMesh.setLocalTranslation(tailRotorTrans);
			/************************* UGLY TEMPORARY APACHE STUFF!!! ******************************/
			RenderState as = resources.getRenderState("alphablend");
			if(as != null) tailRotorMesh.setRenderState(as); tailRotorMesh.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT); tailRotorMesh.updateRenderState();
			/************************* UGLY TEMPORARY APACHE STUFF ENDS!!! ******************************/
			
			addSpinController(tailRotorMesh, Vector3f.UNIT_X, 5.0f);
		}
		
		if( (mesh != null) || (mainRotorMesh != null) || (tailRotorMesh != null) ) {
			aircraft.getAircraftNode().setModelBound(new OrientedBoundingBox());
			aircraft.getAircraftNode().updateModelBound();
		}
		
		return aircraft;
	}
	
	// TODO: Maybe not use controllers for this as they seem over complicated.
	private static void addSpinController(Spatial toSpin, Vector3f rotationAxis, float speed) {
        Quaternion q = new Quaternion();
        SpatialTransformer st = new SpatialTransformer(1);
        st.setObject(toSpin, 0, -1);
        q.fromAngleAxis(0, rotationAxis);
        st.setRotation(0, 0, q);
        q.fromAngleAxis(FastMath.HALF_PI, rotationAxis);
        st.setRotation(0, 0.25f, q);
        q.fromAngleAxis(FastMath.PI, rotationAxis);
        st.setRotation(0, 0.5f, q);
        q.fromAngleAxis(FastMath.HALF_PI * 3, rotationAxis);
        st.setRotation(0, 0.75f, q);
        q.fromAngleAxis(FastMath.TWO_PI, rotationAxis);
        st.setRotation(0, 1.0f, q);
        st.setRepeatType(SpatialTransformer.RT_WRAP);
        st.setSpeed(speed);
        st.interpolateMissing();
        toSpin.addController(st);
	}
	
	private static EDCombatAircraft setupAircraft(String name,
												 AttributeSet template,
												 GameResourcePack resources,
												 RemoveableEntityManager entityManager,
												 EDTemplateSet weaponTemplates) {
		
		if(template == null) return null;
		
		float[] f = null; String[] s = null;
		
		float hitPoints = 1.0f;
		float thrust = 0; float lift = 0; float weight = 0;
		float rollRate = 0; float pitchRate = 0; float yawRate = 0;
		
		EDCombatAircraft aircraft = new EDCombatAircraft(name);
		
		f = template.getFloatAttribute("thrust"); if( (f != null) && (f.length > 0) ) thrust = f[0];
		f = template.getFloatAttribute("lift"); if( (f != null) && (f.length > 0) ) lift = f[0];
		f = template.getFloatAttribute("weight"); if( (f != null) && (f.length > 0) ) weight = f[0];
		
		f = template.getFloatAttribute("rollRate"); if( (f != null) && (f.length > 0) ) rollRate = f[0] * FastMath.DEG_TO_RAD;
		f = template.getFloatAttribute("pitchRate"); if( (f != null) && (f.length > 0) ) pitchRate = f[0] * FastMath.DEG_TO_RAD;
		f = template.getFloatAttribute("yawRate"); if( (f != null) && (f.length > 0) ) yawRate = f[0] * FastMath.DEG_TO_RAD;
		
		f = template.getFloatAttribute("hitPoints"); if( (f != null) && (f.length > 0) ) hitPoints = f[0];
		aircraft.setCharacteristics(thrust, lift, weight, rollRate, pitchRate, yawRate, hitPoints);
		
		s = template.getStringAttribute("destructionEvent");
		if( (s != null) && (s.length > 0) ) aircraft.setOnDestroyedEventType(s[0]);
		
		s = template.getStringAttribute("primaryWeapon");
		AttributeSet weapon1 = null, weapon2 = null;
		if( (s != null) && (s.length > 0) ) {weapon1 = weaponTemplates.getTemplate(s[0]);}
		if( (s != null) && (s.length > 1) ) {weapon2 = weaponTemplates.getTemplate(s[1]);}
		Vector3f[] firePoints = null;
		f = template.getFloatAttribute("firePoints"); if( (f != null)  && (f.length >= 3)) {
			firePoints = new Vector3f[f.length / 3];
			for(int i = 0; (i + 2) < f.length; i += 3) {
				firePoints[i / 3] = new Vector3f( f[i], f[i+1], f[i+2]);
			}
			aircraft.setFirepoints(firePoints);
		}
		aircraft.setPrimaryGun(weapon1, weapon2, resources, entityManager);

		s = template.getStringAttribute("thrusterEffect");
		if( (s != null) && (s.length > 0) ) {
			Vector3f[] thrusterPositions = null;
			String effectPath = s[0];
			f = template.getFloatAttribute("thrusterPositions"); if( (f != null)  && (f.length >= 3)) {
				thrusterPositions = new Vector3f[f.length / 3];
				for(int i = 0; (i + 2) < f.length; i += 3) {
					thrusterPositions[i / 3] = new Vector3f( f[i], f[i+1], f[i+2]);
				}
				if(thrusterPositions.length > 0) {
					ManagedParticleEffect effect = GameWorldInfo.getGameWorldInfo().getParticleManager().getManagedParticleEffect
						(name + "ThrusterEffect", thrusterPositions[0], 0.0f, effectPath, aircraft.getAircraftNode());
					//entityManager.add(effect);
				}
				if(thrusterPositions.length > 1) {
					ManagedParticleEffect effect = GameWorldInfo.getGameWorldInfo().getParticleManager().getManagedParticleEffect
						(name + "ThrusterEffect", thrusterPositions[1], 0.0f, effectPath, aircraft.getAircraftNode());
					//entityManager.add(effect);
				}
			}
		}		
		
		s = template.getStringAttribute("engineSound");
		if( (s != null) && (s.length > 0) ) {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(s[0], "audio", aircraft, null);
		}
		
		return aircraft;
	}

	// *** Thisis uuuuugly and should be replaced. At minimum get them in .jme format saved so the stored resource is already a TriMesh.
	private static TriMesh extractTriMesh(String propertyLabel, AttributeSet template, GameResourcePack resources, int childNumber) {
		String[] s = template.getStringAttribute(propertyLabel);
		if( (s == null) || (s.length == 0)) return null;
		Spatial sp = resources.getSpatial(s[0]);
		if( (sp == null) || !(sp instanceof Node) ) return null;
		Node n = (Node)sp; 
		if(n.getQuantity() < childNumber) return null;
		sp = n.getChild(childNumber);
		if(!(sp instanceof TriMesh)) return null;
		
		return (TriMesh)sp;
	}
	
	private static TriMesh extractTriMesh(String propertyLabel, AttributeSet template, GameResourcePack resources, String childName) {
		String[] s = template.getStringAttribute(propertyLabel);
		if( (s == null) || (s.length == 0)) return null;
		Spatial sp = resources.getSpatial(s[0]);
		if( (sp == null) || !(sp instanceof Node) ) return null;
		Node n = (Node)sp; 

		sp = n.getChild(childName);
		if(!(sp instanceof TriMesh)) return null;
		
		return (TriMesh)sp;
	}
}
