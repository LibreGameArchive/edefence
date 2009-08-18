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

/** GameReuseable is implemented in order to allow reuseable object managers to deactivate and recycle objects. 
 * @author James Waddington*/
public abstract class GameReuseable implements GameRemoveable {
	protected boolean active = false; /** should only be set by internal methods, to ensure that other deactivation tasks are completed also.*/
	protected boolean deactivating = false;
	
	public boolean isActive() {
		return active;
	}
	
	/** Should only be used by the entity manager. Intended to ensure that a recycled object is deactivated if not properly reset. */
	protected void setDeactivating() {
		deactivating = true;
	}
	
	protected boolean isDeactivating() {
		return deactivating;
	}
	
	/** Should only be called by the entity manager once it has handled it's side of deactivation.
	 * @see deactivate()
	 */
	protected void setDeactivated() {
		deactivating = false;
		active = false;
	}
	
	/** Used by the manager class to recycle a deactivated object instead of creating a new one.
	 * Must reset all instance variables that differentiate a specific object, in the same way as a the constructor would.
	 * Will usually have similar or the same parameters as the equivalent constructor.
	 * Should not usually have to instantiate and load contents, as that should have been done when it was originally created.
	 */
	protected abstract void reset();
}
