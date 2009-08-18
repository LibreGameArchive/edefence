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

import com.calefay.exodusDefence.EDFader.FadeMode;
import com.calefay.exodusDefence.EDLevel.LightData;
import com.calefay.exodusDefence.EDLevel.SplatLayer;
import com.calefay.exodusDefence.EDLevel.skyboxSides;
import com.calefay.utils.GameResourcePack;
import com.calefay.utils.TextCanvas2D;
import com.gibbon.mfkarpg.terrain.splatting.TerrainPass;
import com.jme.bounding.OrientedBoundingBox;
import com.jme.image.Texture;
import com.jme.image.Image.Format;
import com.jme.light.DirectionalLight;
import com.jme.light.Light;
import com.jme.light.PointLight;
import com.jme.light.SpotLight;
import com.jme.math.Plane;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.renderer.pass.BasicPassManager;
import com.jme.scene.Node;
import com.jme.scene.Skybox;
import com.jme.scene.Spatial.CullHint;
import com.jme.scene.shape.Quad;
import com.jme.scene.state.CullState;
import com.jme.scene.state.FogState;
import com.jme.scene.state.LightState;
import com.jme.scene.state.ZBufferState;
import com.jme.scene.state.CullState.Face;
import com.jme.scene.state.FogState.DensityFunction;
import com.jme.scene.state.FogState.Quality;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jme.util.Timer;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.effects.water.WaterRenderPass;
import com.jmex.terrain.TerrainBlock;


public class EDDisplaySystem {

	private DisplaySystem display;
	
	private BasicPassManager passManager;
	
	private Quad loadingQuad = null;
	public EDFader fader = null;	// TODO: Make private
	
	private TerrainBlock tb = null;
	private Skybox skybox = null;
	
	public EDDisplaySystem() {
		
		display = DisplaySystem.getDisplaySystem();
		
		passManager = new BasicPassManager();
		
		TextCanvas2D loadingTC = new TextCanvas2D(128, 32);
	    loadingTC.setFont("Sans PLAIN 24", 24f);
	    loadingTC.writeText("LOADING...", 0, 32);
	    loadingQuad = loadingTC.getQuad("LoadingQuad", 128.0f, 32.0f, display.getRenderer());
	    loadingQuad.setRenderQueueMode(Renderer.QUEUE_ORTHO);
	    
	    fader = new EDFader();
	    fader.setSize(display.getWidth(), display.getHeight());
	}
	
	public void initializeLevel(EDLevel level, EDGameModel model, GameResourcePack levelResources, Node skyboxRoot, Camera cam) {	// TODO: Rationalize the parameters.
		FogState fs = buildFogState(level.getFogSettings()); 
        if(fs != null) {
    		model.getGameWorldRoot().setRenderState(fs);
    		model.getGameWorldRoot().updateRenderState();
        }

		createLights(level, model.getGameWorldRoot());
		createSkybox(level, skyboxRoot);
		tb = model.getTerrain(); if(tb != null) {buildTerrainVisual(level, levelResources, fs);}
		if(level.hasWaterPlane()) {
			setupWater(level.getWaterHeight(), model.getGameWorldRoot(), cam);
		}
	}
	
	public void update(float interpolation) {
		if(fader != null) fader.update(interpolation);
	}
	
	public void updateGameplay(float interpolation, Camera cam) {
		passManager.updatePasses(interpolation);
		
		if(waterQuad != null) updateWater(cam);
		
		skybox.setLocalTranslation(cam.getLocation().x, cam.getLocation().y, cam.getLocation().z);
		skybox.updateGeometricState(interpolation, true);
	}
	
	protected void renderGameplay(float interpolation, Node rootNode) {
		passManager.renderPasses(display.getRenderer());
		if(tb != null) {
			tb.setCullHint(CullHint.Always); display.getRenderer().draw(rootNode); tb.setCullHint(CullHint.Dynamic); // Tacky
		}
	}
	
	public void fadeOut(float duration) {
		// Bit hacky but this will fade out whatever is already on the screen.
		Timer timer = Timer.getTimer();
		float remaining = duration;
		float interpolation;
		Node n = new Node("FaderNode");
		fader.startIncrementalFade(duration, n);
		
		while(remaining > 0) {
			timer.update();
			interpolation = timer.getTimePerFrame();
			remaining -= interpolation;
			display.getRenderer().clearZBuffer();
			fader.incrementalFadeOut(interpolation);
			display.getRenderer().draw(n);
			display.getRenderer().displayBackBuffer();	// Need to fade both buffers as they are not getting redrawn.
			//try{Thread.sleep(50);}catch(Exception e){};
			Thread.yield();
		}
		display.getRenderer().clearBuffers();
		display.getRenderer().displayBackBuffer();
		display.getRenderer().clearBuffers();
	}
	
	public void displayLoading(boolean yes, Node parentNode) {
		if(yes) {
			parentNode.attachChild(loadingQuad);
		    loadingQuad.setLocalTranslation(display.getWidth() / 2, display.getHeight() / 2, 0);
		    display.getRenderer().clearBuffers();
		    display.getRenderer().draw(parentNode); display.getRenderer().displayBackBuffer(); display.getRenderer().draw(parentNode);	// Make sure it is also displayed in the backbuffer.
		} else {
			loadingQuad.removeFromParent();
		}
	}
	
