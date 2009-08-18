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
import java.util.Random;

import com.calefay.exodusDefence.entities.EDCombatAircraft;
import com.calefay.exodusDefence.weapons.EDSeekingMissile;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.GameWorldInfo;
import com.calefay.utils.RemoveableEntityManager;
import com.calefay.utils.GameEventHandler.GameEventListener;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.Node;
import com.jmex.audio.AudioSystem;
import com.jmex.audio.AudioTrack;
import com.jmex.audio.AudioTrack.TrackType;
import com.jmex.audio.MusicTrackQueue.RepeatType;

public class EDAudioEventProcessor {

	private AudioSystem audio;
	private GameEventListener audioListener = null;
	private RemoveableEntityManager audioManager = null;
	private Camera listenerCam = null;
	private AudioTrack music = null;
	
	private GameResourcePack soundResources = null;
	Random rand = null;
	
	/** EDAudioEventProcessor will listen on the event queue "audio" and processes any events. It will create an audio system and play sounds on it in response to events.
	 * It uses it's own independant entitymanager in order to keep audio largely separate from the rest of the game.*/
	public EDAudioEventProcessor(int maxEvents, Camera listenerCam) {
		this.listenerCam = listenerCam;
		
		audio = AudioSystem.getSystem();
		audio.setMasterGain(0.8f);
		audio.setDopplerFactor(1.0f);
		audio.setUnitsPerMeter(1.0f);
		
        audio.getEar().trackOrientation(listenerCam);
        audio.getEar().trackPosition(listenerCam);
        
		audioManager = new RemoveableEntityManager();
		audioListener = GameWorldInfo.getGameWorldInfo().getEventHandler().addListener("audio", 500);

		soundResources = new GameResourcePack();
		soundResources.parseResourcePack("data/scripts/soundresources.txt");
		soundResources.loadPending();

		rand = new Random();
        music = getMusic(soundResources.getSound("TumblingMotion"), true);
        audio.getMusicQueue().setRepeatType(RepeatType.NONE);
        audio.getMusicQueue().setCrossfadeinTime(2.5f);
        audio.getMusicQueue().setCrossfadeoutTime(2.5f);
        audio.getMusicQueue().addTrack(music);
	}
	
	private void processEvents() {
		GameEvent gameEvent = audioListener.getNextEvent();
		while(gameEvent != null) {
			handleEvent(gameEvent);
			gameEvent = audioListener.getNextEvent();
		}
	}
	
