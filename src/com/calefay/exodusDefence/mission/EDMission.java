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

import java.util.ArrayList;

import com.calefay.simpleUI.UIChangeableTextContainer;
import com.calefay.utils.GameEvent;
import com.calefay.utils.GameRemoveable;
import com.calefay.utils.GameWorldInfo;


public class EDMission implements GameRemoveable {
	private String name = null;
	private boolean active = false;
	private ArrayList<EDObjective> objectives = null;
	private boolean allMet = false, anyMet = false, anyUpdated = false;
	private GameEvent completedEvent = null;
	private float timer = 0, checkFrequency = 1;
	
	private boolean showOnHUD = false;
	private UIChangeableTextContainer uiPanel = null;
	
	public EDMission(String name) {
		this.name = name;
		active = true;
		objectives = new ArrayList<EDObjective>();
		checkFrequency = 1f;
		
		this.showOnHUD = false;
	}
	
	public void addObjective(EDObjective objective) {
		if( (objective == null) || objectives.contains(objective) ) return;
		objectives.add(objective);
	}
	
	/* Sets the interval in seconds between condition checks. If zero or negative, they will never be checked but event based conditions will still function.*/
	public void setCheckFrequency(float time) {checkFrequency = time;}
	
	public void update(float interpolation) {
		if( !active || (checkFrequency <= 0)) return;
		timer -= interpolation;
		if(timer <= 0) {
			timer = checkFrequency;
			refresh();
		}
	}
	
	public void refresh() {
		if(!active) return;
		
		if(allMet) return;	// Currently completion of objectives is final, they cannot be "un-met".
		allMet = true; anyMet = false; anyUpdated = false;
		for(EDObjective o : objectives) {
			if(o != null) {
				if(o.refreshStatus()) anyMet = true; else allMet = false;
				if(o.textHasUpdated()) anyUpdated = true;
			}
		}
		
		if(showOnHUD && anyUpdated) updateDisplay();
		
		if(anyMet && allMet) {
			if(completedEvent != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(completedEvent,"scriptEvents");
			deactivate();
		}
		
	}

	public void updateDisplay() {
		if(uiPanel == null) return;
		
		uiPanel.clearCanvas();
		uiPanel.setFont("Sans BOLD 14", 14f);
		uiPanel.writeText("Objectives", 0, 15 );
		uiPanel.setFont("Sans PLAIN 11", 11f);
		int y = 0;
		for(int i = 0; i < objectives.size(); i++) {
			EDObjective o = objectives.get(i);
			if( (o != null) && (o.getStatusText() != null)) {
				uiPanel.writeText(o.getStatusText(), 0, 40 + y );
				y += 15;
			}
		}
		uiPanel.updateText();
	}
	
	/* Checks the supplied event against each objective and updates them if appropriate.*/
	public void checkEvent(GameEvent e) {
		/* TODO: Currently this is called externally from the game event handling code. 
		 * Might be better to give this class it's own listener on the gameplay queue. This would require the routing listener class (which has a list of listeners to allow multiple listeners on one queue).
		 * Might also need to be careful as currently we send events to the gameplay queue which gives a theoretical infinite loop possibility (only if misused). Could work around by feeding events to a victoryevents queue instead.
		 */
		if(!active) return;

		if(e.getEventType().equalsIgnoreCase("MarkObjectiveComplete")) {
			// Only process a MarkObjectiveComplete event if it refers to an event within this mission.
			String[] args = (String[])e.getEventInitiator();
			if( (args == null) || (args.length < 2) || (!args[0].equalsIgnoreCase(name)) ) return;
		}
		
		for(EDObjective o : objectives) {
			o.receiveEvent(e);
		}
	}
	
	public void flush() {
		allMet = false;
		if(objectives != null) {
			for(EDObjective c : objectives) {
				if(c != null) c.cleanup();
			}
			objectives.clear();
		}
	}
	
	public void setCompletedEvent(GameEvent e) {completedEvent = e;}
	public void setShowOnHUD(boolean show) {this.showOnHUD = show;}
	public void setHUDPanel(UIChangeableTextContainer container) {this.uiPanel = container; if(uiPanel != null) updateDisplay();}
	public boolean showOnHUD() {return showOnHUD;}
	
	/* NOTE - This should not generally be used.*/
	public void setName(String name) {this.name = name;}	// Rather not have this as potentially the name could get out of synch with the HashMap label, but it is used by the loader.
	public String getName() {return name;}
	public int getObjectiveCount() {return objectives.size();}
	public int getHUDObjectiveCount() {
		int count = 0;
		for(EDObjective o : objectives) {
			if( (o != null) && (o.getStatusText() != null) ) count++;
		}
		return count;
	}
	
	public boolean isActive() {return active;}
	public void deactivate() {active = false;}
	
	public void cleanup() {
		active = false;
		flush(); objectives = null;
		
		if(uiPanel != null) {uiPanel.removeFromParent(); uiPanel.cleanup();} uiPanel = null;
	}
	
}
