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


import com.calefay.utils.GameRemoveable;
import com.calefay.utils.QuadGenerator;
import com.calefay.utils.QuadGenerator.HorizVertAxes;
import com.calefay.utils.QuadGenerator.HorizontalAlignment;
import com.calefay.utils.QuadGenerator.VerticalAlignment;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial.LightCombineMode;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.ZBufferState;
import com.jme.scene.state.ZBufferState.TestFunction;
import com.jme.system.DisplaySystem;

public class EDFader implements GameRemoveable {

	public enum FadeMode {INACTIVE, FADING_IN, FADING_OUT};
	
	private boolean active = false;
	
	private Quad faderQuad = null;
	private ColorRGBA color = null;
	
	private float duration = 0;
	private float remaining = 0;
	private FadeMode mode = null;
	
	public EDFader() {
		active = true;
	
		mode = FadeMode.INACTIVE;
				
		faderQuad = QuadGenerator.getQuad("FaderQuad", 1.0f, 1.0f, HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM, HorizVertAxes.XY);
		
		faderQuad.setRenderQueueMode(Renderer.QUEUE_ORTHO);
		faderQuad.setZOrder(0);

		ZBufferState zs = DisplaySystem.getDisplaySystem().getRenderer().createZBufferState();
		zs.setEnabled(true);
		zs.setFunction(TestFunction.Always);
		faderQuad.setRenderState(zs);
		
        BlendState  as= DisplaySystem.getDisplaySystem().getRenderer().createBlendState();
        as.setBlendEnabled(true);
        as.setSourceFunction(BlendState.SourceFunction.SourceAlpha);
        as.setDestinationFunction(BlendState.DestinationFunction.OneMinusSourceAlpha);
        as.setTestEnabled(true);
        as.setTestFunction(BlendState.TestFunction.GreaterThan);
        as.setEnabled(true);
        faderQuad.setRenderState(as);
		
		faderQuad.setLightCombineMode(LightCombineMode.Off);
		color = new ColorRGBA(0, 0, 0, 1.0f);
		faderQuad.setDefaultColor(color);
		
		faderQuad.updateRenderState();
		
	}

	public void startFade(FadeMode type, Node n, float duration) {
		if(type == FadeMode.INACTIVE) return;
		
		n.attachChild(faderQuad); faderQuad.setLocalTranslation(0, 0, 0);
		
		this.duration = duration;
		this.remaining = duration;
		this.mode = type;
		
		if(type == FadeMode.FADING_IN) color.a = 1.0f; else color.a = 0.0f;
	}
	
	public void setSize(float width, float height) {
		if(faderQuad != null) {
			faderQuad.getLocalScale().set(width, height, 1.0f);
			faderQuad.updateWorldVectors();
		}
	}
	
	public boolean isActive() {return active;}

	public void update(float interpolation) {
		if(!active || (duration <= 0) ) return;
		if(mode == FadeMode.INACTIVE) return;

		remaining -= interpolation;
		if(remaining < 0) {
			remaining = 0;
			mode = FadeMode.INACTIVE;
			faderQuad.removeFromParent();
		}
		
		float a = 0;
		a = remaining / duration;
		if(mode == FadeMode.FADING_OUT) a = 1.0f - a;
		color.a = a;
	}

	private float actualBrightness = 0;
	
	public void startIncrementalFade(float duration, Node parent) {
		this.duration = duration;
		this.remaining = duration;
		actualBrightness = 1.0f;
		parent.attachChild(faderQuad); faderQuad.setLocalTranslation(0, 0, 0);
	}
	
	/** Use to fade out a screen by rendering a translucent quad over it repeatedly.
	 *  This method just sets the alpha based on fade time and framerate, and gives you back the quad.*/
	public void incrementalFadeOut(float interpolation) {
		if(duration <= 0) return;
		float desiredBrightness = remaining / duration;
		float brightnessChange = desiredBrightness / actualBrightness;
		float alpha = 1.0f - brightnessChange;	// As we are applying a black rectangle - it's alpha is 1 - the desired brightness of the underlying image.
		color.a = alpha;
		
		actualBrightness = desiredBrightness;
		remaining -= interpolation; 
	}
	
	public void deactivate() {
		active = false;
	}
	
	public void cleanup() {
		active = false;
		if(faderQuad != null) faderQuad.removeFromParent(); faderQuad = null;
		color = null;
	}
}
