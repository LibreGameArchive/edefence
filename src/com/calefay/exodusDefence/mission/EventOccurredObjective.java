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

import com.calefay.utils.GameEvent;

public class EventOccurredObjective extends EDBaseObjective {
	private GameEvent comparisonEvent = null;
	
	/* A victory codition satisfied when a certain event is sent to the victoryconditions queue.
	 * @param e: Template for the event that will satisfy this condition. Does not have to be the same object, but must contain matching fields. If e has null source or target, they will not be checked against.*/
	public EventOccurredObjective(GameEvent e) {
		super();
		comparisonEvent = e;
	}
	
	public String getLabel() {return "Event occurs";}
	
	public void receiveEvent(GameEvent e) {
		super.receiveEvent(e);
		
		if(isComplete() || (comparisonEvent == null) ) return;

		if( (comparisonEvent.getEventType().equals(e.getEventType())
			&& ( (comparisonEvent.getEventInitiator() == null) || comparisonEvent.getEventInitiator() == e.getEventInitiator() )
			&& ( (comparisonEvent.getEventTarget() == null) || (comparisonEvent.getEventTarget() == e.getEventTarget()) ) )) {
			progress();
		}
	}
	
	public void cleanup() {
		super.cleanup();
		comparisonEvent = null;
	}
}