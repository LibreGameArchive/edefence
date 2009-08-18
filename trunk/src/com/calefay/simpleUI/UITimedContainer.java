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
package com.calefay.simpleUI;

import com.calefay.utils.GameRemoveable;

public class UITimedContainer extends UIContainer implements GameRemoveable {

	protected float life = 0;
	
	/** A simple extension of UIContainer which implements GameRemoveable so that it can be added to a RemoveableEntityManager.
	 * It has a lifetime specified in the constructor - after this time has passed the container will be automatically deleted.*/
	public UITimedContainer(String containerName, float lifeTime,
							int posX, int posY,
							int order, int sizeX, int sizeY) {
		super(containerName, posX, posY, order, sizeX, sizeY);
		life = lifeTime;
	}

	/** active status is a bit confusing at the moment as UIContainer and GameRemoveable have slightly different intentions with active.
	 *  A container is usually set inactive temporarily whenever it is not displayed, whereas a GameRemoveable is permanently removed when inactive.
	 *  As it will be removed by the manager, timed containers should not be set inactive unless it is permanent.*/
	public void deactivate() {
		setActive(false);
	}

	public void update(float interpolation) {
		if( !isActive() ) return;
		life -= interpolation;
		if(life <= 0) {
			life = 0;
			deactivate();
			return;
		}
	}

}
