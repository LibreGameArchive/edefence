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

public interface GameRemoveable {
	
	public boolean isActive();
	
	/** Used both by the manager class and by the user to deactivate an object and make it available for recycling.
	 * Must set deactivating to true. 
	 * May do whatever else the implementation requires, such as detaching the node.
	 * Should allow visibility that it is being deactivated so that references can be removed and only cleanup resources on the next update.
	 */
	public void deactivate();
	
	/** Should release all resources and reset fields, ready for garbage collection.
	 * Assumes that references to it have been/will be cleared down separately.*/
	public void cleanup();
	
	// TODO: Create a system for updating at a given interval rather than every frame.
	public void update(float interpolation);
	
}
