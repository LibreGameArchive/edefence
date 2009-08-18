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

import java.nio.FloatBuffer;

import com.jme.scene.state.TextureState;

public class UICheckBox extends UITextureContainer {
	protected boolean checked = false;
	
	public UICheckBox(String name, boolean initialStatus, TextureState image, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer) {
		
		super(name, image, sizeX, sizeY, posX, posY, zOrder, parentContainer);
		
		checked = initialStatus;
		setTransparency(0.6f);
		setTextureCoords();
	}
	
	/** Returns true if the checkbox is checked, false if not.*/
	public boolean getStatus() {
		return checked;
	}
	
	protected boolean handleClick() {
		if(checked) checked = false; else checked = true;
		setTextureCoords();
		return false;
	}
	
	protected void onHover() {
		setTransparency(1.0f);
	}
	
	protected void onHoverOff() {
		setTransparency(0.6f);
	}
	
    private void setTextureCoords() {
    	float yOffs = 0; if(!checked) yOffs = 0.5f;
    	
        FloatBuffer tbuf = visibleQuad.getTextureCoords(0).coords;
        tbuf.clear();
        tbuf.put(0).put(0.5f + yOffs);
        tbuf.put(0).put(0 + yOffs);
        tbuf.put(1.0f).put(0 + yOffs);
        tbuf.put(1.0f).put(0.5f + yOffs);
    }
}