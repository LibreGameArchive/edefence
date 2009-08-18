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

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

public class UITextureContainer extends UIContainer
{
	public Quad visibleQuad = null;
	private static BlendState as = null;
	
	public UITextureContainer(String name, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder) {
		
		super(name, posX, posY, zOrder, sizeX, sizeY);
		
		visibleQuad = new Quad(name + "UIQuad", sizeX, sizeY);
		visibleQuad.setRenderQueueMode(Renderer.QUEUE_ORTHO);  
		visibleQuad.setZOrder(zPos);
		
		visibleQuad.setLocalTranslation( (sizeX / 2.0f), (sizeY / 2.0f), 0);
	
		if(as == null) {
			as = DisplaySystem.getDisplaySystem().getRenderer().createBlendState();	// FIXME: Remove the hidden OpenGL call.
			as.setBlendEnabled(true);
			as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
			as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
			as.setTestEnabled(true);
			as.setTestFunction(BlendState.TestFunction.GreaterThan);
		}
		visibleQuad.setRenderState(as);
		visibleQuad.updateRenderState();
		getDisplayNode().attachChild(visibleQuad);
	}

	public UITextureContainer(String name, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parent) {
		this(name, sizeX, sizeY, posX, posY, zOrder);
		if(parent != null) parent.attachChild(this);	// TODO: Don't pass parent to the constructor.
	}
	
	public UITextureContainer(String name, TextureState t, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer) {
		
		this(name, sizeX, sizeY, posX, posY, zOrder, parentContainer);
		setTextureState(t);
	}

	public UITextureContainer(String name, ColorRGBA color, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer) {
		
		this(name, sizeX, sizeY, posX, posY, zOrder, parentContainer);
		
		visibleQuad.setSolidColor(color);
		visibleQuad.updateRenderState();
	}
	
	public void setTextureState(TextureState ts) {
		visibleQuad.setRenderState(ts);
		visibleQuad.updateRenderState();
	}
	
	/** Rotates the quad on which the texture is displayed. Does not change the texture itself.*/
	public void rotateTexture(float angleInDegrees) {
		Quaternion q = new Quaternion();
		
		q.fromAngleAxis(angleInDegrees * FastMath.DEG_TO_RAD, Vector3f.UNIT_Z);
		visibleQuad.setLocalRotation(q);
	}
	
	public void setTransparency(float t) {
		visibleQuad.setSolidColor(new ColorRGBA( 1.0f, 1.0f, 1.0f, t));

	}
	
}