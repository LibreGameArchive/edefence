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
import java.util.Collections;

public class GameEventScript {
	private ArrayList<GameScriptEvent> script = null;
	
	/** A GameEventScript is essentially just a set of events with a time delay.
	 *  Each time it updates, it passes any events that have reached or passed their occursTime to the specified listener.*/
	// TODO: Add methods to run and pause the script.
	public GameEventScript() {
		script = new ArrayList<GameScriptEvent>();
	}
	
	/** Adds an event to the script. 
	 *  The event will be added at the end of the script and NOT in its proper chronological position.
	 *  This may mean the script needs sorting again.
	 */
	public void addScriptEvent(GameScriptEvent event) {
		// TODO: Add at least the option to add events in their proper position.
		script.add(event);
	}
	
	public void sortScript() {
		Collections.sort(script);
	}
	
	/** Copies all events from the entire script to the event handler straight away, without creating a script instance.*/
	public void runEntire(GameEventHandler handler) {
		for(GameScriptEvent e : script) {
			if(handler != null) handler.addEvent(e.getEvent(), e.getTargetQueue());
		}
	}
	
	public GameEventScriptInstance getScriptInstance(GameEventHandler h) {
		return new GameEventScriptInstance(h);
	}
	
	public void cleanup() {
		script.clear();	// FIXME: Should actually cleanup script objects as they can hold references.
	}
	
	public void printScript() {
		for(GameScriptEvent e : script) {
			System.out.println(e.getOccursTime() + "     " + e.getEvent().getEventType());
		}
	}
	
	/*********************************** Instance class follows ****************************************/
	
	public class GameEventScriptInstance implements GameRemoveable {
		private String label = null;
		private GameEventHandler handler = null;
		private float elapsedTime = 0;
		private int nextEvent = 0;
		private int repeatCount = 0;
		private boolean active = false;
		
		private GameEventScriptInstance(GameEventHandler h) {
			active = true;
			setHandler(h);
			elapsedTime = 0;
			nextEvent = 0;
			repeatCount = 0;
		}
		
		public void update(float interpolation) {
			if(script.size() <= 0) return;
			
			// Check if the script has ended
			if(nextEvent >= script.size()) {
				repeatCount--;
				if(repeatCount <= 0) {
					deactivate();
				} else {
					elapsedTime = 0;
					nextEvent = 0;
				}
			}
			if(!isActive()) return;
			
			elapsedTime += interpolation;
			// Check if any events are due to occur. If so, pass them to the listener.
			GameScriptEvent scriptEvent = script.get(nextEvent);
			while(scriptEvent.getOccursTime() <= elapsedTime) {
				// Should we provide a warning when handler is null?
				if(handler != null) handler.addEvent(scriptEvent.getEvent(), scriptEvent.getTargetQueue());
				nextEvent++;
				if(nextEvent < script.size()) scriptEvent = script.get(nextEvent); else break;
			}
		}
	
		public void setRepeat(int repetitions) {
			repeatCount = repetitions;
			if(repeatCount < 0) repeatCount = 0;
		}
	
		/** Sets the elapsed time in the script, ignoring any events before this. */
		public void setElapsedTime(float time) {
			printScript();
			int pos = Collections.binarySearch(script, new GameScriptEvent(time, new GameEvent("Search", null, null)) );
			if(pos < 0) pos = (-pos)-1;		// The search returns (-pos) - 1 when it does not find an exact match, so we have to reverse it.
			elapsedTime = time;
			nextEvent = pos;
		}
		
		public void initializeScript() {
			elapsedTime = 0;
			nextEvent = 0;
		}
	
		/** Sets the listener that will receive the events from this script.*/
		public void setHandler(GameEventHandler h) {
			handler = h;
		}
		
		public boolean isActive() {return active;}
		
		public void setLabel(String label) {this.label = label;}
		public String getLabel() {return label;}
		
		public void deactivate() {
			active = false;
			if(label != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("ScriptTerminated",  "scriptEvents", label, this);
		}
		
		public void cleanup() {
			label = null;
			active = false;
			handler = null;
			elapsedTime = 0; nextEvent = 0; repeatCount = 0;
		}
		
	}
	
}
