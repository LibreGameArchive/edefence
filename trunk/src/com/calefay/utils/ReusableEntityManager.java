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
import java.util.List;

public class ReusableEntityManager {
	/* Provides a bucket to throw stuff into so that it will get updated, and removed when it becomes inactive.
	 * Intended to be subclassed and used to manage transitory entities, that it might be more effecient to reuse than recreate.
	 * The subclass should define a factory method to return an instance of the particular class being managed. 
	 * The instance should be obtained using getEntity, then reset if possible, otherwise using new and addEntity.
	 */

	protected List<GameReuseable> entities;
	protected int maxCacheSize = 0;	// This determines the maximum number of inactive entities that will be retained before deletion.
	protected int cachedEntities = 0; // Stores the number of inactive entities actually in memory at the current moment in time.
	
	protected ReusableEntityManager(int maxCachedEntities) {
		entities = new ArrayList<GameReuseable>();
		maxCacheSize = maxCachedEntities;
		cachedEntities = 0;
	}
	 
	public void updateEntities(float interpolation) {	

		// TODO is using an iterator here really a good idea? Benchmark this manager against just adding and deleting array entries on demand.
		for (Iterator<GameReuseable> it = entities.iterator(); it.hasNext(); ) {
			GameReuseable e = it.next();
			if (e.isDeactivating()) {
					if ( cachedEntities >= maxCacheSize) {
						it.remove();	// permanently delete this entity if we don't have room in cache.
					} else {
						cachedEntities++;
						e.setDeactivated();
					}
			} else {
				if (e.isActive()) {
					e.update(interpolation);
					} // end if (isActive)
				}
		}
	}
	
	/** Adds a new entity to the manager. Should be used for newly created objects NOT recycled ones (they're already in there).*/
	protected void addEntity(GameReuseable e) {
		entities.add(e);
	}
	
	/** This method will return an entity that is ready to be reset and reused, if one is available in cache, null if not.
	 *  Subclass should use this method to check for an existing entity, only creating a new one if needed.
	 *  Entity should ALWAYS be reset once it has been returned by this method.
	 */
	protected GameReuseable requestEntity() {
		GameReuseable g = null;
		
		for(GameReuseable e : entities) {
			if (!e.isActive()) {
				g = e;
				g.setDeactivating(); // Make sure that the object is deactivated again if not properly reset.
				cachedEntities--;
				break;	
			}
		}
		return g;
		
	}
	
}
