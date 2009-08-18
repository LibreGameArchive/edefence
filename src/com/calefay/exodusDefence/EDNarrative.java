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


import java.nio.FloatBuffer;

import com.calefay.simpleUI.UIContainerBackground;
import com.calefay.simpleUI.UITimedContainer;
import com.calefay.utils.TextCanvas2D;
import com.jme.math.FastMath;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.TexCoords;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.RenderState.StateType;
import com.jme.util.geom.BufferUtils;

public class EDNarrative extends UITimedContainer {
	
	private Quad textQuad = null;
	private Quad iconQuad = null;
	private UIContainerBackground backgroundContainer = null;
	
	private static final int INITIALX = 32, INITIALY = 140;
	
	private int actualX = 0, actualY = 0;
	private float fadeOutModifier = 1.0f;	// Multiplies by the alpha. Used to show older messages more transparent even if they are still not in the fade out period.
	
	private ColorRGBA fadeCol = null, bkgCol = null;
	
	/** ED specific class for displaying the character icon narrative text. The ensemble exists for a fixed time, and fades out towards the end of it.*/ 
	public EDNarrative(float lifeTime, String[] text, int zOrder, Renderer renderer) {
		super("Narrative", lifeTime, INITIALX, INITIALY, zOrder, 512, 12);		// TODO: eww hardcoding.
		fadeOutModifier = 1.0f;
		fadeCol = new ColorRGBA( 1.0f, 1.0f, 1.0f, 1.0f);
		
		int tl = (text != null) ? text.length : 1;
		actualX = 512; 
		actualY = (tl > 0) ? (tl * 16) : 16;

		int textureX = FastMath.nearestPowerOfTwo(actualX);
        int textureY = FastMath.nearestPowerOfTwo(actualY);
		
		TextCanvas2D tc = new TextCanvas2D(textureX, textureY);	// TODO: Don't create a new canvas every time?
		tc.setFont("Sans PLAIN 12", 12f);
		
		for(int y = 0; y < tl; y++) {	// convoluted y positioning because scaleQuadTexture clips out the top of the texture when sizes don't match not the bottom.
			tc.writeText(text[tl-y-1], 0, textureY - (y * 16) - 4 );	// Hardcoded line spacing. Also note this ignores the first two lines as they are parameters.
		}
		
		textQuad = tc.getQuad(name + "TextQuad", actualX, actualY, renderer);
		tc.cleanup(); tc = null;
		textQuad.setRenderQueueMode(Renderer.QUEUE_ORTHO);  
		textQuad.setZOrder(zPos);
		
		scaleQuadTexture(textQuad, actualX, actualY);
		
		float yOffs = (actualY / 2.0f) - 4f; if(tl < 4) yOffs += (4 - tl) * 8.0f;
		textQuad.setLocalTranslation( 70.0f + (actualX / 2.0f),  yOffs - 12, 0);
		getDisplayNode().attachChild(textQuad);
		
		float sY = (actualY > 64.0f) ? actualY : 64.0f;
		resize((int)actualX + 64, (int)sY - 28);
	}
	
	public void scaleQuadTexture(Quad q, int actualX, int actualY) {
		float scaleX = (float)actualX / FastMath.nearestPowerOfTwo(actualX);
        float scaleY = (float)actualY / FastMath.nearestPowerOfTwo(actualY);
        FloatBuffer texCo = q.getTextureCoords(0).coords;//q.getTextureBuffer(0, 0);
        FloatBuffer newTC = BufferUtils.createFloatBuffer(texCo.limit());
        texCo.rewind();
        for(int i = 0; i < texCo.limit(); i += 2){
            float u = texCo.get();
            float v = texCo.get();
            newTC.put(u * scaleX);
            newTC.put(v * scaleY);
        }
        q.setTextureCoords(new TexCoords(newTC));
	}
	
	public EDNarrative(float lifeTime, String[] text, int zOrder, TextureState ts, Renderer renderer) {
		this(lifeTime, text, zOrder, renderer);
		
		iconQuad = new Quad(name + "IconQuad", 64, 64);	// TODO: Fix hardcoded size.
		iconQuad.setRenderQueueMode(Renderer.QUEUE_ORTHO);  
		iconQuad.setZOrder(zPos);
		int yPos = text.length > 4 ? 32 + (8 * (text.length - 4)) : 32;
		iconQuad.setLocalTranslation(32.0f, yPos - 12, 0);
		iconQuad.setRenderState(ts);
        
		RenderState alphaState = textQuad.getRenderState(StateType.Blend);
        if(alphaState != null) iconQuad.setRenderState(alphaState);
		iconQuad.updateRenderState();
		
		getDisplayNode().attachChild(iconQuad);
	}
	
	public void update(float interpolation) {
		super.update(interpolation);
		if(life < 1.0f) {	// TODO: Change hardcoded fadeout time (1s)
			fadeCol.set(1.0f, 1.0f, 1.0f,  fadeOutModifier * life);
			if(textQuad != null) textQuad.setSolidColor(fadeCol);
			if(iconQuad != null) iconQuad.setSolidColor(fadeCol);
			if(backgroundContainer != null) {
				bkgCol.set(0.1f, 0.1f, 0.1f, 0.5f * fadeOutModifier * life);
				backgroundContainer.setColor(bkgCol);
			}
			
		}
	}
	
	public int getYSize() {return actualY > 64 ? actualY : 64;}
	
	/* Used to move the narrative down the list as new ones are added.*/
	public void setOffset(int distance) {setPosition(INITIALX, INITIALY - distance);}
	
	public void setFadeoutModifier(float factor) {
		if(factor == fadeOutModifier) return;
		fadeOutModifier = FastMath.clamp(factor, 0.0f, 1.0f);
		if(fadeCol != null) fadeCol.set(1.0f, 1.0f, 1.0f,  fadeOutModifier * FastMath.clamp(life, 0.0f, 1.0f));
		if(textQuad != null) textQuad.setSolidColor(fadeCol);
		if(iconQuad != null) iconQuad.setSolidColor(fadeCol);
		if(bkgCol != null) bkgCol.set(0.1f, 0.1f, 0.1f,  0.5f * fadeOutModifier * FastMath.clamp(life, 0.0f, 1.0f));
	}
	
	public void setBackgroundContainer(UIContainerBackground bkg) {
		backgroundContainer = bkg;
		bkgCol = new ColorRGBA(0.1f, 0.1f, 0.1f, 0.5f);
		if(backgroundContainer != null) {backgroundContainer.setColor(bkgCol);}
	}
	
	public void cleanup() {
		super.cleanup();
		if(textQuad != null) {
			TextureState ts = (TextureState)textQuad.getRenderState(StateType.Texture);
			ts.deleteAll(true);
			textQuad.removeFromParent(); 
			textQuad = null;
		}
		if(iconQuad != null) iconQuad.removeFromParent(); iconQuad = null;
		actualX = 0; actualY = 0;
	}
}
