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
package com.calefay.exodusDefence.edControls;

import java.util.ArrayList;
import java.util.List;

import com.calefay.exodusDefence.EDSmoothedAction;
import com.calefay.exodusDefence.edControls.ControlEnums.actionType;
import com.calefay.exodusDefence.entities.EDAircraft;
import com.calefay.exodusDefence.weapons.Gun;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.action.InputAction;


public class EDAircraftInputHandler extends InputHandler {

	private List<EDGunFireAction> fire;
	
	private EDValueSmoother rollSmoother = null;
	private EDValueSmoother pitchSmoother = null;
	private EDValueSmoother yawSmoother = null;
	private EDValueSmoother throttleSmoother = null;
	private EDAircraft handledAircraft = null;
	private boolean invertY = false;
	private boolean isHelicopterHandler = false;
	
	private boolean firstFrame = true;
		
	public EDAircraftInputHandler(EDAircraft aircraft, TurretCameraHandler ch) {
		firstFrame = true;
		
		handledAircraft = aircraft;
		fire = new ArrayList<EDGunFireAction>();
		
		rollSmoother = new EDValueSmoother(5);
		pitchSmoother = new EDValueSmoother(5);
		yawSmoother = new EDValueSmoother(5);
		throttleSmoother = new EDValueSmoother(1);
		
		setupKeyboardLook(ch);
		setupMouseLook(ch);
	}
	
	/* Use to consume the first frame's movement without moving the aircraft (eg. to clear any stored movement after pause).*/
	public void setFirstFrame(boolean f) {firstFrame = f;}
	
	/** Changes the turret which this handler controls.
	 * Note that this will clear out all fire keys, which will need to be reassigned (presumably to new guns).
	 * @param turret - The new turret to be handled.
	 */
	public void setHandledAircraft(EDAircraft aircraft) {
		handledAircraft = aircraft;
	}
	
	public void clearFireKeys() {
		for(EDGunFireAction fireA : fire) {
			this.removeAction(fireA);
		}
		fire.clear();
	}
	
	public void addFireKey(Gun g, int keyBinding) {
		EDGunFireAction newFire = new EDGunFireAction(g);
		addAction(newFire, DEVICE_KEYBOARD, keyBinding, AXIS_NONE, false);
		fire.add(newFire);
	}
	
	public void addFireMouseButton(Gun g, int button) {
		EDGunFireAction newFire = new EDGunFireAction(g);
		addAction(newFire, DEVICE_MOUSE, button, AXIS_NONE, false);
		fire.add(newFire);
	}
    
    private void setupKeyboardLook(TurretCameraHandler ch) {
    	KeyBindingManager keyboard = KeyBindingManager.getKeyBindingManager();
        
        keyboard.set("pitchForward", KeyInput.KEY_W);
        keyboard.set("", KeyInput.KEY_S);
        keyboard.set("rollRight", KeyInput.KEY_D);
        keyboard.set("rollLeft", KeyInput.KEY_A);
        keyboard.set("yawRight", KeyInput.KEY_C);
        keyboard.set("yawLeft", KeyInput.KEY_X);
        keyboard.set("zoomIn", KeyInput.KEY_Q);
        keyboard.set("zoomOut", KeyInput.KEY_Z);
        
    	EDSmoothedAction keypitchForward = new EDSmoothedAction(1.0f, pitchSmoother, actionType.KEYBOARD);
        addAction(keypitchForward, "pitchForward", true);
        EDSmoothedAction keypitchBack = new EDSmoothedAction(-1.0f, pitchSmoother, actionType.KEYBOARD);
        addAction(keypitchBack, "pitchBack", true);
        
        EDSmoothedAction keyrollRight = new EDSmoothedAction(1.0f, rollSmoother, actionType.KEYBOARD);
        addAction(keyrollRight, "rollRight", true);
        EDSmoothedAction keyrollLeft = new EDSmoothedAction(-1.0f, rollSmoother, actionType.KEYBOARD);
        addAction(keyrollLeft, "rollLeft", true);
        
        EDSmoothedAction keyYawRight = new EDSmoothedAction(1.0f, yawSmoother, actionType.KEYBOARD);
        addAction(keyYawRight, "yawRight", true);
        EDSmoothedAction keyYawLeft = new EDSmoothedAction(-1.0f, yawSmoother, actionType.KEYBOARD);
        addAction(keyYawLeft, "yawLeft", true);
        
        InputAction keyZoomIn = new TurretCameraZoomAction(20.0f, ch, actionType.KEYBOARD);
        addAction(keyZoomIn, "zoomIn", true);
        InputAction keyZoomOut = new TurretCameraZoomAction(-20.0f, ch, actionType.KEYBOARD);
        addAction(keyZoomOut, "zoomOut", true);
    }
    
