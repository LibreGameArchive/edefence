package com.calefay.exodusDefence;

import com.jme.util.ThrowableHandler;

public class EDExceptionHandler implements ThrowableHandler {

	private EDBaseGame game = null;
	private EDAppletGame applet = null;
	
	public void registerGame(EDGame game) {this.game = game;}
	public void registerGame(EDAppletGame game) {this.applet = game;}
	
	public void handle(Throwable t) {
		System.out.println("Exception in game loop: ");
		t.printStackTrace();
		if(game != null) game.cleanup();
		if(applet != null) applet.cleanup();
	}

}
