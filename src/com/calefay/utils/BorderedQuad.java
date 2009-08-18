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

import java.nio.FloatBuffer;

import com.jme.math.Vector3f;
import com.jme.scene.TexCoords;
import com.jme.scene.TriMesh;
import com.jme.util.geom.BufferUtils;

public class BorderedQuad extends TriMesh {

	private static final long serialVersionUID = 1L;

    public BorderedQuad() {this("borderquad");}

	public BorderedQuad(String name) {this(name, 1, 1);}

	public BorderedQuad(String name, float width, float height) {
		super(name);
		initialize(width, height);
	}
	
	public BorderedQuad(String name, float width, float height, float borderSize) {
		super(name);
		initialize(width, height, borderSize);
	}

	public void initialize(float width, float height) {
		this.initialize(width, height, 0.1f * width);
	}
	
	public void initialize(float width, float height, float bSize) {
		setVertexCount(16);
		setVertexBuffer(BufferUtils.createVector3Buffer(getVertexCount()));
		setNormalBuffer(BufferUtils.createVector3Buffer(getVertexCount()));
        FloatBuffer tbuf = BufferUtils.createVector2Buffer(getVertexCount());
        setTextureCoords(new TexCoords(tbuf));
	    setTriangleQuantity(18);
	    setIndexBuffer(BufferUtils.createIntBuffer(getTriangleCount() * 3));
	    
		getVertexBuffer().put( (-width / 2f) - bSize).put( (height / 2f) + bSize).put(0);
		getVertexBuffer().put(-width / 2f).put( (height / 2f) + bSize).put(0);
		getVertexBuffer().put(width / 2f).put( (height / 2f) + bSize).put(0);
		getVertexBuffer().put( (width / 2f) + bSize).put( (height / 2f) + bSize).put(0);
		
		getVertexBuffer().put( (-width / 2f) - bSize).put(height / 2f).put(0);
		getVertexBuffer().put(-width / 2f).put( (height / 2f)).put(0);
		getVertexBuffer().put(width / 2f).put( (height / 2f)).put(0);
		getVertexBuffer().put( (width / 2f) + bSize).put(height / 2f).put(0);

		getVertexBuffer().put( (-width / 2f) - bSize).put(-height / 2f).put(0);
		getVertexBuffer().put(-width / 2f).put( (-height / 2f)).put(0);
		getVertexBuffer().put(width / 2f).put( (-height / 2f)).put(0);
		getVertexBuffer().put( (width / 2f) + bSize).put(-height / 2f).put(0);
		
		getVertexBuffer().put( (-width / 2f) - bSize).put( (-height / 2f) - bSize).put(0);
		getVertexBuffer().put(-width / 2f).put( (-height / 2f) - bSize).put(0);
		getVertexBuffer().put(width / 2f).put( (-height / 2f) - bSize).put(0);
		getVertexBuffer().put( (width / 2f) + bSize).put( (-height / 2f) - bSize).put(0);
		
		
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);
		getNormalBuffer().put(0).put(0).put(1);

        
		tbuf.put(0).put(1);
        tbuf.put(0.5f).put(1);
        tbuf.put(0.5f).put(1);
        tbuf.put(1).put(1);
        
        tbuf.put(0).put(0.5f);
        tbuf.put(0.5f).put(0.5f);
        tbuf.put(0.5f).put(0.5f);
        tbuf.put(1).put(0.5f);
        
        tbuf.put(0).put(0.5f);
        tbuf.put(0.5f).put(0.5f);
        tbuf.put(0.5f).put(0.5f);
        tbuf.put(1).put(0.5f);
        
        tbuf.put(0).put(0);
        tbuf.put(0.5f).put(0);
        tbuf.put(0.5f).put(0);
        tbuf.put(1).put(0);

	    getIndexBuffer().put(4);
	    getIndexBuffer().put(1);
	    getIndexBuffer().put(0);
	    getIndexBuffer().put(4);
	    getIndexBuffer().put(5);
	    getIndexBuffer().put(1);
	    
	    getIndexBuffer().put(5);
	    getIndexBuffer().put(2);
	    getIndexBuffer().put(1);
	    getIndexBuffer().put(5);
	    getIndexBuffer().put(6);
	    getIndexBuffer().put(2);
	    
	    getIndexBuffer().put(6);
	    getIndexBuffer().put(3);
	    getIndexBuffer().put(2);
	    getIndexBuffer().put(6);
	    getIndexBuffer().put(7);
	    getIndexBuffer().put(3);
	    
	    getIndexBuffer().put(8);
	    getIndexBuffer().put(5);
	    getIndexBuffer().put(4);
	    getIndexBuffer().put(8);
	    getIndexBuffer().put(9);
	    getIndexBuffer().put(5);
	    
	    getIndexBuffer().put(9);
	    getIndexBuffer().put(6);
	    getIndexBuffer().put(5);
	    getIndexBuffer().put(9);
	    getIndexBuffer().put(10);
	    getIndexBuffer().put(6);
	    
	    getIndexBuffer().put(10);
	    getIndexBuffer().put(7);
	    getIndexBuffer().put(6);
	    getIndexBuffer().put(10);
	    getIndexBuffer().put(11);
	    getIndexBuffer().put(7);
	    
	    getIndexBuffer().put(12);
	    getIndexBuffer().put(9);
	    getIndexBuffer().put(8);
	    getIndexBuffer().put(12);
	    getIndexBuffer().put(13);
	    getIndexBuffer().put(9);
	    
	    getIndexBuffer().put(13);
	    getIndexBuffer().put(10);
	    getIndexBuffer().put(9);
	    getIndexBuffer().put(13);
	    getIndexBuffer().put(14);
	    getIndexBuffer().put(10);
	    
	    getIndexBuffer().put(14);
	    getIndexBuffer().put(11);
	    getIndexBuffer().put(10);
	    getIndexBuffer().put(14);
	    getIndexBuffer().put(15);
	    getIndexBuffer().put(11);
	}

	public Vector3f getCenter() {
		return worldTranslation;
	}
}
