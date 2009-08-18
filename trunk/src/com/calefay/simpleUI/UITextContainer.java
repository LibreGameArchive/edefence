package com.calefay.simpleUI;


import com.calefay.utils.TextCanvas2D;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

public class UITextContainer extends UITextureContainer
{
	// FIXME: **** ADD CLEANUP TO CLEAR THE TEXT TEXTURE AS IT IS UNIQUE TO EACH INSTANCE!!!
	/* NOTE - Both the texture and the visible quad will be sizeX by sizeY in pixels.*/
	public UITextContainer(String name, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer,
			String font, float fontSize,
			String ... text) {
		super(name, sizeX, sizeY, posX, posY, zOrder, parentContainer);
		
		// FIXME: ENFORCE POWER OF 2 TEXTURES
		
		TextCanvas2D tc = new TextCanvas2D(sizeX, sizeY);
		tc.setFont(font, fontSize);
		writeText(tc, text);
		
		TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();	// FIXME: Hidden OpenGL call.
        ts.setTexture(tc.createTexture());
        ts.setEnabled(true);
        setTextureState(ts);
		
		tc.cleanup(); tc = null;
	}

	public UITextContainer(String name, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer,
			String ... text) {
		this(name, sizeX, sizeY, posX, posY, zOrder, parentContainer, "Sans PLAIN 12", 12f, text);
	}
	
	private void writeText(TextCanvas2D canvas, String ... text) {
		if( (text == null) || (text.length < 1) ) return;
		for(int y = 0; y < text.length; y++) {
			canvas.writeText(text[y], 0, (y + 1) * 14);	// Hardcoded line spacing.
		}
	}
	
	public UITextContainer(String name, String displayText, 
			int sizeX, int sizeY,
			int posX, int posY, int zOrder,
			UIContainer parentContainer) {

		this(name, sizeX, sizeY, posX, posY, zOrder, parentContainer, displayText);
	}
	
}
