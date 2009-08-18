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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/** Maintains a set of listeners, and serves as a routing system to send events to the appropriate listener.
 *  Picking up and acting on the events is done in the code which owns the listener.*/
public class GameEventHandler {
	// TODO: This class should probably be thread safe so that event handling can be done in a seperate thread.
	private HashMap<String, GameEventListener> eventListeners= null;	// Could use HashMap of ArrayLists to allow multiple listeners of the same type.
	private GameEventListener defaultListener = null;	// Optional listener which gets any events for which the specified listener is not found
	
	public GameEventHandler() {
		eventListeners = new HashMap<String, GameEventListener>();
	}
	
	/** Adds a new listener which will receive events of type eventType.
	 * Returns that listener or null if one already exists.
	 * @param eventQueue Name of the queue that this listener picks up. Can be anything;
	 *  the same queue name should be used in any events that this listener is to pick up.*/
	public GameEventListener addListener(String eventQueue, int maxSize) {
		// TODO: Add the option for listeners of unlimited size. Probably by supplying zero or negative size.
		GameEventListener listener = null;
		if(!eventListeners.containsKey(eventQueue)) {
			listener = new GameEventListener(maxSize);
			eventListeners.put(eventQueue, listener);
		}
		return listener;
	}
		
	/** Used to remove a listener. This should be used instead of cleaning up the listener directly.*/
	public void removeListener(String queue) { // Not ideal to reference it by queue name, esp as would like to be able to have multiple listeners on one queue.
		if(queue == null) return;
		
		GameEventListener listener = eventListeners.get(queue);
		if(listener != null) {
			listener.cleanup();
			eventListeners.remove(queue);
		}
	}
	
	/** Adds a new event with the supplied parameters. 
	 * If queue matches an existing listener, the event will be passed to that listener.
	 * Otherwise is will be passed to the default listener, if there is one. If not, it will be ignored.
	 * @param typeOfEvent - The specific type of event. This should correspond to a specific action.
	 * @param queue - The queue to place the event in. If there is a corresponding listener, then that listener will get the event.
	 */
	public void addEvent(String typeOfEvent, String queue, Object initiator, Object target) {
		addEvent( new GameEvent(typeOfEvent, initiator, target), queue);
	}

	public void addEvent(GameEvent event, String queue) {
		if(event.getEventType() == null) return;
		// TODO: Do something when the supplied queue does not exist and there is no default listener. Currently this is silently ignored.
		GameEventListener listener = eventListeners.get(queue);
		if(listener == null) listener = defaultListener;
		if(listener != null) listener.addEvent(event);	
	}
	
	public void setDefaultListener(GameEventListener listener) {
		defaultListener = listener;
	}
	
	/** Clears all events from all listeners, without processing them.. */
	public void flush() {
		GameEventListener listener = null;
		Iterator<String> it = eventListeners.keySet().iterator();
		while(it.hasNext()) {
			listener = eventListeners.get(it.next());
			listener.flushEvents();
		}
	}
	
	/** Cleans up the event handler ready for destruction or reuse.*/
	public void cleanup() {
		GameEventListener listener = null;
		Iterator<String> it = eventListeners.keySet().iterator();
		while(it.hasNext()) {
			listener = eventListeners.get(it.next());
			listener.cleanup();
		}
		eventListeners.clear();
	}
	
	
	/******************************* Listener classes follow *************************************/
	public class GameEventListener {
		private List<GameEvent> eventQueue = null; // TODO not sure if arrayList is a good data structure to use for a FIFO queue.
		private int maxSize = 0;
		
		/** Not intended to be instantiated directly. Should be done through the GameEventHandler's addListener method.*/
		private GameEventListener(int maxEvents) {
			eventQueue = new ArrayList<GameEvent>();
			maxSize = maxEvents;
		}
		
		private void addEvent(GameEvent event) {
			if(event == null) return;
			
			if (eventQueue.size() <= maxSize) {
				eventQueue.add(event);
			} else {// Possibly clear&disable the queue in the event of filling up on the basis it isn't getting checked and might be holding on to references.
				System.err.println("ERROR: ran out of space for new events!"); // FIXME: This NEEDS to throw an exception!!
			}
		}
		
		public GameEvent getNextEvent() {
			if(eventQueue.size() > 0) 
				{return eventQueue.remove(0);} 
			else
				{return null;}
		}

		/** Clears all remaining events from this listerner's queue, without processing them.*/
		public void flushEvents() {
			eventQueue.clear();
		}
		
		public void listEvents() {
			System.out.println("*********** GAMEEVENTLISTENER PRINTOUT BEGINS ***********");
			for(GameEvent e : eventQueue) {
				System.out.println(e.getEventType());
			}
			System.out.println("*********** GAMEEVENTLISTENER PRINTOUT ENDS ***********");
		}
		
		/** Cleans up the listener, rendering it invalid. Should NOT be called directly other than by the event handler.
		 * @see GameEventHandler.removeListener()*/
		private void cleanup() {
			maxSize = 0;
			flushEvents();
			eventQueue = null;
		}
	}
}
