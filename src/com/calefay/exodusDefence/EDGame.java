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

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import com.calefay.utils.GameEvent;
import com.calefay.utils.GameEventHandler;
import com.calefay.utils.GameEventHandler.GameEventListener;
import com.jme.renderer.ColorRGBA;
import com.jme.system.DisplaySystem;
import com.jme.system.JmeException;

public class EDGame extends EDBaseGame {
	
	private static final String DEFAULT_RENDERER = "LWJGL";
	
	private GameEventHandler eventHandler = null;
	private GameEventListener systemListener = null;
	
	private EDGameDelegate delegate = null;
	
	private boolean recoveringFromError = false, recoveryCycle = false;
	
	@Override
	protected void initGame() {
	    eventHandler = new GameEventHandler();
	    systemListener = eventHandler.addListener("SYSTEM", 500);
		delegate.initGame(eventHandler);
		
		recoveringFromError = false; recoveryCycle = false;
	}

	@Override
	protected void initSystem() {
		EDExceptionHandler handler = new EDExceptionHandler();
		handler.registerGame(this);
		setThrowableHandler(handler);
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = ge.getDefaultScreenDevice();
		
		int width = device.getDisplayMode().getWidth(); int height = device.getDisplayMode().getHeight();
		int bpp = device.getDisplayMode().getBitDepth(); int refreshRate = device.getDisplayMode().getRefreshRate();
		width = 800; height = 600; bpp = 24; refreshRate = 60;
		System.out.println("Setting up display: " + width + "x" + height + "x" + bpp + "@" + refreshRate);
		
		try {
			display = DisplaySystem.getDisplaySystem(DEFAULT_RENDERER);
			display.createWindow(width, height, bpp, refreshRate, false);
	
		} catch (JmeException e) {
			e.printStackTrace();
			System.exit(1);
		}
	
		display.setTitle("Exodus Defence");
		display.setVSyncEnabled(true);
		display.getRenderer().setBackgroundColor(ColorRGBA.black);	
		
		delegate = new EDGameDelegate(false, width, height, bpp, refreshRate);
		delegate.initSystem();
	}

	@Override
	protected void reinit() {
		EDPreferences menuSettings = delegate.menuSettings;
		if(menuSettings == null) {System.err.println("ERROR: Could not reinit() as no settings are available.");}
		
		try {
			display.recreateWindow(menuSettings.getWidth(), menuSettings.getHeight(), 
					menuSettings.getBitsPerPixel(), menuSettings.getRefreshRate(), menuSettings.getFullscreen());
			if(!display.isFullScreen()) display.moveWindowTo(0, 0);
		} catch (JmeException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		delegate.reinit();
	}
	
	@Override
	protected void render(float interpolation) {
		try {
			delegate.render(interpolation);
		}  catch(Exception e) {
			recover(e);
			// TODO: Ideally check the display is intact and recreate it if not.
		}
	}

	@Override
	protected void update(float interpolation) {
		recoveryCycle = recoveringFromError; recoveringFromError = false;
		try {
			delegate.update(interpolation);
			processSystemEvents();
		} catch(Exception e) {
			recover(e);
		}
	}
	
	private void recover(Throwable e) {
		System.out.println("Error in game loop.");
		e.printStackTrace();
		if(recoveryCycle) {
			System.out.println("Error during recovery cycle. Closing down.");
			finished = true;
		} else {
			System.out.println("Attempting recovery.");
			eventHandler.flush();
			delegate.fullRecover(eventHandler);
			reinit();
		}
		recoveringFromError = true; recoveryCycle = true;
		
		processSystemEvents();
	}
	
	private void processSystemEvents() {
		GameEvent e = systemListener.getNextEvent();
		while(e != null) {
			handleSystemEvent(e);	// TODO: Would like a way of securing this so that scripts can't send system events.
			e = systemListener.getNextEvent();
		}
	}
	
	protected void handleSystemEvent(GameEvent e) {
		if(e.getEventType().equalsIgnoreCase("quittodesktop")) {
			finished = true;
		} else if(e.getEventType().equalsIgnoreCase("displayreset")) {
			reinit();
		} else delegate.handleSystemEvent(e);
	}
	
	@Override
	protected void cleanup() {
		System.out.println("Cleaning up game resources.");
		if(systemListener != null) systemListener.flushEvents(); systemListener = null;
		if(delegate != null) delegate.cleanup(); delegate = null;
		if(eventHandler != null) eventHandler.cleanup(); eventHandler = null;
	}
	
	/*********************************** Main method below ********************************/
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		EDGame app = new EDGame();
		app.start();
	}
}
