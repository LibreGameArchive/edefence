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

public class EDPreferences {
	public enum SettingLMH {LOW, MEDIUM, HIGH};
	
	private boolean webView = false;

	private int width = 0;
	private int height = 0;
	private int bitsPerPixel = 0;
	private int refreshRate = 0;
	private boolean fullscreen = false;
	private SettingLMH textureDetail = SettingLMH.LOW;
	private SettingLMH particleDetail = SettingLMH.LOW;
	
	private boolean invertY = false;
	private boolean enableSound = false;
	private boolean loggingEnabled = false;

	public boolean narrativeBackgrounds = true;
	private int levelSelected = 0;
	
	public EDPreferences(boolean isWebView, int width, int height, int bitsPerPixel, int refreshRate, 
			boolean fullscreen, SettingLMH textureDetail, SettingLMH particleDetail) {
		invertY = false;
		setWebView(isWebView);
		setDisplaySettings(width, height, bitsPerPixel, refreshRate, fullscreen, textureDetail, particleDetail);
	}
	
	public void setDisplaySettings(int width, int height, int bitsPerPixel, int refreshRate, 
			boolean fullscreen, SettingLMH textureDetail, SettingLMH particleDetail) {
		this.width = width;
		this.height = height;
		this.bitsPerPixel = bitsPerPixel;
		this.refreshRate = refreshRate;
		this.fullscreen = fullscreen;
		this.textureDetail = textureDetail;
		this.particleDetail = particleDetail;
	}
	
	public void setResolution(int width, int height) {
		this.width = width;
		this.height = height;
	}
	
	public void setInvertY(boolean invert) {this.invertY = invert;}
	public void setLevelSelection(int l) {this.levelSelected = l;}
	public void setFullscreen(boolean fs) {fullscreen = fs;}
	public void setBitsPerPixel(int bpp) {bitsPerPixel = bpp;}
	public void setRefreshRate(int rr) {refreshRate = rr;}
	public void setTextureDetail(SettingLMH td) {textureDetail = td;}
	public void setParticleDetail(SettingLMH pd) {particleDetail = pd;}
	public void setSoundEnabled(boolean enableSound) {this.enableSound = enableSound;}
	public void setWebView(boolean webView) {this.webView = webView;}
	public void setLoggingEnabled(boolean enableLogging) {this.loggingEnabled = enableLogging;}
	
	public int getWidth() {return width;}
	public int getHeight() {return height;}
	public boolean getFullscreen() {return fullscreen;}
	public boolean getSoundEnabled() {return enableSound;}
	public int getBitsPerPixel() {return bitsPerPixel;}
	public int getRefreshRate() {return refreshRate;}
	public int getLevelSelection() {return levelSelected;}
	public SettingLMH getTextureDetail() {return textureDetail;}
	public SettingLMH getParticleDetail() {return particleDetail;}
	public boolean getInvertY() {return invertY;}
	public boolean showWebView() {return webView;}
	public boolean loggingEnabled() {return loggingEnabled;}
	
	public String toString() {
		String s = width + " x " + height;
		return s;
	}
}
