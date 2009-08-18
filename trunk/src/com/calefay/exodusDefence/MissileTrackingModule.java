package com.calefay.exodusDefence;

import com.calefay.exodusDefence.weapons.BaseProjectile;
import com.calefay.utils.GameEntity;


public abstract class MissileTrackingModule {
// Could be an interface right now.. ho hum
	public abstract void trackTarget(BaseProjectile missile, GameEntity target);
	
}
