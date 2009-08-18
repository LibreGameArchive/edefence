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
import com.calefay.utils.GameWorldInfo;
import com.jme.math.Vector2f;
import com.jme.scene.Node;

/** The basic unit of the simpleUI system, very roughly approximating a scenegraph node. Every component should extend this.
 * Handles positioning, attaching children, detecting and responding to mouse events (usually subclass extends this)
 * Does not have any visible aspect, but does provide a node to attach them to which is automatically positioned correctly.
 * @author James Waddington
 *
 */
public class UIContainer {
	private boolean cleanedUp = false;
	
	protected int blX, blY;
	private float percentageX, percentageY;
	protected int width, height;
	protected float percentageWidth, percentageHeight;
	protected int zPos = 0;
	protected UIContainer parentContainer;
	protected Node displayNode = null;
	protected ArrayList<UIContainer> children;
	protected boolean active = false;
	private boolean proportionalPosition = false, proportionalSize = false;
	protected boolean hovered = false;
	
	protected String targetEventQueue = null;	// The queue that events from this component will be sent to.
	protected String onClickEventType = null;
	protected Object onClickEventParameters = null;
	
	protected String name = null;
	
	public UIContainer( String containerName, int posX, int posY, int zOrder, int sizeX, int sizeY) {
		this.name = containerName;
		
		cleanedUp = false;
		
		targetEventQueue = "uiInternal";	// Hardcoding - should prob have some constants.
		onClickEventType = "uiClick";	// Hardcoding - should prob have some constants.
		
		parentContainer = null;
		children = new ArrayList<UIContainer>();
		active = true;
		proportionalPosition = false; proportionalSize = false;
		hovered = false;
		
		blX = posX; blY = posY;
		percentageX = 0; percentageY = 0;

		width = sizeX; height = sizeY;
		percentageWidth = 0; percentageHeight = 0;
		zPos = zOrder;
		
		displayNode = new Node(name + "UIContainerNode");
		updatePosition(true);
	}
	
	/** Sets the containers position relative to it's parent. Position specified in pixels.*/
	public void setPosition(int x, int y) {
		if(cleanedUp) return;
		this.proportionalPosition = false;
		blX = x; blY = y;
		updatePosition(true);
	}
	
	/* *Sets the container's position relative to it's parent, as a fraction of the parent container's size. 
	 * eg. 0.9f, 0.5f would be 90% of the way across and half way down.
	 * NOTE: Cannot be used on the root node (as it has no parent to position relative to).
	 * Does NOT do bounds checking - so you CAN use negative values, or values greater than 1.0 to specify a position outside the parent container.*/
	public void setPosition(float x, float y) {
		if(getParent() == null) return;
		this.proportionalPosition = true;
		percentageX = x; percentageY = y;
		updatePosition(true);
	}
	
	/* If this container is proportionally positioned, then this method will recalculate the actual position.*/
	private void recalculatePosition() {
		if(cleanedUp) return;
		if(proportionalPosition && (getParent() != null)) {
			blX = (int)(percentageX * getParent().width);	// TODO: Remove direct member variable access.
			blY = (int)(percentageY * getParent().height);
		}
	}
	
	@Deprecated
	public void updatePosition() {updatePosition(true);}	// TODO: Remove this leaving only the private parametered version.
	
	/** Just to ensure that updateGeometricState is only called once.*/
	private void updatePosition(boolean initiator) {
		if(cleanedUp) return;
		
		recalculatePosition();
		displayNode.setLocalTranslation( blX, blY, 0);
		
		for(UIContainer c : children) {
			c.updatePosition(false);
		}
		if(initiator) displayNode.updateGeometricState(0, true);
	}
	
	/* Change the width and height of the container. Dimensions specified in pixels.*/
	public void resize(int width, int height) {
		proportionalSize = false;
		this.width = width; this.height = height;
		updateSize();	// To ensure that any proportionally sized children are resized also.
		updatePosition(true);	// To ensure that any proportionally positioned children have their positions recalculated.
	}
	
	/* Change the width and height of the container. Dimensions specified as a fraction of the parent container's size.*/
	public void resize(float width, float height) {
		proportionalSize = true;
		this.percentageWidth = width; this.percentageHeight = height;
	
		updateSize();	// To ensure that any proportionally sized children are resized also.
		updatePosition(true);
	}
	