    private void setupMouseLook(TurretCameraHandler ch) {
    	//InputAction mousewheel = new TurretCameraZoomAction(2.0f, ch, actionType.MOUSE);
    	InputAction mousewheel = new EDSmoothedAction(1.0f, throttleSmoother, actionType.MOUSE);
        addAction( mousewheel, InputHandler.DEVICE_MOUSE, InputHandler.BUTTON_NONE, 2, false );
        
        EDSmoothedAction mouseSpin = new EDSmoothedAction(3.0f, rollSmoother, actionType.MOUSE);
        addAction(mouseSpin, InputHandler.DEVICE_MOUSE, InputHandler.BUTTON_NONE, 0, false);
        
        EDSmoothedAction mouseElevate = new EDSmoothedAction(2.0f, pitchSmoother, actionType.MOUSE);
        addAction(mouseElevate, InputHandler.DEVICE_MOUSE, InputHandler.BUTTON_NONE, 1, false);
    }

    public void setInvertY(boolean y) {
    	invertY = y;
    }
    
    // FIXME: It seems this should be done by registering an event with the input handler rather than overriding. See the superclass method for details.
    public void update( float time ) {
    	super.update(time);
    	
    	if(!isEnabled() || (handledAircraft == null) ) return;
    	if(!handledAircraft.isActive()) return;
    	
    	if(firstFrame) {firstFrame = false; return;}
    	
    	rollSmoother.update(time);
    	float rollAmount = rollSmoother.getAverageValue();
    	if(rollAmount != 0) {
    		if(isHelicopterHandler) handledAircraft.setControlStickZ(-rollAmount / time); else
    			handledAircraft.setControlStickX(rollAmount / time);// Kinda ugly. Divide by time because it should be -1..1 while the aircraft applies the tpf to it's actual maneuver.
    		handledAircraft.getAircraftNode().updateWorldData(time);	// FIXME: Would rather not do this here.
    	}
    	
    	pitchSmoother.update(time);
    	float pitchAmount = pitchSmoother.getAverageValue();
   		if(pitchAmount != 0) {
   			if(invertY) pitchAmount = -pitchAmount; 
   			handledAircraft.setControlStickY(pitchAmount / time);	// Kinda ugly. Divide by time because it should be -1..1 while the aircraft applies the tpf to it's actual maneuver.
   			handledAircraft.getAircraftNode().updateWorldData(time); // FIXME: Would rather not do this here.
   		}
   		
    	yawSmoother.update(time);
    	float yawAmount = yawSmoother.getAverageValue();
   		if(yawAmount != 0) { 
   			if(isHelicopterHandler) handledAircraft.setControlStickX(yawAmount / time);	
   				else handledAircraft.setControlStickZ(yawAmount / time);
   			handledAircraft.getAircraftNode().updateWorldData(time); // FIXME: Would rather not do this here.
   		}
   		
   		throttleSmoother.update(time);
   		float throttleChange = throttleSmoother.getAverageValue();
   		handledAircraft.moveStickThrottle(-throttleChange / 50f);	// One tick of the wheel is 1
   	}
    
    public void setIsHelicopterHandler(boolean isHelicopter) {isHelicopterHandler = isHelicopter;}
    
    public void cleanup() {
    	clearFireKeys();
    	clearActions();
    	fire = null;
    	handledAircraft = null;
    	rollSmoother.deactivate(); rollSmoother = null;
    	pitchSmoother.deactivate(); pitchSmoother = null;
    	yawSmoother.deactivate(); yawSmoother = null;
    	throttleSmoother.deactivate(); throttleSmoother = null;
    	
    }
}
