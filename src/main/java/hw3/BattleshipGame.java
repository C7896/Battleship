package hw3;

import hw3.Ship;
import hw3.Location;

import java.util.HashMap;

enum GameStates {INIT, IN_PROGRESS, PLAYER1_WINS, PLAYER2_WINS, DRAW}

/**
 * BattleshipGame class
 * <p>
 * The BattleshipGame class represents a game of Battleship with two players.
 * It is a complete back-end, so all components necessary for the entire game
 * are included (except GUI).
 * </p>
 * 
 * @author Chev Kodama
 * @version 1.0
 */
public class BattleshipGame {
	/*
	 * Abstraction function:
	 * 	total represents the total number of ships on each player's board
	 * 	ship_sizes represents the lengths of the ships on each player's board
	 * 	p1_ships and p2_ships stores the length, location, orientation, and alive status
	 * 		of all ships as the values
	 * 		and uses the coordinates in the form "row,column" as the keys
	 * 	p1_grid represents the host's board
	 * 	p2_grid represents the client's board
	 * 		each board will be initialized at size length X length
	 * 		each value in each board can be one 0, 1, or 2
	 * 			0 represents an empty location
	 * 			1 represents an intact part of a ship
	 * 			2 represents a destroyed part of a ship
	 * 	p1_alive represents the number of ships left alive on p1_grid
	 * 	p2_alive represents the number of ships left alive on p2_grid
	 */
	/*
	 * for grids:
	 * 	0 = unoccupied ------------------ #00CAFF
	 * 	1 = occupied -------------------- #0F9420
	 * 	2 = occupied and hit ------------ #F52A1F
	 * 	3 = unoccupied and hit ---------- #1F40F5
	 * 	4 = part of a destroyed ship ---- #881010
	 */
	private static final int total = 5;
	private static final int length = 10;
	private static final int[] ship_sizes = {8, 6, 5, 4, 3};
	private HashMap<String, Ship> p1_ships;
	private HashMap<String, Ship> p2_ships;
	private int[][] p1_grid;
	private int[][] p2_grid;
	private int p1_alive;
	private int p2_alive;
	private GameStates state;
	/*
	 * Representation invariant:
	 * 	total == 5
	 * 	length == 10
	 * 	p1_ships, p2_ships, p1_grid, p2_grid, p1_alive, p2_alive, state != null
	 * 	for ( String coords : p1_ships.keySet()) {
	 * 		coords ! null
	 * 	}
	 * 	for (Ship ship : p1_ships.values()) {
	 * 		ship != null
	 * 	}
	 * for ( String coords : p2_ships.keySet()) {
	 * 		coords ! null
	 * }
	 * for (Ship ship : p2_ships.values()) {
	 * 		ship != null
	 * 	}
	 * 	for (0 < r < length) {
	 * 		for (0 < c < length) {
	 * 			p1_grid[r][c], p2_grid[r][c] != null
	 * 			p1_grid[r][c], p2_grid[r][c] >= 0
	 * 			p1_grid[r][c], p2_grid[r][c] <= 2
	 * 		}
	 * 	}
	 * 	p1_alive >= 0
	 * 	p2_alive >= 0
	 */
	
	/**
	 * BattleshipGame constructor.
	 */
	public BattleshipGame() {
		state = GameStates.INIT;
		p2_grid = new int[length][length];
		p1_alive = 5;
		p2_alive = 5;
		p1_ships = new HashMap<String, Ship>();
		p2_ships = new HashMap<String, Ship>();
		for ( int i = 0; i < 10; i += 2 ) {
			p1_ships.put(i + "," + 0, new Ship(ship_sizes[i/2], i, 0, true));
			p2_ships.put(i + "," + 0, new Ship(ship_sizes[i/2], i, 0, true));
		}
		
		p1_grid = new int[10][10];
		p2_grid = new int[10][10];
		
		/* add ships to grids */
		setUpShips(true);
		setUpShips(false);
	}
	
	private boolean setUpShips(boolean host) {
		if ( state != GameStates.INIT ) {
			return false;
		}

		int[][] placeholder = new int[10][10];
		for ( int r = 0; r < 10; r++ ) {
			for ( int c = 0; c < 10; c++ ) {
				placeholder[r][c] = 0;
			}
		}
		
		/* add ships to placeholder */
		for ( Ship ship : (host ? p1_ships.values() : p2_ships.values()) ) {
			boolean added;
			if ( ship.horizontal() ) {
				added = addHorizontal(ship, placeholder);
			} else {
				added = addVertical(ship, placeholder);
			}
			
			if ( !added ) {
				System.out.println("failed to add");
				return false;
			}
		}
		
		/* copy placeholder to p1 and p2 grids */
		for ( int r = 0; r < 10; r++ ) {
			for ( int c = 0; c < 10; c++ ) {
				if ( host ) {
					p1_grid[r][c] = placeholder[r][c];
				} else {
					p2_grid[r][c] = placeholder[r][c];
				}
			}
		}
		
		return true;
	}
	
