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
import com.calefay.utils.GameWorldInfo;

public abstract class EDBaseObjective implements EDObjective {
	private boolean complete = false;
	private String label = null;
	private GameEvent progressedEvent = null, completedEvent = null;
	private int requiredRepetitions = 1, totalRepetitions = 1;
	
	private boolean textUpdated = false;
	private String statusText = null;
	
	public EDBaseObjective() {
		complete = false;
		textUpdated = false;
		completedEvent = null; progressedEvent = null;
		
		this.requiredRepetitions = 1;
	}
	
	public boolean isComplete() {return complete;}
	public void setCompletedEvent(GameEvent e) {completedEvent = e;}
	public void setProgressedEvent(GameEvent e) {progressedEvent = e;}
	public void setRepetitions(int reps) {this.requiredRepetitions = reps; totalRepetitions = reps;}
	public void setStatusText(String text) {this.statusText = text;}
	/* Sets a label by which this specific objective can be referenced. Use for example, to mark the objective complete by a script event instead of actually meeting all requirements.*/
	public void setLabel(String label) {this.label = label;}
	
	/* Progresses the objective by one repetition. Should be called whenever one repetition of the objective is achieved. Sends the appropriated events and performs completion check.*/
	protected void progress() {
		textUpdated = true;
		if(progressedEvent != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(progressedEvent,"scriptEvents");
		if(--requiredRepetitions <= 0) {
			doCompletion();
		}
	}
	
	private void doCompletion() {
		complete = true;
		GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent("ObjectiveComplete","gameplayEvents", this, null);
		if(completedEvent != null) GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(completedEvent,"scriptEvents");
		onCompletion();
	}
	
	/* Override to provide custom behaviour on objective completion.*/
	protected void onCompletion() {}
	
	public void receiveEvent(GameEvent e) {
		if(isComplete()) return;
		if(e.getEventType().equalsIgnoreCase("MarkObjectiveComplete")) {
			String[] args = (String[])e.getEventInitiator();
			if( (args != null) && (args.length > 1) && (args[1].equalsIgnoreCase(label)) ) doCompletion();
		}
	}
	
	public boolean refreshStatus() {return complete;}
	
	/* Returns true if the text has been updated since the last time it was checked.*/
	public boolean textHasUpdated() {return textUpdated;}
	
	public String getStatusText() {
		textUpdated = false;
		if(statusText == null) return null;
		if(complete) return (statusText.concat(" (done)"));
		if(totalRepetitions > 1) return (statusText + " (" + (totalRepetitions - requiredRepetitions) + "/" + totalRepetitions + ")");
		return statusText;
	}
	
	public void cleanup() {
		requiredRepetitions = 0;
		complete = false;
		completedEvent = null;
		progressedEvent = null;
	}
}