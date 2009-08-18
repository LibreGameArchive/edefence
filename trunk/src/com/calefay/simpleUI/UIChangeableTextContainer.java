package com.calefay.simpleUI;


import java.awt.Color;

import com.calefay.utils.TextCanvas2D;
import com.jme.scene.state.RenderState;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

public class UIChangeableTextContainer extends UITextureContainer
{
	private TextCanvas2D tc = null;
	/* NOTE - Both the texture and the visible quad will be sizeX by sizeY in pixels.*/
	public UIChangeableTextContainer(String name, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder) {
		super(name, sizeX, sizeY, posX, posY, zOrder, null);
		
		// FIXME: ENFORCE POWER OF 2 TEXTURES
		
		tc = new TextCanvas2D(sizeX, sizeY);
		tc.setFont("Sans PLAIN 12", 12f);
		tc.clear();
		
		TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();	// FIXME: Hidden OpenGL call.
        ts.setTexture(tc.createTexture());
        ts.setEnabled(true);
        setTextureState(ts);
	}
	
	public UIChangeableTextContainer(String name, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer,
			String ... text) {
		super(name, sizeX, sizeY, posX, posY, zOrder, parentContainer);
		
		// FIXME: ENFORCE POWER OF 2 TEXTURES
		
		tc = new TextCanvas2D(sizeX, sizeY);
		tc.setFont("Sans PLAIN 12", 12f);
		tc.clear();
		writeText(text);
		
		TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();	// FIXME: Hidden OpenGL call.
        ts.setTexture(tc.createTexture());
        ts.setEnabled(true);
        setTextureState(ts);
	}
	
	/* Clears the underlying canvas. Does not update the visible component.*/
	public void clearCanvas() {if(tc != null) tc.clear();}
	public void clearCanvas(Color background) {
		if(tc == null) return;
		tc.setBackground(background);
		tc.clear();
	}
	
	/* Writes text to the underlying canvas. Will not show until updateText() is called.*/
	public void writeText(String ... text) {
		for(int y = 0; y < text.length; y++) {
			tc.writeText(text[y], 0, (y + 1) * 12);	// Hardcoded line spacing.
		}
	}
	
	/* Writes text to the underlying canvas. Will not show until updateText() is called.*/
	public void writeText(String text, int x, int y) {
		if(text != null) tc.writeText(text, x, y);
	}
	
	public void setFont(String font, float size) {tc.setFont(font, size);}
	
	public UIChangeableTextContainer(String name, String displayText, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer) {

		this(name, sizeX, sizeY, posX, posY, zOrder, parentContainer, displayText);
	}
	
	/* Clears the canvas and writes new text, then updates the visible element.*/
	public void changeText(String text) {	// TODO: This is ugly. Instead of replacing the texture update it perhaps using LWJGLTextureUpdater?
		tc.clear();
		writeText(text);
		updateText();
	}

	/* Updates the visible element from the underlying canvas.*/
	public void updateText() {
		TextureState ts = (TextureState)visibleQuad.getRenderState(RenderState.RS_TEXTURE);
		ts.deleteAll(true);
        ts.setTexture(tc.createTexture());
	}
	
	public void cleanup() {
		TextureState ts = (TextureState)visibleQuad.getRenderState(RenderState.RS_TEXTURE);
		ts.deleteAll(true); ts = null;
		tc.cleanup(); tc = null;
	}
}
