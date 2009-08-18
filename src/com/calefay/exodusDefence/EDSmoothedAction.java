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

import com.calefay.exodusDefence.edControls.ControlEnums;
import com.calefay.exodusDefence.edControls.EDValueSmoother;
import com.jme.input.action.InputAction;
import com.jme.input.action.InputActionEvent;


public class EDSmoothedAction extends InputAction {
	
	protected ControlEnums.actionType typeOfAction;
	EDValueSmoother smoother = null;
	float speed = 0;
	
	public EDSmoothedAction(float rotationSpeed, EDValueSmoother valueSmoother, ControlEnums.actionType t) {
		smoother = valueSmoother;
		speed = rotationSpeed;
		
		typeOfAction = t;
	}
		
	public void performAction(InputActionEvent evt) {
		float magnitude = 0;
		if(typeOfAction == ControlEnums.actionType.MOUSE) {magnitude = evt.getTriggerDelta();} else {magnitude = evt.getTime();}
		smoother.addValue(-speed * magnitude);
	}
	
}
