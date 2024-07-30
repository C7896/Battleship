package hw3;

import java.util.Objects;

/**
 * @author Chev Kodama
 * @version 2.0
 */
public class Location {
	private int row;
	private int column;
	private int hashCode;
	
	/**
	 * Location default constructor
	 */
	public Location() {
		this(0, 0);
	}
	
	/**
	 * Location custom constructor
	 * @param r	the row
	 * @param c	the column
	 */
	public Location(int r, int c) {
		row = r;
		column = c;
		hashCode = Objects.hash(r, c);
	}
	
	/**
	 * Sets row to r.
	 * @param r	the new row
	 */
	public void setRow(int r) {
		row = r;
	}
	
	/**
	 * Sets column to c.
	 * @param c	the new column
	 */
	public void setCol(int c) {
		column = c;
	}
	
	/**
	 * Returns the row.
	 * @return	this.row
	 */
	public int row() {
		return row;
	}
	
	/**
	 * Returns the column.
	 * @return	this.column
	 */
	public int col() {
		return column;
	}
	
	/**
	 * Returns true if this is equal to obj.
	 * @param obj	The object to check
	 * @return		true if this is equal to obj,
	 * 				false otherwise.
	 */
	@Override
	public boolean equals(Object obj) {
		if ( obj == null || obj.getClass() != this.getClass() ) {
			return false;
		}
		
		Location other_location = ( Location ) obj;
		if ( other_location.row() == row && other_location.col() == column ) {
			return true;
		}
		return false;
	}
	
	/**
	 * Returns the hashcode of this object.
	 * @return	this.hashCode
	 */
	@Override
    public int hashCode() {
        return this.hashCode;
    }
}