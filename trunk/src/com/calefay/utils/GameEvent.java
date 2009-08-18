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

/** GameEvent is used both to queue events for the event handler and (usually by the event handler) to send actions to gameEntities.
 * For an action, 'target' is the action data - eg. damage amount. Actions are applied directly to entities, so they do not need a target.
 * @author James Waddington
 *
 */
public class GameEvent {
	private Object eventInitiator = null;
	private Object eventTarget = null;
	private String eventType = null;
		
	public GameEvent(String type, Object initiator, Object target) {
		eventInitiator = initiator;
		eventTarget = target;
		eventType = type;
	}
		
	public String getEventType() {
		return eventType;
	}
		
	public Object getEventInitiator() {
		return eventInitiator;
		}
	
	public Object getEventTarget() {
		return eventTarget;
	}
}