	/* If this container is proportionally sized, then this method will recalculate the actual size.*/
	private void recalculateSize() {
		if(proportionalSize && (getParent() != null)) {
			width = (int)(percentageWidth * getParent().width);	// TODO: Remove direct member variable access.
			height = (int)(percentageHeight * getParent().height);
		}
	}
	
	private void updateSize() {
		recalculateSize();
		for(UIContainer c : children) {c.updateSize();}
	}
	
	/** Attaches the physical representation of this container to a supplied node rather than creating one.
	 * Note - this will change the parent of the supplied node to the display node of this container's parent, if it has one.*/
	public void setDisplayNode(Node n) {
		if(n == null) return;
		
		displayNode = n;
		if(parentContainer != null) {parentContainer.getDisplayNode().attachChild(n);}
		for(UIContainer c : children) {
			n.attachChild( c.getDisplayNode() );
		}
	}
	
	public void setParent(UIContainer parent) {
		parentContainer = parent;
		updatePosition(true);
	}
	
	public void removeFromParent() {
		if(parentContainer != null) {
			parentContainer.detachChild(this);
		}
	}
	
	/** Looks up a component with a given name.
	 * This is a crude implementation which walks through all children until it finds the first match.
	 * Intended for occasional use eg. Collecting a set of information from a screen when it is completed.
	 * Not going to be ideal performance for frequent calls in complicated systems.
	 */
	public UIContainer getChild(String searchName) {
		if( name.equals(searchName) ) return this;
		
		UIContainer searchResult = null;
		for(UIContainer c : children) {
			searchResult = c.getChild(searchName);
			if( (searchResult != null) && (searchResult.getName().equals(searchName)) ) {
				break;
			}
		}
		return searchResult;
	}
	
	/** Setting active to false will prevent the user from seeing or interacting with it.
	 *  @see setSuspended()*/
	public void setActive(boolean a) {
		if(a) {
			if( (parentContainer != null) && (displayNode != null) ) {
				parentContainer.getDisplayNode().attachChild(displayNode);
			}
		} else {
			if(displayNode != null) {
				if(displayNode.getParent() != null) {
					displayNode.removeFromParent();
				}
			}
		}
		updatePosition(true);
		active = a;
	}
	
	/** setSuspended(true) will suspend the container such that it can still be seen but cannot be interacted with.
	 * @see setActive()*/
	public void setSuspended(boolean s) {
		active = !s;
	}
	
	/** Invoked every update that the container is hovered
	 * @see onHover() is invoked only once as the mouse enters the container zone.
	 */
	protected void hoverCheck() {
		if(!hovered) {
			hovered = true;
			onHover();
		}
	}
	
	/** Processes a left click on this container. 
	 *  First handleClick is invoked. If that returns false, the event is passed to the parent container if not null, or the targetEventQueue
	 *  @see handleClick()*/
	protected void onClick() {
		if(handleClick()) return;
		if(parentContainer != null) {
			parentContainer.onChildEvent( new GameEvent(onClickEventType, this, onClickEventParameters) );
		} else {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(onClickEventType, targetEventQueue, this, onClickEventParameters);
		}
	}
	
	/** Override this method to take action in response to a click.
	 * @return true if no further action is required, false to send an event as normal.*/
	protected boolean handleClick() {return false;}
	
	/** Recursively passes an event back up through the parent hierarchy giving parent containers the chance to handle it.
	 *  This class should not normally be overridden, as this may prevent the event from being registered.
	 *  @see handleChildEvent() - Override this method to act on a child event.*/
	protected void onChildEvent(GameEvent event) { 
		// TODO: All events currently get passed right back up the tree to the root container before being broadcast. This is inefficient default behavior since the vast majority will be. Consider making is optional.
		if(handleChildEvent(event)) return;
		
		if(parentContainer != null) {
			parentContainer.onChildEvent(event);
		} else {
			GameWorldInfo.getGameWorldInfo().getEventHandler().addEvent(event, targetEventQueue);
		}
	}
	
	/** Override this method to take action in response to an event from a child container.
	 * 	@return true if the event was handled and no further action is required. Return false to continue with the default action.*/
	protected boolean handleChildEvent(GameEvent event) {return false;}
	
	/** Returns the world coordinates (in UI space) of the bottom left corner.*/
	public Vector2f getWorldPosition() {
		Vector2f worldPos = null;
		if(parentContainer == null) {
			worldPos = new Vector2f(blX, blY);
		} else {
			worldPos = parentContainer.getWorldPosition();
			worldPos.x += blX;
			worldPos.y += blY;
		}
		return worldPos;
	}
	
