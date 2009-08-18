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
import com.calefay.exodusDefence.entities.EDTurret;
import com.calefay.exodusDefence.weapons.Gun;
import com.jme.input.InputHandler;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.input.action.InputAction;


public class EDTurretInputHandler extends InputHandler {

	private List<EDGunFireAction> fire;
	
	private EDValueSmoother spinSmoother = null;
	private EDValueSmoother elevationSmoother = null;
	private EDTurret handledTurret = null;
	private boolean invertY = false;
		
	private boolean firstFrame = true;
	
	public EDTurretInputHandler(EDTurret turret, TurretCameraHandler ch) {
		firstFrame = true;
		
		handledTurret = turret;
		fire = new ArrayList<EDGunFireAction>();
		
		spinSmoother = new EDValueSmoother(5);
		elevationSmoother = new EDValueSmoother(5);
		
		setupKeyboardLook(ch);
		setupMouseLook(ch);
	}
	
	/** Changes the turret which this handler controls.
	 * Note that this will clear out all fire keys, which will need to be reassigned (presumably to new guns).
	 * @param turret - The new turret to be handled.
	 */
	public void setHandledTurret(EDTurret turret) {
		handledTurret = turret;
	}
	
	/* Use to consume the first frame's movement without moving the aircraft (eg. to clear any stored movement after pause).*/
	public void setFirstFrame(boolean f) {firstFrame = f;}
	
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
        
        keyboard.set("elevateUp", KeyInput.KEY_W);
        keyboard.set("elevateDown", KeyInput.KEY_S);
        keyboard.set("spinRight", KeyInput.KEY_D);
        keyboard.set("spinLeft", KeyInput.KEY_A);
        keyboard.set("zoomIn", KeyInput.KEY_Q);
        keyboard.set("zoomOut", KeyInput.KEY_Z);
        
    	EDSmoothedAction keyElevateUp = new EDSmoothedAction(1.0f, elevationSmoother, ControlEnums.actionType.KEYBOARD);
        addAction(keyElevateUp, "elevateUp", true);
        
        EDSmoothedAction keyElevateDown = new EDSmoothedAction(-1.0f, elevationSmoother, ControlEnums.actionType.KEYBOARD);
        addAction(keyElevateDown, "elevateDown", true);
        
        EDSmoothedAction keySpinRight = new EDSmoothedAction(1.0f, spinSmoother, ControlEnums.actionType.KEYBOARD);
        addAction(keySpinRight, "spinRight", true);
        
        EDSmoothedAction keySpinLeft = new EDSmoothedAction(-1.0f, spinSmoother, ControlEnums.actionType.KEYBOARD);
        addAction(keySpinLeft, "spinLeft", true);
        
        InputAction keyZoomIn = new TurretCameraZoomAction(20.0f, ch, ControlEnums.actionType.KEYBOARD);
        addAction(keyZoomIn, "zoomIn", true);
        
        InputAction keyZoomOut = new TurretCameraZoomAction(-20.0f, ch, ControlEnums.actionType.KEYBOARD);
        addAction(keyZoomOut, "zoomOut", true);
    }
    
    private void setupMouseLook(TurretCameraHandler ch) {
    	InputAction mousewheel = new TurretCameraZoomAction(2.0f, ch, ControlEnums.actionType.MOUSE);
        addAction( mousewheel, InputHandler.DEVICE_MOUSE, InputHandler.BUTTON_NONE, 2, false );
        
        EDSmoothedAction mouseSpin = new EDSmoothedAction(3.0f, spinSmoother, ControlEnums.actionType.MOUSE);
        addAction(mouseSpin, InputHandler.DEVICE_MOUSE, InputHandler.BUTTON_NONE, 0, false);
        
        EDSmoothedAction mouseElevate = new EDSmoothedAction(2.0f, elevationSmoother, ControlEnums.actionType.MOUSE);
        addAction(mouseElevate, InputHandler.DEVICE_MOUSE, InputHandler.BUTTON_NONE, 1, false);
    }

    public void setInvertY(boolean y) {
    	invertY = y;
    }
    
    // FIXME: It seems this should be done by registering an event with the input handler rather than overriding. See the superclass method for details.
    public void update( float time ) {
    	super.update(time);
    	if(!isEnabled() || (handledTurret == null) ) return;
    	if(firstFrame) {firstFrame = false; return;}
    	
    	spinSmoother.update(time);
    	float spinAmount = spinSmoother.getAverageValue();
    	if(spinAmount != 0) {
    		handledTurret.spin(spinAmount);
    		handledTurret.getTurretNode().updateWorldData(time);	// FIXME: Would rather not do this here.
    		}
    	
    	elevationSmoother.update(time);
    	float elevationAmount = elevationSmoother.getAverageValue();
   		if(elevationAmount != 0) {
   			if(invertY) elevationAmount = -elevationAmount; 
   			handledTurret.elevate(elevationAmount);
   			// TODO: Don't use the barrel node
   			handledTurret.getBarrelNode1().updateWorldData(time); // FIXME: Would rather not do this here.
   			}
   		
   		}
    
    public void cleanup() {
    	clearFireKeys();
    	clearActions();
    	fire = null;
    	handledTurret = null;
    	spinSmoother = null;
    	elevationSmoother = null;
    }
}
