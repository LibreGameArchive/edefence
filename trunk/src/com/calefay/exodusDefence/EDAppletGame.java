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

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;

import com.calefay.utils.GameEvent;
import com.calefay.utils.GameEventHandler;
import com.calefay.utils.GameEventHandler.GameEventListener;
import com.jme.renderer.ColorRGBA;
import com.jmex.awt.applet.BaseApplet;

public class EDAppletGame extends BaseApplet {
	
	private static final long serialVersionUID = 1L;
	
	private GameEventHandler eventHandler = null;
	private GameEventListener systemListener = null;
	
	private EDGameDelegate delegate = null;
	
	private boolean recoveringFromError = false, recoveryCycle = false;
	
	@Override
	protected void initGame() {
	    eventHandler = new GameEventHandler();
	    systemListener = eventHandler.addListener("SYSTEM", 500);
		delegate.initGame(eventHandler);
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
	
		display.setTitle("Exodus Defence");
		display.setVSyncEnabled(true);
		display.getRenderer().setBackgroundColor(ColorRGBA.black);	
		
		delegate = new EDGameDelegate(true, width, height, bpp, refreshRate);
		delegate.initSystem();
	}

	@Override
	protected void reinit() {
		EDPreferences menuSettings = delegate.menuSettings;
		if(menuSettings == null) {System.err.println("ERROR: Could not reinit() as no settings are available."); return;}
		
		if(menuSettings.getFullscreen() != isFullScreen()) {
			togglefullscreen();
			if(isFullScreen()) {
				Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
				menuSettings.setResolution(dimension.width, dimension.height);
				display.setWidth(dimension.width); display.setHeight(dimension.height);
			} else {
				menuSettings.setResolution(getWidth(), getHeight());
				display.setWidth(getWidth()); display.setHeight(getHeight());
			}
			delegate.reinit();
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
	public void destroy() {
		super.destroy();
		cleanup();
	}
	
	@Override
	protected void cleanup() {
		System.out.println("Cleaning up applet.");
		if(isFullScreen()) {
			System.out.println("Exiting fullscreen mode.");
			togglefullscreen();
		}
		if(systemListener != null) systemListener.flushEvents(); systemListener = null;
		if(delegate != null) delegate.cleanup(); delegate = null;
		if(eventHandler != null) eventHandler.cleanup(); eventHandler = null;
	}

}
