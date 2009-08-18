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

public class GameScriptEvent implements Comparable<GameScriptEvent> {
	private float occursTime = 0;
	private String targetQueue = null;
	private GameEvent event = null;

	public GameScriptEvent(float time, GameEvent e, String queue) {
		if(time < 0) time = 0;
		occursTime = time;
		event = e;
		setTargetQueue(queue);
	}
	
	public GameScriptEvent(float time, GameEvent e) {
		this(time, e, null);
	}
	
	public float getOccursTime() {return occursTime;}
	public GameEvent getEvent() {return event;}
	public String getTargetQueue() {return targetQueue;}
	public void setTargetQueue(String queue) {targetQueue = queue;}
	public String toString() {return "GameScriptEvent time = " + occursTime + "     queue = " + targetQueue + 
		"     event = " + event;}
	
	public int compareTo(GameScriptEvent e) {	// hmm..
		int i = 0;
		if(this.occursTime > e.occursTime) i = 1;
		if(this.occursTime < e.occursTime) i = -1;
		return i;
	}
}
