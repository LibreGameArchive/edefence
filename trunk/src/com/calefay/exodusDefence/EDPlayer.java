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

import com.calefay.exodusDefence.EDGameModel.TurretUpgrade;


/* Represents a player in the game. Has entities which it controls, upgrades available etc. Is controlled by an interface, or potentially AI or network listener.
 * EDPlayer is a part of the game model not just a part of the user interface (ie they can receive and apply upgrades, get credit for kills etc.*/
public class EDPlayer {

	public boolean upgradeUpdated = false;		// Replace this with a proper listener.
	
	private TurretUpgrade availableUpgrade = null;
	
	public EDPlayer() {
		upgradeUpdated = false;
	}
	
	public void applyCurrentUpgrade() {	// FIXME: Get rid of the parameter.
		availableUpgrade = null;
	}
	
	public void addAvailableUpgrade(TurretUpgrade upgradeType) {
		upgradeUpdated = true;
		availableUpgrade = upgradeType;
	}
	
	public TurretUpgrade getAvailableUpgradeType() {
		return availableUpgrade;
	}
	
	/* To reset the player for a new level.*/
	public void reset() {
		availableUpgrade = null;
	}
	
	public void cleanup() {
		availableUpgrade = null;
	}
}
