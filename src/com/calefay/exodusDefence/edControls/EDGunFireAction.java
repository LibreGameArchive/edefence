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


import com.calefay.exodusDefence.weapons.Gun;
import com.jme.input.action.InputActionEvent;
import com.jme.input.action.KeyInputAction;


public class EDGunFireAction extends KeyInputAction {

	Gun gun;
	
	public EDGunFireAction(Gun g) {
		gun = g;
	}
	
	public void performAction(InputActionEvent evt) {
		if(gun == null) {return;}
		
		if(evt.getTriggerPressed()) {
			gun.fire();
		} else {
			gun.stopFiring();
		}
	}

}
