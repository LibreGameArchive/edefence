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

import com.jme.image.Texture;
import com.jme.math.Vector3f;
import com.jme.scene.state.TextureState;

public class UIProgressBar extends UITextureContainer {
	
	private float maxValue = 0;
	private float valueIncrement = 0;
	private float currentValue = 0;
	private Texture texture = null;
	
	// TODO: This whole class sucks.
	public UIProgressBar(String name,
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			float max, float step, float current,
			UIContainer parentContainer) {
		super(name, sizeX, sizeY, posX, posY, zOrder, parentContainer);
		initialize(max, step, current);
	}
	
	public UIProgressBar(String name, TextureState ts,
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			float max, float step, float current,
			UIContainer parentContainer) {
		super(name, sizeX, sizeY, posX, posY, zOrder, parentContainer);
		
		visibleQuad.setRenderState( ts );
		visibleQuad.updateRenderState();
		
		initialize(max, step, current);
	}
	
	public void initialize(float max, float step, float current) {
		maxValue = max;
		valueIncrement = step;
		currentValue = current;
		setProgress(current);
	}
	
	public void setProgress(float p) {
		if(p > maxValue) p = maxValue;
		if(p < 0) p = 0;
		
		// TODO: Make it only update if the a full step has been completed.
		currentValue = p;
		
		float fillFraction = currentValue / maxValue;
		visibleQuad.setLocalScale( new Vector3f(fillFraction,1.0f, 1.0f));
		visibleQuad.getLocalTranslation().setX( -(width / 2) * (1 - fillFraction) );
		if(texture != null) texture.setScale( new Vector3f(fillFraction,1.0f,1.0f));
		updatePosition();	// TODO: Get rid of the need for this, and remove the method from UIContainer - this is the only place it is called.
	}
	
	@Override
	public void cleanup() {
		super.cleanup();
		maxValue = 0; valueIncrement = 0; currentValue = 0;
		texture = null;
	}
}
