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

import java.util.ArrayList;

import com.calefay.utils.GameEvent;
import com.jme.renderer.ColorRGBA;
import com.jme.scene.state.TextureState;

public class UISelector extends UIContainer {
	protected ArrayList<String> choices = null;	// This is a list of the text labels for a selection.
	protected ArrayList<Object> choiceObjects = null;	// This is a list of objects that correspond to a selection.
	protected int currentChoice = 0;
	
	protected UIChangeableTextContainer currentChoiceUIText = null;
	private UIMenuButton scrollLeftButton = null;
	private UIMenuButton scrollRightButton = null;
	
	public UISelector(	String name,
						TextureState scrollTexture,
						String[] options, String[] items,
						int sizeX, int sizeY,
						int posX, int posY, int zOrder) {
		
		super(name, posX, posY, zOrder, sizeX, sizeY);
		
		choices = new ArrayList<String>();
		choiceObjects = new ArrayList<Object>();
		if( (options == null) || (options.length == 0) ) {
			addOption(" ");
		} else {
			addOption(options, items);
		}
		
		currentChoice = 0;
		currentChoiceUIText = new UIChangeableTextContainer(name + "UISelectorText", choices.get(0), sizeX, sizeY, 16, 0, zOrder, this);
		
		scrollLeftButton = new UIMenuButton(name + "UISelectorLeft", scrollTexture, 8, 16, 0, 0, 10, this);
		scrollRightButton = new UIMenuButton(name + "UISelectorRight", scrollTexture, 8, 16, (sizeX - 16), 0, 10, this);
		scrollRightButton.rotateTexture(180.0f);
	}
	
	public UISelector(  String name,
						TextureState scrollTexture,
						String[] options,
						int sizeX, int sizeY,
						int posX, int posY, int zOrder) {
		this(name, scrollTexture, options, null, sizeX, sizeY, posX, posY, zOrder);
	}
	
	/** Adds an option to the list.
	 * @param option The text label that will be displayed for the option in the UI.
	 * @param item The object that corresponds to this choice. Can be null.
	 */
	public void addOption(String option, Object item) {
		choices.add(option);
		choiceObjects.add(item);
	}
	
	public void addOption(String option) {
		addOption(option, null);
	}
	
	public void addOption(String[] options) {
		for(String s : options) {
			addOption(s);
		}
	}
	
	public void addOption(String[] options, String[] items) {
		if(items == null) {addOption(options); return;}
		for(int i = 0; i < options.length; i++) {
			if(i < items.length) addOption(options[i], items[i]); else addOption(options[i]);
		}
	}
	
	public void replaceOption(int index, String option, Object item) {
		choices.set(index,option);
		choiceObjects.set(index,item);
	}
	
	public void setOptions(String[] options) {
		if(options == null || options.length == 0) return;
		choices.clear(); choiceObjects.clear();
		for(String s : options) {
			addOption(s);
		}
	}
	
	public void setCurrentChoice(int choice) {
		if(choice < choices.size()) {
			currentChoice = choice;
			currentChoiceUIText.changeText(choices.get(choice));
		}
	}
	
	public int getCurrentChoice() {return currentChoice;}
	public String getCurrentChoiceText() {return choices.get(currentChoice);}
	public String getChoiceText(int choice) {return choices.get(choice);}
	public Object getChoiceObject(int choice) {return choiceObjects.get(choice);}
	public Object getCurrentChoiceObject() {return choiceObjects.get(currentChoice);}
	
	public void moveSelection(int howMuch) {
		currentChoice += howMuch;
		if(currentChoice >= choices.size() ) {currentChoice = choices.size() - 1;}
		if(currentChoice <= 0){currentChoice = 0;}
		setCurrentChoice(currentChoice);
	}
	
	protected boolean handleChildEvent(GameEvent event) {
		if(event.getEventInitiator() == scrollLeftButton) {moveSelection(-1); return true;}
		if(event.getEventInitiator() == scrollRightButton) {moveSelection(1); return true;}
		return false;
		// TODO: Probably useful to broadcast an event as well eg. selection changed. 
	}
	
	public void addBackground(ColorRGBA col) {
		// FIXME: This is ugly. Lots of containers might need this so should not be done at this subclass. Also it won't resize with the container etc.
		new UITextureContainer(name + "selectorbackground", col, width - 24, 16, 8, 0, zPos + 10, this);	// FIXME: UGH THIS IS ALL HARDCODED!
	}
}
