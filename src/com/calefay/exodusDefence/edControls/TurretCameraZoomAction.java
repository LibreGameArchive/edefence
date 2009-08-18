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

import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;

public class TurretCameraZoomAction extends InputAction {

	
	protected ControlEnums.actionType typeOfAction;
	protected TurretCameraHandler camHandler = null;
	protected float zoomSpeed = 0;
	
	public TurretCameraZoomAction(float speed, TurretCameraHandler ch, ControlEnums.actionType t) {
		camHandler = ch;
		zoomSpeed = speed;
		typeOfAction = t;
	}
	
	public void performAction(InputActionEvent evt) {
		float magnitude = 0;
		if(typeOfAction == ControlEnums.actionType.MOUSE) {magnitude = evt.getTriggerDelta();} else {magnitude = evt.getTime();}
		camHandler.addFollowDistance( -zoomSpeed * magnitude);
	}

}
