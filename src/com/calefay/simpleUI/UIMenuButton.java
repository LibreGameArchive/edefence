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

import com.jme.scene.state.TextureState;

public class UIMenuButton extends UITextureContainer
{
	
	public UIMenuButton(String name, TextureState ts, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer) {
		super(name, ts, sizeX, sizeY, posX, posY, zOrder, parentContainer);
		setTransparency(0.5f);
		}
	
	protected void onHover() {
		setTransparency(1.0f);
	}
	
	protected void onHoverOff() {
		setTransparency(0.5f);
	}
	
}