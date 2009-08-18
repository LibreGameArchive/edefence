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
import java.net.URL;

import com.jme.renderer.Renderer;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.util.export.Savable;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.effects.particles.ParticleSystem;

public class LoadParticleEffect {
	
	public static Node loadParticleEffect(String effect) {
		return loadParticleEffect(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_TEXTURE, effect) );
	}
	/** Returns a Node on successful load, null on fail. If the jme file has only a ParticleMesh, it will be attached to a new Node.*/
	public static Node loadParticleEffect(URL fileURL) {

		Savable l = null;
		try {
			BinaryImporter binaryImporter = new BinaryImporter(); // Used to convert the jme usable file to a Node
			l = binaryImporter.load(fileURL);
		} catch(Exception err) {
	         System.out.println("Error loading .jme file:" + err);
	         System.out.println("URL was: " + fileURL);
	         return null;	// Could allow it to return an empty node, less harsh on errors that way.
	      }

		Node particleNode = new Node();
		if( (l instanceof Node) && !(l instanceof ParticleSystem) ) {
			particleNode = (Node)l;
			} else {
				ParticleSystem p = (ParticleSystem)l;
				particleNode = new Node("particleEffectNode");
				particleNode.attachChild(p);
				}
		
		particleNode.setRenderQueueMode(Renderer.QUEUE_TRANSPARENT);
		
		return particleNode;
	}

	// crude - can improve when this is a manager class.
	public static void respawnParticleNode(Node pNode) {
		for(Spatial s : pNode.getChildren() ) {
	          ParticleSystem pG = (ParticleSystem)s;
			  pG.forceRespawn();
			  }
	}
	}
