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


import com.calefay.utils.GameRemoveable;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.Node;
import com.jmex.audio.AudioSystem;
import com.jmex.audio.AudioTrack;
import com.jmex.audio.AudioTrack.TrackType;
 
public class EDSoundEffect implements GameRemoveable {
	private String name = null;
	private Node emitter = null;
	private Vector3f emitterPos = null;
	private AudioTrack sound = null;
	private Camera listenerCam = null;	// TODO: Might be preferable not to hold on to listener in each sfx as it should probably be the same for all sounds.
	
	private boolean repeating = false;
	private boolean movingEmitter = false;
	private boolean active = false;
	
	private float playRangeSQ = 0;
	private float stopRangeSQ = 0;
	
	private GameRemoveable linkedEntity = null;

	/** A none-repeating, moveable positional sound effect. Can be added to a GameRemoveable manager and will then automatically update itself and remove itself when complete.*/
	public EDSoundEffect(String name, URL resource, Node emitter, Camera listener, float playRange, float stopRange, float maxVolume) {
		this.emitter = emitter;
        initialize(name, resource, listener, playRange, stopRange, maxVolume);
        sound.track(emitter);
        movingEmitter = true;
	}

	/** A none-repeating, static, positional sound effect. Can be added to a GameRemoveable manager and will then automatically update itself and remove itself when complete.*/
	public EDSoundEffect(String name, URL resource, Vector3f position, Camera listener, float playRange, float stopRange, float maxVolume) {
		movingEmitter = false;
		initialize(name, resource, listener, playRange, stopRange, maxVolume);
		emitterPos = new Vector3f(position);
		sound.setWorldPosition(emitterPos);
		
	}
	
	protected void initialize(String name, URL resource, Camera listener, float playRange, float stopRange, float maxVolume) {
		this.name = name;
		active = true;
		repeating = false;
		
		this.listenerCam = listener;
		sound = AudioSystem.getSystem().createAudioTrack(resource, false);
        sound.setType(TrackType.POSITIONAL);

        sound.setRelative(false);
        sound.setLooping(false);
        
        sound.setMaxAudibleDistance(playRange);
        sound.setReferenceDistance(playRange / 10f);
        sound.setRolloff(1.0f);
        //sound.autosetRolloff();

        playRangeSQ = playRange * playRange;
        stopRangeSQ = stopRange * stopRange;
        
        sound.setTargetVolume(maxVolume); sound.setVolume(maxVolume);
        sound.getPlayer().setStartTime(System.currentTimeMillis());	// Workaround to stop it cutting at the first time check (which should return 0 but doesn't)
	}
	
	public void setPitch(float pitch) {sound.setPitch(pitch);}
	public void setReferenceDistance(float distance) {sound.setReferenceDistance(distance);}
	
	public void cleanup() {
		active = false;
		sound.stop();
		sound = null;
	}

	
	public void linkToEntity(GameRemoveable entity) {
		linkedEntity = entity;	// TODO: Not sure about this approach but it does help avoid orphaned repeating sounds.
	}
	
	public void deactivate() {
		active = false;
		sound.stop();
	}

	public boolean isActive() {return active;}
	/** If set then the effect will repeat endlessly and will never be automatically removed.*/
	public void setRepeating(boolean repeating) {
		this.repeating = repeating;
		sound.setLooping(repeating);
	}

	public void update(float interpolation) {
		if(linkedEntity != null && !linkedEntity.isActive()) {
			deactivate();
			return;
		}
		if( !repeating && ((sound.getCurrentTime() * sound.getPitch()) > sound.getTotalTime()) ) {
			deactivate();
		} else {
			if(movingEmitter) emitterPos = emitter.getWorldTranslation();
			float emitterDistSQ = listenerCam.getLocation().distanceSquared(emitterPos);
			if(sound.isPlaying()) {
				if(emitterDistSQ > stopRangeSQ) {sound.stop();}
			} else {
				if(emitterDistSQ < playRangeSQ) sound.play();
			}
		}
	}

	public String toString() {return name;}
}
