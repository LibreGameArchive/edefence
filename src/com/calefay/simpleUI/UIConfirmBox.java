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
import com.jme.scene.state.TextureState;

public class UIConfirmBox extends UIContainer {

	private UITextContainer textLabel = null;
	private UIMenuButton yesButton = null;
	private UIMenuButton noButton = null;
	
	private String yesEventType = null;
	private String noEventType = null;
	private Object yesEventParameters = null;
	private Object noEventParameters = null;
	
	public UIConfirmBox(String containerName,
						TextureState yesTS, TextureState noTS,
						int posX, int posY,
						int order, int sizeX, int sizeY, 
						String ... confirmationText) {
		super(containerName, posX, posY, order, sizeX, sizeY);
		if((confirmationText == null) || (confirmationText.length < 1)) confirmationText = new String[] {"blank"};
		yesEventType = "confirmYes"; noEventType = "confirmNo";
		
		yesButton = new UIMenuButton(name + "yesButton", yesTS, 64, 32, 10, 0, order, this);
		noButton = new UIMenuButton(name + "noButton", noTS, 32, 32, (sizeX - 48), 0, order, this);
		int sx = 256; if(confirmationText[0].length() < 12) sx = 128;
		textLabel = new UITextContainer(name + "TextLabel", sx, 16 * confirmationText.length, 10, height - 32, order, this, "Sans BOLD 14", 14f, confirmationText);	// FIXME: Hardcoded size
	}
	
	protected boolean handleChildEvent(GameEvent event) {
		if( (event.getEventInitiator() != yesButton) && (event.getEventInitiator() != noButton) ) return false;
		if(event.getEventInitiator() == yesButton) {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(yesEventType, targetEventQueue, this, yesEventParameters);
		}
		if(event.getEventInitiator() == noButton) {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(noEventType, targetEventQueue, this, noEventParameters);
		}
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("popupClose", targetEventQueue, this, null);
		this.setActive(false);
		
		return true;
	}
	
	public void setYesEvent(String eventType, Object eventParameters) {
		this.yesEventType = eventType;
		this.yesEventParameters = eventParameters;
		
	}
	
	public void setNoEvent(String eventType, Object eventParameters) {
		this.noEventType = eventType;
		this.noEventParameters = eventParameters;
	}
}
