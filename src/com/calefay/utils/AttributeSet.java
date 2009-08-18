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

import java.util.HashMap;

public class AttributeSet {
	private HashMap<String, float[]> floatAttributes = null;
	private HashMap<String, String[]> stringAttributes = null;
	
	public AttributeSet() {
		floatAttributes = new HashMap<String, float[]>();
		stringAttributes = new HashMap<String, String[]>();
	}
	
	public boolean hasAttribute(String attributeName) {
		return(stringAttributes.containsKey(attributeName) || floatAttributes.containsKey(attributeName));
	}
	
	public boolean isEmpty() {
		return(stringAttributes.isEmpty() && floatAttributes.isEmpty());
	}
	
	public void addAttribute(String attributeName, String[] data) {
		stringAttributes.put(attributeName, data);
	}
	
	public void addAttribute(String attributeName, float[] data) {
		floatAttributes.put(attributeName, data);
	}
	
	public String[] getStringAttribute(String attributeName) {
		return stringAttributes.get(attributeName);
	}
	
	public float[] getFloatAttribute(String attributeName) {
		return floatAttributes.get(attributeName);
	}
	
	public void cleanup() {
		floatAttributes.clear(); floatAttributes = null;
		stringAttributes.clear(); stringAttributes = null;
	}
}