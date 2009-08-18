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

import java.util.ArrayList;

import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.Controller;
import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.scene.Text;
import com.jme.scene.Spatial.CullHint;
import com.jme.scene.Spatial.LightCombineMode;
import com.jme.scene.Spatial.TextureCombineMode;
import com.jme.scene.state.LightState;
import com.jme.scene.state.TextureState;

public class DebugMethods {

	public static ArrayList<Text> text = null;
	
	public static void listNodeChildren(Node n) {
		if(n == null) {System.err.println("printNodeChildren: Node provided was null."); return;}
		if(n.getQuantity() <= 0) {System.out.println("printNodeChildren: Node " + n.getName() + " has no children."); return;}
		System.out.println("********** Child list begins for node " + n.getName() + " **********");
		for(Spatial s : n.getChildren()) {
			System.out.println(s);
			if(s instanceof Node) listNodeChildren((Node)s);
		}
		System.out.println("********** Node child list ends (" + n.getName() + ") **********");
	}
	
	public static void listControllers(Node n) {
		System.out.println("Node: " + n);
		for(Controller c : n.getControllers()) {
			System.out.println("->Controller: " + c);
		}
		for(Spatial s : n.getChildren()) {
			System.out.println("-->Child: " + s);
			if(s instanceof Node) {
				Node childNode = (Node)s;
				listControllers(childNode);
			} else {
				listControllers(s);
			}
		}
	}
	
	public static void listControllers(Spatial n) {
		System.out.println("Spatial: " + n);
		for(Controller c : n.getControllers()) {
			System.out.println("->Controller: " + c);
		}
	}
	
	/** Adds a Text object to the root node (taken from GameWorldInfo). It is returned if you want to store it locally, or can be retrieved from DebugMethods.text*/
	public static Text addText(int yPos) {
		if(text == null) text = new ArrayList<Text>();
		
		Text t = Text.createDefaultTextLabel("Text", "Readout");
        t.setCullHint(CullHint.Never);
        t.setTextureCombineMode(TextureCombineMode.Replace);
        t.setLocalTranslation(new Vector3f(1, yPos, 0));
        t.setLightCombineMode(LightCombineMode.Off);
        t.setSolidColor(ColorRGBA.red);
        t.setRenderQueueMode(Renderer.QUEUE_ORTHO);
        t.setZOrder(0);
        GameWorldInfo.getGameWorldInfo().getRootNode().attachChild(t);
        text.add(t);
        return t;
	}
}
