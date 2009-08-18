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
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BracketedTextParser {
	
	private static final Pattern extractFloatPattern = Pattern.compile("\\-?[0-9]+\\.?[0-9]*");
	private static final Pattern extractQuotedPattern = Pattern.compile("(\")[^\"]*(\")");
	
	private static final String matchFloatRegex = "\\-?[0-9]+\\.?[0-9]*";
	private static final String matchQuotedRegex = "\"[^\"]*\"";
	
	private static final Pattern attributeFloatPattern = Pattern.compile("\\S+\\s*=\\s*(" + matchFloatRegex + "\\s){1,}");
	private static final Pattern attributeStrPattern = Pattern.compile("\\S+\\s*=\\s*(" + matchQuotedRegex + "\\s){1,}");
	private static final Pattern attributeNamePattern = Pattern.compile("\\S+(?=\\s*=\\s)");
	
	private StringBuffer stringSource = null;
	private BufferedReader fileSource = null;
	private boolean eof = true;
	private int stringPointer = -1;
	private char lastRead = ' ';
	
	public BracketedTextParser() {lastRead = ' '; stringPointer = -1; eof = true;}
	public BracketedTextParser(StringBuffer source) {this(); setSource(source);}
	public BracketedTextParser(BufferedReader source) {this(); setSource(source);}
	
	private int read() throws IOException {
		if((fileSource == null) && (stringSource == null)) throw new IOException("Text parser has no source to read from");
		if(fileSource != null) 
			return fileSource.read();
		else
			return readFromString();
	}
	
	private int readFromString() {
		if(stringSource == null) return -1;
		if(stringPointer >= stringSource.length()) return -1;
		return stringSource.charAt(stringPointer++);
	}

	public StringBuffer readBlock() throws IOException {
		if((fileSource == null) && (stringSource == null)) throw new IOException("Text parser has no source to read from");
		boolean eob = false; boolean hasContents = false; boolean removeOuterBrackets = false;
		int bracketCount = 0; int i; 
		StringBuffer block = null;
		
		if(Character.isWhitespace(lastRead)) findNoneWhitespace();
		if(lastRead == '{') {	// Don't include outer brackets in the output (or we will never parse below this level.)
			findNoneWhitespace();
			bracketCount++;
			removeOuterBrackets = true;
		}
		
		while (!eof && !eob) {
							
			if( (bracketCount <= 0) && hasContents ) {
				if( (lastRead == '\n') || (lastRead == '\r') ) eob = true;
				if(lastRead == '{') eob = true;
			}
			
			if(lastRead == '{') {bracketCount++;}
			if(lastRead == '}') {
				bracketCount--;
				if( (bracketCount <= 0) && removeOuterBrackets ) lastRead = ' ';
			}
			
			if(!eob) {
				if( hasContents || !Character.isWhitespace(lastRead) ) {
					hasContents = true;
					if(block == null) block = new StringBuffer();
					block.append(lastRead);
				}
				
				i = read();
				// TODO: Possibly close the file automatically when eof is reached.
				if(i == -1) {eof = true; lastRead = ' ';} else {lastRead = (char)i;}
			}
		}
		if(block != null) block.trimToSize();
		return block;
	}
	
	private void findNoneWhitespace() throws IOException {
		boolean end = false; int i; 
		
		while (!end && !eof) {
			i = read();
			if(i == -1) 
				eof = true; 
			else {
				lastRead = (char)i;
				if(!Character.isWhitespace(lastRead)) end = true;
			}
		}
	}
	
	public void setSource(StringBuffer buff) {
		stringSource = buff;
		stringPointer = 0;
		fileSource = null;
		eof = false;
	}
	
	public void setSource(BufferedReader buff) {
		stringSource = null;
		stringPointer = -1;
		fileSource = buff;
		eof = false;
	}
	
	/* Opens the supplied file for reading and sets it as the parser source.
	 * closeFile should be called when parsing is complete.*/
	public void openSourceFile(String sourceFilename) throws FileNotFoundException {
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(sourceFilename);
		if(stream == null) throw new FileNotFoundException();
		InputStreamReader reader = new InputStreamReader(stream);
		BufferedReader buff = new BufferedReader(reader);
		setSource(buff);
	}
	
	public void closeSourceFile() throws IOException {
		if(fileSource != null) fileSource.close();
	}
	
	public static AttributeSet parseAttributeSet(StringBuffer text) {
		if(text == null) {
			System.err.println("Error - no data provided to parseAttributeSet");
			return null;
		}
		AttributeSet set = new AttributeSet();
		
		Matcher attributeFloatMatcher = attributeFloatPattern.matcher(text);
		while(attributeFloatMatcher.find()) {
			String s = attributeFloatMatcher.group();
			String attributeName = getSingleMatch(s, attributeNamePattern);
			float[] data = parseMultiFloat(s);
			set.addAttribute(attributeName, data);
		}
		
		Matcher attributeStrMatcher = attributeStrPattern.matcher(text);
		while(attributeStrMatcher.find()) {
			String s = attributeStrMatcher.group();
			
			String attributeName = getSingleMatch(s, attributeNamePattern);
			String[] data = parseMultiString(s); 
			set.addAttribute(attributeName, data);
		}
		
		if(set.isEmpty()) return null; else return set;
	}
	
	private static String getSingleMatch(String s, Pattern p) {
		Matcher m = p.matcher(s);
		if(m.find()) {
			return m.group();
		} else {
			System.err.println("Bracket formatted text parser: could not match string " + s + " with pattern " + p.pattern());
			return null;
		}
	}
	
	private static String[] parseMultiString(String s) {
		Matcher m = extractQuotedPattern.matcher(s);
		int matchCount = 0;
		while(m.find()) {matchCount++;}	// Has to be a better way!
		
		if(matchCount > 0) {
			m.reset();
			String[] result = new String[matchCount];
			for(int i = 0; (i < matchCount) && m.find(); i++) {
				String group = m.group();
				// (Trim quotes) Ideally do this in the regex, but using lookahead/behind means the quotes are available to other matches so you get the spaces between strings returned as " ". Can't think how to correct this at the moment.
				result[i] = group.substring(1, group.length() - 1);
			}
			return result;
		} else {
			return null;
		}
	}
	
	private static float[] parseMultiFloat(String s) {
		Matcher m = extractFloatPattern.matcher(s);
		int matchCount = 0;
		while(m.find()) {matchCount++;}	// Has to be a better way!
		
		if(matchCount > 0) {
			m.reset();
			float[] result = new float[matchCount];
			for(int i = 0; (i < matchCount) && m.find(); i++) {
				result[i] = parseFloat(m.group());
			}
			return result;
		} else {
			return null;
		}
	}
	
	private static float parseFloat(String s) {
		try {
			float f = Float.parseFloat(s);
			return f;
		} catch (NumberFormatException e) {
			System.err.println("Failed to parse float from " + s + " - using zero.");
			return 0f;
		}
	}	
}
