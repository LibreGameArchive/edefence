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
package com.calefay.utils;

import com.calefay.exodusDefence.EDParticleManager;
import com.jme.scene.Node;

public class GameWorldInfo {
	// Probably should get rid of that is it might be considered evil!
	/* Stores all the information that classes lower down the tree might need to know about their world.
	 * Provides getters and setters for those fields. This means that they don't need to be passed all those values.
	 * It also allows the references to be cleared easily to free up objects for the garbage collector.
	 */
	
	private static GameWorldInfo currentWorldInfo = null;
	private Node rootNode = null;
	private GameEventHandler masterEventHandler = null;
	private EDParticleManager particleManager = null;
	
	public GameWorldInfo(Node root, 
						GameEventHandler geHandler) {
		rootNode = root;
		masterEventHandler = geHandler; 
		currentWorldInfo = this;
	}
	
	/** sets the instance which will be returned by GameWorldInfo.getGameWorldInfo() */
	public static void setDefault(GameWorldInfo defaultWorldInfo) {currentWorldInfo = defaultWorldInfo;}
	public static GameWorldInfo getGameWorldInfo() {return currentWorldInfo;}
	public void setRootNode(Node root) {this.rootNode = root;}
	public Node getRootNode() {return rootNode;}
	public GameEventHandler getEventHandler() {return masterEventHandler;}
	public void setEventHandler(GameEventHandler g) {masterEventHandler = g;}
	public EDParticleManager getParticleManager() {return particleManager;}
	public void setParticleManager(EDParticleManager particleManager) {this.particleManager = particleManager;}
	
}
