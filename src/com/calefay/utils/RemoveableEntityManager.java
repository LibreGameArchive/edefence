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
import java.util.Iterator;

public class RemoveableEntityManager {
	/* Provides a bucket to throw stuff into so that it will get updated, and removed when it becomes inactive.
	 * Simply adds items when told to and removes them when they become inactive. No reuse. Not usually necessary to subclass.
	 */

	private ArrayList<GameRemoveable> entities;
	private ArrayList<GameRemoveable> newEntities;
	private boolean updating = false;
	
	public RemoveableEntityManager() {
		entities = new ArrayList<GameRemoveable>();
		newEntities = new ArrayList<GameRemoveable>();
	}
	 
	public void updateEntities(float interpolation) {
		updating = true;
		for (Iterator<GameRemoveable> it = entities.iterator(); it.hasNext();) {	// Look into the performance of using a deletions array instead of an iterator.
			GameRemoveable gu = it.next();
			if (gu.isActive()) {
				gu.update(interpolation);
			} else {
				gu.cleanup();
				it.remove();
			}
		}
		
		if(!newEntities.isEmpty()) {	// This is to prevent concurrent modification exception if one of the entities adds another entity during the update cycle.
			entities.addAll(newEntities);
			newEntities.clear();
		}
		
		updating = false;
	}
	
	public void add(GameRemoveable e) {	// TODO: Check it isn't already there
		if(e != null) {
			if(updating) {newEntities.add(e);} else {entities.add(e);}
		}
	}
	
	public GameRemoveable get(int i) {
		if(i < entities.size())
			return entities.get(i);
		else
			return null;
	}
	
	public int getSize() {
		return entities.size();
	}
	
	/** Clears out the entity list. NOTE: This WILL invoke the cleanup method of every entity, not just remove them from the list.*/
	public void clearEntities() {
		for(GameRemoveable gr : entities) {gr.cleanup();}
		entities.clear();
		
		for(GameRemoveable gr : newEntities) {gr.cleanup();}
		newEntities.clear();
	}
	
	public void cleanup() {
		clearEntities();
		entities = null; newEntities = null;
		updating = false;
	}
	
	// Debugging/Informational methods
	public void printEntityList() {
		System.out.println("********** RemoveableEntityManager list begins **********");
		for(GameRemoveable g : entities) {
			System.out.println(g.toString());
		}
		System.out.println("********** RemoveableEntityManager list ends **********");
	}
}
