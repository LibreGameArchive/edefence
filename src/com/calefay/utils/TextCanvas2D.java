package com.calefay.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import com.jme.image.Texture;
import com.jme.renderer.Renderer;
import com.jme.scene.Spatial.LightCombineMode;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.BlendState;
import com.jme.scene.state.TextureState;
import com.jme.util.TextureManager;

public class TextCanvas2D {
	private static BlendState as = null;
	
	private BufferedImage bImage = null;
	private Graphics2D gfx2D = null;
	private int width, height;

    /** Base code taken from forum post by hevee*/
    public TextCanvas2D(int xSize, int ySize) {
        bImage = new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_ARGB);
        
        gfx2D = bImage.createGraphics();
        gfx2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gfx2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        this.width = xSize; this.height = ySize;
        
        setFont("Sans PLAIN 40", 40f);
        gfx2D.setBackground(new Color(0, 0, 0, 0) );
        setForeground(new Color(1f, 1f, 1f));
    }
    
    public void setFont(String fontStr, float fontResolution){
        Font drawFont = Font.decode(fontStr).deriveFont(fontResolution);
        gfx2D.setFont(drawFont);
    }
    public void setForeground(Color foreground) {gfx2D.setColor(foreground);}
    public void setBackground(Color background) {gfx2D.setColor(background);}
    
    public void writeText(String text, int posX, int posY) {gfx2D.drawString(text, posX, posY);}
    
    public void clear() {
    	gfx2D.clearRect(0, 0, width, height);
    }
    
    /** Creates a new texture object representing this canvas.*/
    public Texture createTexture() {
    	Texture tx = TextureManager.loadTexture(bImage, Texture.MinificationFilter.BilinearNoMipMaps, Texture.MagnificationFilter.Bilinear, true);
    	return tx;
    }
    
    /** Returns a textured quad. Created from scratch whenever this method is called.*/
    public Quad getQuad(String name, float sizeX, float sizeY, Renderer renderer){	// FIXME: REMOVE THIS, THIS CLASS SHOULD NOT BE CONCERNED WITH ALPHASTATES ETC. IT IS FOR CREATING TEXT.
        Quad q = new Quad(name, sizeX, sizeY);
        TextureState ts = renderer.createTextureState();
        Texture tex = createTexture();
        
        ts.setTexture(tex);
        ts.setEnabled(true);
        q.setRenderState(ts);
        
        q.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
        
        if(as == null) {
        	as = renderer.createBlendState();
            as.setBlendEnabled(true);
            as.setTestEnabled(true);
            as.setTestFunction(BlendState.TestFunction.GreaterThan);
            as.setEnabled(true);
        }
		
        q.setRenderState(as);
        
        q.setLightCombineMode(LightCombineMode.Off);
        q.updateRenderState();
        return q;
    }

    public void cleanup() {
    	if(gfx2D != null) gfx2D.dispose(); gfx2D = null;
    	bImage = null;
    }
}