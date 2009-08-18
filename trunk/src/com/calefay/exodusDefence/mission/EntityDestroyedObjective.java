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
package com.calefay.exodusDefence.mission;

import com.calefay.utils.GameEntity;
import com.calefay.utils.GameEvent;

public class EntityDestroyedObjective extends EDBaseObjective {
	
	private String entityName = null;
	
	public EntityDestroyedObjective(String entityName) {
		super();
		
		this.entityName = entityName;
	}
	
	public String getLabel() {return "Destroy target";}
	
	public void receiveEvent(GameEvent e) {
		super.receiveEvent(e);
		if(isComplete() || (entityName == null) || (e == null) ) return;
		
		if(e.getEventType().equalsIgnoreCase("EntityDestroyed")) {
			GameEntity entity = (GameEntity)e.getEventInitiator();
			if( (entity != null) && entityName.equals(entity.getName()) ) {
				progress();
			}
		}
	}
	
	public void cleanup() {
		super.cleanup();
		entityName = null;
	}
}