	public void startFade(FadeMode type, Node n, float duration) {
		fader.startFade(type, n, duration);
	}
	
	public void createSkybox(EDLevel currentLevel, Node parent) {
		skybox = new Skybox("Skybox", 10f, 10f, 10f);
		
		Texture north = TextureManager.loadTexture(
				ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, 
						currentLevel.getSkyboxTexturePath(skyboxSides.North)),
						Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear,
						Format.GuessNoCompression, 1.0f, true);
		Texture south = TextureManager.loadTexture(
				ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE,
						currentLevel.getSkyboxTexturePath(skyboxSides.South) ),
						Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear,
						Format.GuessNoCompression, 1.0f, true);
		Texture east = TextureManager.loadTexture(
				ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE,
						currentLevel.getSkyboxTexturePath(skyboxSides.East)),
						Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear,
						Format.GuessNoCompression, 1.0f, true);
		Texture west = TextureManager.loadTexture(
				ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, 
						currentLevel.getSkyboxTexturePath(skyboxSides.West)),
						Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear,
						Format.GuessNoCompression, 1.0f, true);
		Texture up = TextureManager.loadTexture(
				ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, 
						currentLevel.getSkyboxTexturePath(skyboxSides.Up)),
						Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear,
						Format.GuessNoCompression, 1.0f, true);
		Texture down = TextureManager.loadTexture(
				ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, 
						currentLevel.getSkyboxTexturePath(skyboxSides.Down)),
						Texture.MinificationFilter.BilinearNearestMipMap, Texture.MagnificationFilter.Bilinear,
						Format.GuessNoCompression, 1.0f, true);

		
		skybox.setTexture(com.jme.scene.Skybox.Face.North, north);
		skybox.setTexture(com.jme.scene.Skybox.Face.South, south);
		skybox.setTexture(com.jme.scene.Skybox.Face.East, east);
		skybox.setTexture(com.jme.scene.Skybox.Face.West, west);
		skybox.setTexture(com.jme.scene.Skybox.Face.Up, up);
		skybox.setTexture(com.jme.scene.Skybox.Face.Down, down);
		
		skybox.preloadTextures();
		parent.attachChild(skybox);
		parent.updateRenderState();
	}

	private void buildTerrainVisual(EDLevel level, GameResourcePack levelResources, FogState fs) {
		TerrainPass tPass = new TerrainPass();
        tPass.setRenderMode(TerrainPass.MODE_BEST);
        
        ZBufferState buf = display.getRenderer().createZBufferState();
        buf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        
        CullState cs = display.getRenderer().createCullState();
        cs.setCullFace(Face.Back);
        cs.setEnabled(true);
        
        tPass.setPassState(buf);
        tPass.setPassState(cs);
        tPass.addTerrain(tb);
        
        tPass.setTileScale(level.splatBaseLayer.scale);	// FIXME: Hardcoded. Need to check exactly how it works.
        Texture baseTex = levelResources.getTexture(level.splatBaseLayer.texture);
        tPass.addDetail(baseTex, null, level.splatBaseLayer.scale);
        
        Texture lightmap = levelResources.getTexture("shadow");
        tPass.setLightmap(lightmap, Texture.CombinerScale.One);
        
        if(level.splatDetailLayers != null)
        	for( SplatLayer detail : level.splatDetailLayers) {
            	Texture detailTex = levelResources.getTexture(detail.texture);
            	Texture alphaTex = levelResources.getTexture(detail.mask);

            	if( (alphaTex != null) && (detailTex != null) ) {
            		tPass.addDetail(detailTex, alphaTex, detail.scale);
            	}
            }
        
        if(fs != null) tPass.setFog(fs);
        
        tb.updateRenderState();
        passManager.add(tPass);

	}
	
	private FogState buildFogState(float[] settings) {
		if( (settings == null) || (settings.length != 7) ) return null;
		// Array layout is: start, end, density, colorR, colorG, colorB, colorA}
		
		FogState fs = display.getRenderer().createFogState();
		fs.setStart(settings[0]);
		fs.setEnd(settings[1]);
		fs.setDensity(settings[2]);
		fs.setColor(new ColorRGBA(settings[3], settings[4], settings[5], settings[6]));
		fs.setEnabled(true);
		
		fs.setDensityFunction(DensityFunction.Linear);
		fs.setQuality(Quality.PerVertex);
		return fs;
	}
	
	private void createLights(EDLevel currentLevel, Node gameRoot) {
		if(currentLevel.lights == null) return;
		LightState ls = display.getRenderer().createLightState();
	    ls.setEnabled(true);
	    
		for(LightData data : currentLevel.lights) {
			Light light = null;
			switch(data.type) {
			case DIRECTIONAL: 
				DirectionalLight dl = new DirectionalLight();
				dl.setDirection(data.direction);
				light = dl;
				break;
			case POINT: 
				PointLight pl = new PointLight();
				pl.setLocation(data.location);
				light = pl;
				break;
			case SPOT: 
				SpotLight sl = new SpotLight();
				sl.setDirection(data.direction);
				sl.setLocation(data.location);
				light = sl;
				break;
			}
			if(light != null) {
				if(data.ambient != null) light.setAmbient(data.ambient);
				if(data.diffuse != null) light.setDiffuse(data.diffuse);
				if(data.specular != null) light.setSpecular(data.specular);
				light.setEnabled(true);
				ls.attach(light);
			}
		}
		
		gameRoot.setRenderState(ls);
	    gameRoot.updateRenderState();
	}
	
	/**************** Water stuff begins *******************/
	public final float MAXWATERDISTANCE = 3000f;
	
    private Quad waterQuad;
    private float waterHeight = 0f;
    private float textureScale = 0.025f;//0.07f;
	private WaterRenderPass waterEffectRenderPass;
    
	// TODO: The model must have some representation of the water, as you can collide with it.
    private void setupWater(float waterHeight, Node parent, Camera cam) {
        waterQuad = new Quad("waterQuad", 1, 1);
        waterQuad.setModelBound(new OrientedBoundingBox());
        waterQuad.updateModelBound();

        ZBufferState zbuf = display.getRenderer().createZBufferState();
        zbuf.setEnabled(true);
        zbuf.setFunction(ZBufferState.TestFunction.LessThanOrEqualTo);
        waterQuad.setRenderState(zbuf);
        
        this.waterHeight = waterHeight;
        
        FloatBuffer normBuf = waterQuad.getNormalBuffer();
        normBuf.clear();
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);
        normBuf.put(0).put(1).put(0);

        parent.attachChild(waterQuad);
        
		if(waterQuad == null) {System.err.println("Error: Could not setup water visual as model has no water.");}
		waterEffectRenderPass = new WaterRenderPass(cam, 6, false, false);	// Using the refraction shader causes a crash in native code on some machines.
        waterEffectRenderPass.setWaterPlane(new Plane(new Vector3f(0.0f, 1.0f,
                0.0f), 0.0f));
        waterEffectRenderPass.setClipBias(-1.0f);
        waterEffectRenderPass.setReflectionThrottle(0.0f);
        waterEffectRenderPass.setRefractionThrottle(0.0f);
        waterEffectRenderPass.setWaterHeight(waterHeight);
        
        waterEffectRenderPass.setWaterEffectOnSpatial(waterQuad);        
        waterEffectRenderPass.setReflectedScene(skybox);
        //waterEffectRenderPass.addReflectedScene(reflectionTerrain);
        waterEffectRenderPass.setSkybox(skybox);
        passManager.add(waterEffectRenderPass);
    }
        
    public void updateWater(Camera cam) {
        Vector3f transVec = new Vector3f(cam.getLocation().x,
                waterHeight, cam.getLocation().z);
        setTextureCoords(0, transVec.x, -transVec.z, textureScale);
        setVertexCoords(transVec.x, transVec.y, transVec.z);
        waterQuad.updateModelBound();
    }
    
    private void setVertexCoords(float x, float y, float z) {
        FloatBuffer vertBuf = waterQuad.getVertexBuffer();
        vertBuf.clear();

        vertBuf.put(x - MAXWATERDISTANCE).put(y).put(z - MAXWATERDISTANCE);
        vertBuf.put(x - MAXWATERDISTANCE).put(y).put(z + MAXWATERDISTANCE);
        vertBuf.put(x + MAXWATERDISTANCE).put(y).put(z + MAXWATERDISTANCE);
        vertBuf.put(x + MAXWATERDISTANCE).put(y).put(z - MAXWATERDISTANCE);
    }

    private void setTextureCoords(int buffer, float x, float y,
            float textureScale) {
        x *= textureScale * 0.5f;
        y *= textureScale * 0.5f;
        textureScale = MAXWATERDISTANCE * textureScale;
        FloatBuffer texBuf;
        texBuf = waterQuad.getTextureCoords(buffer).coords;
        texBuf.clear();
        texBuf.put(x).put(textureScale + y);
        texBuf.put(x).put(y);
        texBuf.put(textureScale + x).put(y);
        texBuf.put(textureScale + x).put(textureScale + y);
    }
	/**************** Water stuff ends *****************/
    
    /******************************** Cleanup methods below ******************************/
	private void cleanupSkyBox() {
		if(skybox == null) return;
		
		skybox.deleteTextures();
		
		skybox.detachAllChildren();
		skybox.removeFromParent();
		skybox = null;
	}
	
	public void cleanupLevel() {
		cleanupSkyBox();
		if(tb != null) tb.removeFromParent(); tb = null;
		if(waterQuad != null) waterQuad.removeFromParent(); waterQuad = null;
		if(passManager != null) passManager.clearAll();
	}
	
	public void cleanup() {
		display = null;
		
		if(loadingQuad != null) loadingQuad.removeFromParent(); loadingQuad = null;
		if(fader != null) fader.cleanup(); fader = null;
		
		if(passManager != null) passManager.clearAll(); passManager = null;
	}
}
