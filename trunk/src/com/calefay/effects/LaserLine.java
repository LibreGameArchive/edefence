package com.calefay.effects;

import com.jme.scene.shape.Quad;

public class LaserLine extends Quad {
	private static final long serialVersionUID = 1L;

	public LaserLine(String name, float width, float height) {
		super(name,width,height);
		this.getVertexBuffer().clear();
		this.getVertexBuffer().put(-width / 2f).put(0).put(height);
		this.getVertexBuffer().put(-width / 2f).put(0).put(0);
		this.getVertexBuffer().put(width / 2f).put(0).put(0);
		this.getVertexBuffer().put(width / 2f).put(0).put(height);
	}
}
