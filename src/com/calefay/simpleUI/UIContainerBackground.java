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

import com.calefay.utils.BorderedQuad;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

public class UIContainerBackground extends UIContainer {
	public BorderedQuad visibleQuad = null;
	private static BlendState as = null;
	
	public UIContainerBackground(TextureState ts, UIContainer parent) {
		super(parent.getName() + "ContainerBackground", 0, 0, parent.getZOrder() + 2, parent.getWidth(), parent.getHeight());
		
		parent.attachChild(this);
		
		visibleQuad = new BorderedQuad(parent.getName() + "ContainerBackgroundQuad", width, height, 16);	// Hardcoded border size
		visibleQuad.setRenderQueueMode(Renderer.QUEUE_ORTHO);  
		visibleQuad.setZOrder(zPos);
		
		visibleQuad.setLocalTranslation( (width / 2.0f), (height / 2.0f), 0);
	
		if(as == null) {
			as = DisplaySystem.getDisplaySystem().getRenderer().createBlendState();	// FIXME: Remove the hidden OpenGL call.
			as.setBlendEnabled(true);
			as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
			as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
			as.setTestEnabled(true);
			as.setTestFunction(BlendState.TestFunction.GreaterThan);
		}
		visibleQuad.setRenderState(as);
		visibleQuad.setRenderState(ts);
		visibleQuad.updateRenderState();
		getDisplayNode().attachChild(visibleQuad);
	}
	
	public UIContainerBackground(TextureState ts,
								 ColorRGBA color,
								 UIContainer parent) {
		this(ts, parent);
		
		visibleQuad.setSolidColor(color);
		visibleQuad.updateRenderState();
	}
	
	public void setTextureState(TextureState ts) {
		visibleQuad.setRenderState(ts);
		visibleQuad.updateRenderState();
	}
	
	public void setColor(ColorRGBA col) {
		if((visibleQuad != null) && (col != null)) visibleQuad.setSolidColor(col);

	}
}
