package hw3;

import hw3.Location;

/**
 * Ship class
 * <p>
 * This class is an object that stores all of the information about
 * a ship in the Battleship game.
 * </p>
 * 
 * @author Chev Kodama
 * @version 2.0
 */
public class Ship {
	
	private int length;
	private int row;
	private int column;
	private boolean horizontal;
	private boolean alive;
	
	/**
	 * Ship constructor.
	 * @param l	the initial length of this ship.
	 * @param r	the initial row this ship is in.
	 * @param c	the initial column this ship is in.
	 * @param h	the initial orientation of this ship.
	 * 				true is horizontal, 
	 * 				false is vertical
	 */
	public Ship(int l, int r, int c, boolean h) {
		length = l;
		row = r;
		column = c;
		horizontal = h;
		alive = true;
	}
	
	/**
	 * Sets the length of this ship to l.
	 * @param l	the new length of this ship.
	 */
	public void setLength(int l) {
		length = l;
	}
	
	/**
	 * Sets the row this ship is in to r.
	 * @param r	the new row this ship is in.
	 */
	public void setRow(int r) {
		row = r;
	}
	
	/**
	 * Sets the column this ship is in to c.
	 * @param c	the new length of this ship.
	 */
	public void setCol(int c) {
		column = c;
	}
	
	/**
	 * Sets the orientation of this ship to h.
	 * @param h	the new orientation of this ship.
	 * 				true is horizontal, 
	 * 				false is vertical
	 */
	public void setHorizontal(boolean h) {
		horizontal = h;
	}
	
	/**
	 * Sets the alive status of this ship to a.
	 * @param	a the new alive status of this ship.
	 * 				true is alive, 
	 * 				false is dead
	 */
	public void setAlive(boolean a) {
		alive = a;
	}
	
	/**
	 * Returns the length of this ship.
	 * @return	length
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * Returns the row this ship is in.
	 * @return	row
	 */
	public int getRow() {
		return row;
	}
	
	/**
	 * Returns the column this ship is in.
	 * @return	column
	 */
	public int getCol() {
		return column;
	}
	
	/**
	 * Returns the orientation of this ship.
	 * @return	true if horizontal, false if vertical.
	 */
	public boolean horizontal() {
		return horizontal;
	}
	
	/**
	 * Returns the alive status of this ship.
	 * @return	true if alive, false if dead.
	 */
	public boolean alive() {
		return alive;
	}
}