	// FIXME : Check all URLs on sound loads or better have sounds loaded and checked by a resource manager.
	private void handleEvent(GameEvent e) {
		if(e.getEventType().equals("FadeInMusic")) {
			Float fadeTime = (Float)e.getEventInitiator();
			Float volume = (Float)e.getEventTarget();
			fadeInMusic(fadeTime, volume);
		} else if(e.getEventType().equals("FadeOutMusic")) {
			Float fadeTime = (Float)e.getEventInitiator();
			fadeOutMusic(fadeTime);
		} else if(e.getEventType().equals("MissileGroundDetonation")) {
			Vector3f emitter = (Vector3f)e.getEventInitiator();
			float distanceSQ = emitter.distanceSquared(listenerCam.getLocation());
			final float playDist = 300; 
			
			if(distanceSQ < (playDist * playDist) ) {
				URL soundURL = null; EDSoundEffect sound = null;
				if(distanceSQ < (playDist * (playDist / 4)) ) {
					soundURL = soundResources.getSound("ExplosionClose");
					sound = new EDSoundEffect("ExplosionSound",soundURL, emitter, listenerCam, playDist, playDist + 10f, 0.6f);
					sound.setPitch( (rand.nextFloat() * 0.5f) + 0.7f);
				} else {
					soundURL = soundResources.getSound("ExplosionFar");
					sound = new EDSoundEffect("ExplosionSound",soundURL, emitter, listenerCam, playDist, playDist + 10f, 0.6f);
					sound.setReferenceDistance(100f);	// FIXME: Hardcoded sound reference distance
				}
				audioManager.add(sound );
			}
		} else if(e.getEventType().equals("MissileDestroyed")) {
			Vector3f emitter = (Vector3f)e.getEventInitiator();
			float distanceSQ = emitter.distanceSquared(listenerCam.getLocation());
			final float playDist = 200; final float playDistSQ = playDist * playDist;
		
			if(distanceSQ < playDistSQ) {
				URL	soundURL = soundResources.getSound("MissileDestroyed");
				EDSoundEffect sound = new EDSoundEffect("MissileDestroyedSound", soundURL, emitter, listenerCam, playDist, playDist + 10, 0.6f);
				sound.setPitch( (rand.nextFloat() * 0.5f) + 1.0f);
				audioManager.add(sound );
			}
		} else if(e.getEventType().equals("PlasmaBoltFire")) {
			Vector3f emitter = (Vector3f)e.getEventInitiator();
			float distanceSQ = emitter.distanceSquared(listenerCam.getLocation());
			final float playDist = 200f; final float playDistSQ = playDist * playDist;
			
			if(distanceSQ < playDistSQ) {
				URL	soundURL = soundResources.getSound("AlienFire");
				EDSoundEffect sound = new EDSoundEffect("AlienFireSound", soundURL, emitter, listenerCam, playDist, playDist + 10f, 0.5f);
				audioManager.add(sound);
			}
		} else if(e.getEventType().equals("EDProjectileGunFire")) {
			Vector3f emitter = (Vector3f)e.getEventInitiator();
			float distanceSQ = emitter.distanceSquared(listenerCam.getLocation());
			final float playDist = 300f; final float playDistSQ = playDist * playDist;
			
			if(distanceSQ < playDistSQ) {
				URL	soundURL = soundResources.getSound("AAGunFire");
				EDSoundEffect sound = new EDSoundEffect("AAGunFireSound", soundURL, emitter, listenerCam, playDist, playDist + 10f, 0.6f);
				audioManager.add(sound);
			}
		} else if(e.getEventType().equals("Jet")) {
			EDCombatAircraft emitter = (EDCombatAircraft)e.getEventInitiator();
			URL	soundURL = soundResources.getSound("Jet");
			
			EDSoundEffect sound = new EDSoundEffect("JetSound", soundURL, emitter.getAircraftNode(), listenerCam, 400, 440, 0.6f);
			sound.linkToEntity(emitter);
			sound.setPitch(1.0f);
			sound.setReferenceDistance(30f);	// Hardcoded reference distance
			sound.setRepeating(true);
			audioManager.add(sound);
		} else if(e.getEventType().equals("Helicopter")) {
			EDCombatAircraft emitter = (EDCombatAircraft)e.getEventInitiator();
			URL	soundURL = soundResources.getSound("Helicopter");
			
			EDSoundEffect sound = new EDSoundEffect("HelicopterSound", soundURL, emitter.getAircraftNode(), listenerCam, 400, 440, 0.8f);
			sound.linkToEntity(emitter);
			sound.setPitch(1.0f);
			sound.setReferenceDistance(20f);	// Hardcoded reference distance
			sound.setRepeating(true);
			audioManager.add(sound);
		} else if(e.getEventType().equals("EDMissile")) {
			EDSeekingMissile emitter = (EDSeekingMissile)e.getEventInitiator();	// TODO: Use new entities here so you can get the location directly aswell.
			URL	soundURL = soundResources.getSound("Missile");
			
			EDSoundEffect sound = new EDSoundEffect("MissileSound", soundURL, emitter.getProjectileNode(), listenerCam, 400, 440, 0.6f);
			sound.linkToEntity(emitter);
			sound.setPitch(0.6f);	
			sound.setReferenceDistance(30f);	// Hardcoded reference distance
			sound.setRepeating(true);
			audioManager.add(sound);
		} else if(e.getEventType().equals("RocketLaunch")) {
			Node emitter = (Node)e.getEventInitiator();
			URL	soundURL = soundResources.getSound("RocketLaunch");
			EDSoundEffect sound = new EDSoundEffect("RocketLaunchSound", soundURL, emitter, listenerCam, 2000, 2200, 1.0f);
			sound.setReferenceDistance(100f);
			audioManager.add(sound);
		}
		
	}
	
    private AudioTrack getMusic(URL resource, boolean stream) {
        AudioTrack sound = AudioSystem.getSystem().createAudioTrack(resource, stream);
        sound.setType(TrackType.MUSIC);
        sound.setRelative(true);
        sound.setVolume(0); sound.setTargetVolume(0.0f);
        sound.setLooping(false);
        return sound;
    }
    
	public void update(float interpolation) {
		//audio.update(interpolation);
		audio.update();
		audioManager.updateEntities(interpolation);
		processEvents();
	}
	
	private void fadeOutMusic(float fadeTime) {
		if(music == null) return;
		music.fadeOut(fadeTime);
		//music.stop();
	}
	
	private void fadeInMusic(float fadeTime, float volume) {
		if(music == null) return;
		if(!music.isPlaying()) {music.play();}
		music.fadeIn(fadeTime, volume);
		//music.stop();
	}
	
	public boolean isEnabled() {return audio != null;}
	
	public void flush() {
		audioManager.clearEntities();
		audioListener.flushEvents();
	}
	
	public void cleanup() {
		flush();
		GameWorldInfo.getGameWorldInfo().getEventHandler().removeListener("audio");
		music.stop();
		audio.getMusicQueue().removeTrack(music);
		audio.getMusicQueue().stop();
		audio.getMusicQueue().clearTracks();
		audioManager.printEntityList(); audioManager = null;
		audio.cleanup(); audio = null;
	}
}
