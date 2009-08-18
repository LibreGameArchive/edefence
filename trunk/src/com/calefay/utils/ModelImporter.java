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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;

import com.jme.scene.Node;
import com.jme.scene.Spatial;
import com.jme.util.export.binary.BinaryImporter;
import com.jme.util.resource.ResourceLocatorTool;
import com.jmex.model.converters.MaxToJme;
import com.jmex.model.converters.MilkToJme;
import com.jmex.model.converters.ObjToJme;

public class ModelImporter {
    
	public static Spatial loadJME(String filePath) {
		URL fileURL = ModelImporter.class.getClassLoader().getResource(filePath);
		return loadJME(fileURL);
	}
	
	public static Spatial loadJME(URL fileURL) {
		Spatial model = null;
        try {
            BinaryImporter importer = new BinaryImporter();
            model = (Spatial)importer.load(fileURL.openStream());
            return model;
        } catch (IOException e) {
           System.err.println("ERROR: Failed to import jme model from URL: " + fileURL);
           e.printStackTrace();
           return null;
        }
	}
	
    public static Node loadMS3D(String filePath) {
        URL MSFile = ModelImporter.class.getClassLoader().getResource(filePath);
        return loadMS3D(MSFile);
    }
    
    /** Loads the Milkshape file specified. Always returns a Node.*/
    public static Node loadMS3D(URL fileURL) {
        Node file = null;
        
        ByteArrayOutputStream BO = new ByteArrayOutputStream();        
        if (fileURL == null) {
            System.err.println("ERROR - Unable to find milkshape file");
        } else {
            MilkToJme convert = new MilkToJme();
            try {
                convert.convert(fileURL.openStream(), BO);
                file = (Node)BinaryImporter.getInstance().load(new ByteArrayInputStream(BO
                        .toByteArray()));
            } catch (IOException e) {
                System.err.println("ERROR - Exception in Milkshape conversion: " + e);
            }
        }

        return file;
    }
    
    public static Node locateLoadMS3D(String file) {
        return loadMS3D(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL,file));
    }
    
    public static Node locateLoad3DS(String file) {
        return load3DS(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL,file));
    }
    
    public static Spatial locateLoadJME(String file) {
        return loadJME(ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_MODEL,file));
    }
    
    public static Node load3DS(String filePath) {
        URL file = ModelImporter.class.getClassLoader().getResource(filePath);
        return load3DS(file);
    }
    
    public static Node load3DS(URL modelURL) {
        if(modelURL == null) {System.err.println("3DS Loading error: URL was null."); return null;}
        Node r1 = null;
        try {
            MaxToJme C1 = new MaxToJme();
            ByteArrayOutputStream BO = new ByteArrayOutputStream();
            C1.convert(new BufferedInputStream(modelURL.openStream()), BO);
            r1 = (Node)BinaryImporter.getInstance().load(new ByteArrayInputStream(BO.toByteArray()));
            if (r1.getChild(0).getControllers().size() != 0) r1.getChild(0).getController(0).setSpeed(20);    // What is this?!
        } catch (IOException e) {
            System.err.println("Failed to load 3DS file" + e);
        }
        return r1;
    }
    
    public static Node loadObj(URL modelURL) {
        ObjToJme converter=new ObjToJme();
        try {
            converter.setProperty("mtllib",modelURL);
            ByteArrayOutputStream BO=new ByteArrayOutputStream();   
            converter.convert(modelURL.openStream(),BO);
            Node r=(Node)BinaryImporter.getInstance().load(new ByteArrayInputStream(BO.toByteArray()));
            return r;
        } catch (IOException e) {
            System.err.println("Failed to load Obj file" + e);
        }
        return null;
    }
}