	/** Adds a child container, which will be processed whenever this container is.
	 *  Does nothing if the child parameter is null.
	 *  Does nothing if the supplied parameter is already in the child list.*/
	public void attachChild(UIContainer child) {
		if(child == null) return;
		if( children.contains(child) ) return;
		
		child.removeFromParent();
		if(child.isActive()) {getDisplayNode().attachChild( child.getDisplayNode() );}
		children.add(child);
		child.setParent(this);
		//child.updatePosition(true);	// Removed as setParent does an updatePosition.
	}
	
	public void detachChild(UIContainer child) {
		if(child == null) return;
		if( !children.contains(child) ) return;
		getDisplayNode().detachChild(child.getDisplayNode());
		children.remove(child);
		child.setParent(null);
	}
	
	/** If mouse is inside the component's area then this will return it's depth. -1 otherwise.*/
	public UIContainer checkForMouseFocus(float mouseX, float mouseY) {
		UIContainer currentTopFocus = null;
		
		if(active) {
			if( (mouseX > blX) && (mouseX < (blX + width)) && (mouseY > blY) && (mouseY < (blY + height)) )  {
				currentTopFocus = this;
			} else {
				if(hovered) {
					hovered = false;
					onHoverOff();
				}
			}
			
			UIContainer childFocus = null;
			for(UIContainer c : children) {
				childFocus = c.checkForMouseFocus(mouseX - blX, mouseY - blY);
				
				if(childFocus != null) {
					if(currentTopFocus == null) {
						currentTopFocus = childFocus;
					} else {
						if( childFocus.getZOrder() <= currentTopFocus.getZOrder() ) {
							currentTopFocus = childFocus;
						}
					}
				}
				
			}
		}
		
		return currentTopFocus;
	}
	
	public void checkMouseActions( float mouseX, float mouseY, boolean leftClick) {
		
		if(active) {
			UIContainer containerWithFocus = checkForMouseFocus(mouseX, mouseY);
			if(containerWithFocus != null) {
				containerWithFocus.hoverCheck();
				if(leftClick) {containerWithFocus.onClick();}
				}	
			}
		
		}
	
	public String getName() {return name;}
	public Node getDisplayNode() {return displayNode;}
	public void setTargetEventQueue(String targetQueue) {if(targetQueue != null) targetEventQueue = targetQueue;}
	public String getTargetEventQueue() {return targetEventQueue;}
	public void setClickEventType(String eventType) {if(eventType != null) onClickEventType = eventType;}
	public void setClickEventParameters(Object parameters) {onClickEventParameters = parameters;} 
	public String getClickEventType() {return onClickEventType;}
	public UIContainer getParent() {return parentContainer;}
	public int getWidth() {return width;}
	public int getHeight() {return height;}
	public int getZOrder() {return zPos;}
	public void setZOrder(int z) {zPos = z;}
	/* Returns true if the container is currently active in the interface. Not to be confused with isCleanedUp()*/
	public boolean isActive() {return active;}
	/* Returns true if the object has been cleaned up and is therefore no longer valid.*/
	public boolean isCleanedUp() {return cleanedUp;}
	/** Invoked when the mouse cursor moves over the container.*/
	protected void onHover() {}
	/** Invoked when the mouse cursor moves off the container.*/
	protected void onHoverOff() {}
	
	/** Used to cleanup the whole instance. Removes it, detaches any children, and renders this container invalid.
	 * NOTE: This will also cleanup all children attached to this container.*/
	public void cleanup() {cleanup(true);}
	
	private void cleanup(boolean initiator) {
		cleanedUp = true;
		
		active = false;
		
		if(children != null) {
			for(UIContainer c : children) {
				c.cleanup(false);
			}
			children.clear(); // Set to null later as it is checked in removeFromParent()
		}
		if(initiator) {removeFromParent();}
		if(displayNode != null) {displayNode.detachAllChildren(); displayNode = null;}
		
		name = null;
		children = null;
		parentContainer = null;
		targetEventQueue = null; onClickEventType = null;
		blX = 0; blY = 0; 
		proportionalPosition = false; percentageX = 0; percentageY = 0;
		width = 0; height = 0; zPos = 0;
		hovered = false;
	}
}