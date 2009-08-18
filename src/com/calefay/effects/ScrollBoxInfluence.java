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
package com.calefay.effects;

import java.io.IOException;

import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.util.export.InputCapsule;
import com.jme.util.export.JMEExporter;
import com.jme.util.export.JMEImporter;
import com.jme.util.export.OutputCapsule;
import com.jmex.effects.particles.Particle;
import com.jmex.effects.particles.ParticleInfluence;
import com.jmex.effects.particles.ParticleSystem;

public class ScrollBoxInfluence extends ParticleInfluence {
    
    public static final float DEFAULT_MAX_X = 100f;
    public static final float DEFAULT_MAX_Y = 100f;
    public static final float DEFAULT_MAX_Z = 100f;
    
    private float maxX = DEFAULT_MAX_X;
    private float maxY = DEFAULT_MAX_Y;
    private float maxZ = DEFAULT_MAX_Z;
    
    private float sizeX = maxX * 2;
    private float sizeY = maxY * 2;
    private float sizeZ = maxZ * 2;
    
    private Camera cam = null;
    
    private Vector3f workVect = new Vector3f();
    
    public ScrollBoxInfluence(Camera cam, float maxDistX, float maxDistY, float maxDistZ) {
    	setCamera(cam);
    	setMaxDistances(maxDistX, maxDistY, maxDistZ);
    }
    
    @Override
    public void prepare(ParticleSystem particleGeom) {
        super.prepare(particleGeom);
    }
    
    @Override
    public void apply(float dt, Particle particle, int index) {
    	particle.getPosition().subtract(cam.getLocation(), workVect);
    	
    	if(workVect.x > maxX) particle.getPosition().x -= sizeX;
    	if(workVect.y > maxY) particle.getPosition().y -= sizeY;
    	if(workVect.z > maxZ) particle.getPosition().z -= sizeZ;
    	if(workVect.x < -maxX) particle.getPosition().x += sizeX;
    	if(workVect.y < -maxY) particle.getPosition().y += sizeY;
    	if(workVect.z < -maxZ) particle.getPosition().z += sizeZ;
    }
    
    public void setCamera(Camera cam) {
    	this.cam = cam;
    }
    
    public void setMaxDistances(float maxDistX, float maxDistY, float maxDistZ) {
    	this.maxX = maxDistX; this.maxY = maxDistY; this.maxZ = maxDistZ;
    	this.sizeX = maxDistX * 2; this.sizeY = maxDistY * 2; this.sizeZ = maxDistZ * 2;
    }
    
    @Override
    public void write(JMEExporter e) throws IOException {
        super.write(e);
        OutputCapsule cap = e.getCapsule(this);
        cap.write(maxX, "sizeX", DEFAULT_MAX_X);
        cap.write(maxY, "sizeY", DEFAULT_MAX_Y);
    }
    
    @Override
    public void read(JMEImporter e) throws IOException {
        super.read(e);
        InputCapsule cap = e.getCapsule(this);
        maxX = cap.readFloat("maxX", DEFAULT_MAX_X);
        maxY = cap.readFloat("maxY", DEFAULT_MAX_Y);
    }


}
