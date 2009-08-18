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
package com.calefay.simpleUI;

import com.calefay.utils.GameEvent;
import com.calefay.utils.GameWorldInfo;

public class UIMenu extends UIContainer {

	public static final String EVENT_PREVIOUSMENU = "PreviousMenu";	// Event type for a button to switch to the previous menu
	
	protected UIMenu previousMenu = null;
	protected UIMenu nextMenu = null;
	
	public UIMenu(String containerName, 
				  int posX, int posY, int order,
				  int sizeX, int sizeY) {
		super(containerName, posX, posY, order, sizeX, sizeY);
		// TODO Auto-generated constructor stub
	}

	public void setNextMenu(UIMenu next) {nextMenu = next;}
	public void setPreviousMenu(UIMenu previous) {previousMenu = previous;}
	public void setMenuChain(UIMenu previous, UIMenu next) {previousMenu = previous; nextMenu = next;}
	
	protected boolean handleChildEvent(GameEvent event) {
		if(event.getEventType().equals(EVENT_PREVIOUSMENU)) { // TODO: Just don't do it like this!
			this.setActive(false);
			previousMenu.setActive(true);
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(new GameEvent("activeMenuChanged", this, previousMenu), targetEventQueue); // TODO: Hardcoded event type
			return true;
		}
		return false;
	}
}