	private boolean addHorizontal(Ship ship, int[][] placeholder) {
		if ( state != GameStates.INIT ) {
			return false;
		}
		
		if ( ship.getCol() + ship.getLength() > 10 ) {
			System.out.println("New location is off of board");
			return false;
		}
		
		/* check for collisions */
		for ( int c = ship.getCol(); c < ship.getCol() + ship.getLength(); c++ ) {
			if ( placeholder[ship.getRow()][c] > 0 ) {
				System.out.println("New location collides");
				return false;
			}
		}
		
		/* add ship */
		for ( int c = ship.getCol(); c < ship.getCol() + ship.getLength(); c++ ) {
			placeholder[ship.getRow()][c] = 1;
		}
		return true;
	}
	
	private boolean addVertical(Ship ship, int[][] placeholder) {
		if ( state != GameStates.INIT ) {
			return false;
		}
		
		if ( ship.getRow() + ship.getLength() > 10 ) {
			System.out.println("New location is off of board");
			return false;
		}

		/* check for collisions */
		for ( int r = ship.getRow(); r < ship.getRow() + ship.getLength(); r++ ) {
			if ( placeholder[r][ship.getCol()] > 0 ) {
				System.out.println("New location collides");
				return false;
			}
		}

		/* add ship */
		for ( int r = ship.getRow(); r < ship.getRow() + ship.getLength(); r++ ) {
			placeholder[r][ship.getCol()] = 1;
		}
		
		return true;
	}
	
	/**
	 * This moves the ship at the coordinates old_r,old_c to r,c.
	 * The ship does not move if it does not exist, the new
	 * location is out of bounds, or the new location overlaps
	 * with another existing ship.
	 * @param host	true if the client calling this function is
	 * 				the host, i.e. player1.
	 * 				false if the client calling this function is
	 * 				the client, i.e. player2.
	 * @param old_r	the row of the ship to be moved.
	 * @param old_c	the column of the ship to be moved.
	 * @param r		the row the ship is to be moved to.
	 * @param c		the column the ship is to be moved to.
	 * @param h		the new orientation of the ship.
	 * @return		true if the ship has been successfully moved
	 * 				to the new location.
	 * 				false otherwise. In this case, nothing will
	 * 				change.
	 */
	public boolean moveShip(boolean host, int old_r, int old_c, int r, int c, boolean h) {
		if ( state != GameStates.INIT ) {
			return false;
		}
		
		String old_coords = old_r + "," + old_c;
		String coords = r + "," + c;
		
		Ship ship = ( host ? p1_ships.get(old_coords) : p2_ships.get(old_coords) );
		if ( ship == null ) {
			return false;
		}
		boolean old_h = ship.horizontal();
		
		ship.setRow(r);
		ship.setCol(c);
		ship.setHorizontal(h);
		if ( !setUpShips(host) ) {
			/* reset ship */
			ship.setRow(old_r);
			ship.setCol(old_c);
			ship.setHorizontal(old_h);
			return false;
		} else {
			/* change key */
			if ( host ) {
				p1_ships.remove(old_coords);
				p1_ships.put(coords, ship);
			} else {
				p2_ships.remove(old_coords);
				p2_ships.put(coords, ship);
			}
			return true;
		}
	}
	
	/**
	 * This plays one round of this Battleship game. Each player
	 * may make one shot per round. The internal game board is updated.
	 * @param r1	the row of player1's guess.
	 * @param c1	the column of player1's guess.
	 * @param r2	the row of player2's guess.
	 * @param c2	the column of player2's guess.
	 * @return		the state of the game at the end of this function.
	 */
	public GameStates play(int r1, int c1, int r2, int c2) {
		state = GameStates.IN_PROGRESS;
		
		/* check player 1's move */
		if ( p2_grid[r1][c1] == 1 ) {
			p2_grid[r1][c1] = 2;
		} else if ( p2_grid[r1][c1] == 0 ) {
			p2_grid[r1][c1] = 3;	// :D
		}
		
		/* check player 2's move */
		if ( p1_grid[r2][c2] == 1 ) {
			p1_grid[r2][c2] = 2;
		} else if ( p1_grid[r1][c1] == 0 ) {
			p1_grid[r2][c2] = 3;
		}
		
		/* update ship live status */
		checkAlive();
		
		/* update state */
		if ( p1_alive == 0 && p2_alive == 0 ) {
			state = GameStates.DRAW;
		} else if ( p1_alive == 0 ) {
			state = GameStates.PLAYER1_WINS;
		} else if ( p2_alive == 0 ) {
			state = GameStates.PLAYER2_WINS;
		}
		
		return state;
	}
	
