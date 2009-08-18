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
package com.calefay.exodusDefence;

import java.io.IOException;
import java.util.HashMap;

import com.calefay.utils.AttributeSet;
import com.calefay.utils.BracketedTextParser;


public class EDTemplateSet {

	private HashMap<String, AttributeSet> templates = null;
	
	public EDTemplateSet() {
		templates = new HashMap<String, AttributeSet>();
	}
	
	public AttributeSet getTemplate(String name) {
		return templates.get(name);
	}
	
	/* Reads a template from a properly formatted text file.*/
	public void loadTemplate(String templateFile) {
		try {
			BracketedTextParser reader = new BracketedTextParser();
			reader.openSourceFile(templateFile);
			
			StringBuffer block = null;
			do {
				block = reader.readBlock();
				if( (block != null) && (block.toString().trim().toLowerCase().equals("entitytemplate")) ) {
					parseTemplate( BracketedTextParser.parseAttributeSet(reader.readBlock()));
					}
			} while(block != null);
			
			reader.closeSourceFile();
		} catch (IOException e) {
			System.err.println("Error parsing BFT file: " + e.toString());
		}
	}
	
	private void parseTemplate(AttributeSet data) {
		if(data.hasAttribute("templateName")) {
			templates.put(data.getStringAttribute("templateName")[0], data);
		}
	}
	
	public void cleanup() {
		if(templates != null) templates.clear(); templates = null;
	}
}
