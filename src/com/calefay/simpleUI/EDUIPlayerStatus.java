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


import com.calefay.exodusDefence.PlayableEntity;
import com.calefay.exodusDefence.weapons.EDBeamGun;
import com.calefay.exodusDefence.weapons.Gun;
import com.calefay.utils.GameRemoveable;
import com.jme.image.Texture;
import com.jme.image.Texture.ApplyMode;
import com.jme.image.Texture.CombinerScale;
import com.jme.scene.state.TextureState;
import com.jme.system.DisplaySystem;

// Really don't like having this be a GameRemoveable and doing update; it is logically event driven. Ideally it should be a listener on the entity.
public class EDUIPlayerStatus extends UIContainer implements GameRemoveable {

	protected UIProgressBar healthBar = null;
	protected UIProgressBar temperatureBar = null;
	protected UITextureContainer icon = null;
	protected PlayableEntity entity = null;
	
	protected float updateTimer = 0;
	protected float lastHP = 0;
	
	public EDUIPlayerStatus(String name, PlayableEntity t,
							Texture entityIcon, Texture numberOverlay,
							TextureState healthGradient, TextureState heatGratient,
							int sizeX, int sizeY,
							int posX, int posY, int zOrder) {
		
		super(name, posX, posY, zOrder, sizeX, sizeY);
		// FIXME: Get rid of the hidden gl call.
		TextureState ts = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
		ts.setEnabled(true);
		
		ts.setTexture(entityIcon, 0);
		
		if(numberOverlay != null) {
			numberOverlay.setCombineSrc0RGB(Texture.CombinerSource.CurrentTexture);
			numberOverlay.setCombineOp0RGB(Texture.CombinerOperandRGB.SourceColor);
			numberOverlay.setCombineOp1RGB(Texture.CombinerOperandRGB.SourceColor);
			numberOverlay.setCombineScaleRGB(CombinerScale.One);
				
			numberOverlay.setApply(ApplyMode.Add);
			numberOverlay.setCombineFuncRGB(Texture.CombinerFunctionRGB.AddSigned);
			numberOverlay.setCombineSrc1RGB(Texture.CombinerSource.Previous);
			ts.setTexture(numberOverlay, 1);
		}
		
		icon = new UITextureContainer(name + "icon", sizeX, sizeY, 0, 0, zOrder, this);
		//icon.visibleQuad.copyTextureCoords(0, 0, 1);
		icon.visibleQuad.copyTextureCoordinates(0, 1, 1.0f); // FIXME: visibleQuad shouldn't be well.. visible - from here	
		icon.setTextureState(ts);
		setTransparency(0.5f);
		healthBar = new UIProgressBar(name + "healthbar", healthGradient, 
										80, 4, 64, 6, 85, 
										t.getMaxHitpoints(), 1, t.getMaxHitpoints(), this);
		temperatureBar = new UIProgressBar(name + "healthbar", heatGratient, 
										80, 4, 64, 14, 85, 
										100, 1, 100, this);
		
		entity = t;
		lastHP = entity.getHitpoints();
		updateTimer = 0;
		active = true;
	}
	
	public void setTransparency(float t) {icon.setTransparency(t);}
	
	public void update(float interpolation) {
		updateTimer += interpolation;
		if(updateTimer < 0.1) return;	// Only process every tenth of a second;
		updateTimer = 0;
		
		if(!entity.isActive()) {	// FIXME: Ideally this should be a listener on the entity so that it removes itself immediately the entity is destroyed.
			deactivate();
			return;
		}
		
		Gun g = entity.getPrimaryGunBarrel1();
		if(g instanceof EDBeamGun) { // FIXME: EWW THIS IS HORRIBLE@!~
			EDBeamGun gun = (EDBeamGun)g;
			temperatureBar.setProgress( (gun.getTemperature() / gun.getMaxTemperature()) * 100);
		}
		float h = entity.getHitpoints();
		if(h != lastHP) {healthBar.setProgress(h); lastHP = h;}
	}
	
	public void deactivate() {
		active = false;
	}
	
	public void cleanup() {
		active = false;
		entity = null;
		if(icon != null) {icon.cleanup(); icon = null;}
		if(healthBar != null) {healthBar.cleanup(); healthBar = null;}
		if(temperatureBar != null) {temperatureBar.cleanup(); temperatureBar = null;}
	}
	
}
