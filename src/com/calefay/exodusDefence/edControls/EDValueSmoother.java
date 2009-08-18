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
package com.calefay.exodusDefence.edControls;

import com.calefay.utils.GameRemoveable;

/** Smoothes a float value over a given number of updates.	Useful for smoothing mouse input.
 * You can enter a value at any time, then request the average over the last x frames. 0 is used if no value is inserted during a cycle.
 * @author James Waddington
 *
 */
public class EDValueSmoother implements GameRemoveable {

	private boolean active;
	
	private int bufSize = 5;
	private float[] rtnBuffer;
	private int oldestEntry = 0;
	private float currentValue = 0;
	
	public EDValueSmoother(int bufferSize) {
		active = true;
		if(bufferSize > 0) bufSize = bufferSize; else bufSize = 1;
		
		rtnBuffer = new float[bufSize];
		for(int i = 0; i < bufSize; i++) {rtnBuffer[i] = 0;}
		currentValue = 0;
	}
	
	public void update(float interpolation) {
		rtnBuffer[oldestEntry] = currentValue;
		currentValue = 0;
		oldestEntry++;
		if(oldestEntry >= bufSize) {oldestEntry = 0;}
	}
	
	public void addValue(float value) {
		currentValue += value;
	}
	
	public float getAverageValue() {
		float avg = 0;
		for(int i = 0; i < bufSize; i++) {avg += rtnBuffer[i];}
		avg /= bufSize;
		return avg;
	}
	
	public void deactivate() {active = false;}
	public boolean isActive() {return active;}
	
	public void cleanup() {
		rtnBuffer = null;
		oldestEntry = 0; currentValue = 0; active = false;
	}
	
}
