package com.calefay.utils;

public interface GameEntity extends GameMoveable, GameActionable {

	public boolean isDead();
	public String getName();
	public String getFaction();
}