	private void checkAlive() {
		/* check player 1's grid */
		for ( Ship ship : p1_ships.values() ) {
			if ( !ship.alive() ) {
				continue;
			}
			
			/* check if ship is alive */
			boolean alive = false;
			for ( int i = 0; i < ship.getLength(); i++ ) {
				/* increment columns */
				if ( ship.horizontal() ) {
					if ( p1_grid[ship.getRow()][ship.getCol() + i] == 1 ) {
						alive = true;
					}
				/* increment rows */
				} else {
					if ( p1_grid[ship.getRow() + i][ship.getCol()] == 1 ) {
						alive = true;
					}
				}
			}
			
			if ( !alive && ship.alive() ) {
				ship.setAlive(false);
				p1_alive--;
				
				for ( int i = 0; i < ship.getLength(); i++ ) {
					/* increment columns */
					if ( ship.horizontal() ) {
						p1_grid[ship.getRow()][ship.getCol() + i] = 4;
					/* increment rows */
					} else {
						p1_grid[ship.getRow() + i][ship.getCol()] = 4;
					}
				}
			}
		}
		
		/* check player 2's grid */
		for ( Ship ship : p2_ships.values() ) {
			if ( !ship.alive() ) {
				continue;
			}
			
			boolean alive = false;
			for ( int i = 0; i < ship.getLength(); i++ ) {
				/* increment columns */
				if ( ship.horizontal() ) {
					if ( p2_grid[ship.getRow()][ship.getCol() + i] == 1 ) {
						alive = true;
					}
				/* increment rows */
				} else {
					if ( p2_grid[ship.getRow() + i][ship.getCol()] == 1 ) {
						alive = true;
					}
				}
			}
			
			if ( !alive && ship.alive() ) {
				ship.setAlive(false);
				p2_alive--;
				
				for ( int i = 0; i < ship.getLength(); i++ ) {
					/* increment columns */
					if ( ship.horizontal() ) {
						p2_grid[ship.getRow()][ship.getCol() + i] = 4;
					/* increment rows */
					} else {
						p2_grid[ship.getRow() + i][ship.getCol()] = 4;
					}
				}
			}
		}
	}
	
	/**
	 * Creates and returns a hashmap used to find the head
	 * (top and left-most part) of a ship.
	 * @param host	true if player1, false if player2
	 * @return		a HashMap(Location, Location)
	 * 				where the keys are each location of each ship and
	 * 				the values are the heads (top and left-most part)
	 * 				of the ship locations.
	 */
	public HashMap<Location, Location> getShipIndex(boolean host) {
		HashMap<Location, Location> index = new HashMap<Location, Location>();
		
		for ( Ship ship : (host ? p1_ships.values() : p2_ships.values()) ) {
			Location value = new Location(ship.getRow(), ship.getCol());
			for ( int i = 0; i < ship.getLength(); i++ ) {
				/* increment columns */
				if ( ship.horizontal() ) {
					Location key = new Location(ship.getRow(), ship.getCol() + i);
					index.put(key, value);
				/* increment rows */
				} else {
					Location key = new Location(ship.getRow() + i, ship.getCol());
					index.put(key, value);
				}
			}
		}
		
		return index;
	}
	
	/**
	 * Returns the current GameState
	 * of this BattleshipGame.
	 * @return	this.state
	 */
	public GameStates getState() {
		return this.state;
	}
	
	/**
	 * This creates and returns a deep copy of either
	 * player1 or player2's game board.
	 * @param player1	true if the client wants player1's board.
	 * 					false if the client wants player2's board.
	 * @return			a deep copy of p1_grid or p2_grid depending
	 * 					on the value of the parameter player1.
	 */
	public int[][] getGrid(boolean player1) {
		int[][] grid = new int[10][10];
		for ( int r = 0; r < 10; r++ ) {
			for ( int c = 0; c < 10; c++ ) {
				grid[r][c] = ( player1 ? p1_grid[r][c] : p2_grid[r][c] );
			}
		}
		
		return grid;
	}
	
	/**
	 * Returns the number of ships
	 * not destroyed on player1's board.
	 * @return	this.p1_alive
	 */
	public int getP1Alive() {
		return p1_alive;
	}
	
	/**
	 * Returns the number of ships
	 * not destroyed on player2's board.
	 * @return	this.p2_alive
	 */
	public int getP2Alive() {
		return p2_alive;
	}
	
	/**
	 * Returns the number of destroyed
	 * ships on player1's board.
	 * @return	this.total - this.p1_alive
	 */
	public int getP1Dead() {
		return total - p1_alive;
	}
	
	/**
	 * Returns the number of destroyed
	 * ships on player2's board.
	 * @return	this.total - this.p2_alive
	 */
	public int getP2Dead() {
		return total - p2_alive;
	}
	
	/**
	 * Returns the orientation of the ship on
	 * the board indicated by host at (r,c).
	 * @param host	true if player1, false if player2.
	 * @param r		the row of the ship head.
	 * @param c		the column of the ship head.
	 * @return		true if the ship is horizontal,
	 * 				false if it is vertical.
	 */
	public boolean getOrientation(boolean host, int r, int c) {
		return (host ? p1_ships.get(r + "," + c).horizontal() : p2_ships.get(r + "," + c).horizontal());
	}
}