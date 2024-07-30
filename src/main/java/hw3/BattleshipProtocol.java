package hw3;

enum ProtocolStates {INIT, IN_PROGRESS, FINISHED}

/**
 * @author Chev Kodama
 * @version 1.0
 */
public class BattleshipProtocol {
	private ProtocolStates state;
	private BattleshipGame game;
	
	/**
	 * BattleshipProtocol constructor.
	 * @param g	a BattleshipGame object that is in the initialize state.
	 * @throws IllegalArgumentException if game state is not INIT.
	 */
	public BattleshipProtocol(BattleshipGame g) {
		if ( g.getState() != GameStates.INIT ) {
			throw new IllegalArgumentException("Invalid arguments passed to <init>()");
		}
		
		this.state = ProtocolStates.INIT;
		this.game = g;
	}
	
	/**
	 * Processes the command input and runs the corresponding
	 * methods in this BattleshipGame game.
	 * @param command	a string of the format, "host/client, function_name, arguments..."
	 * @return			the outcome of the command being executed.
	 */
	public String process(String command) {
		String response = "";
		String[] commands = parseCommand(command.toLowerCase());
		switch(state) {
			/* command format:
			 * 	commands[0] = "host" or "client"
			 * 	commands[1] = function name
			 * 	commands[2...] = function arguments
			 */
			case ProtocolStates.INIT:	if ( commands.length == 7 && commands[1].toLowerCase().equals("move") ) {
											boolean host = commands[0].toLowerCase().equals("host");
											int old_r = Integer.parseInt(commands[2]);
											int old_c = Integer.parseInt(commands[3]);
											int r = Integer.parseInt(commands[4]);
											int c = Integer.parseInt(commands[5]);
											boolean h = commands[6].toLowerCase().equals("h");
											
											this.game.moveShip(host, old_r, old_c, r, c, h);
											
											break;
										} else if ( commands[1].toLowerCase().equals("play")) {
											state = ProtocolStates.IN_PROGRESS;
										}
			case ProtocolStates.IN_PROGRESS:	if ( commands.length == 6 ) {
													if ( commands[1].toLowerCase().equals("play")) {
														int r1 = Integer.parseInt(commands[2]);
														int c1 = Integer.parseInt(commands[3]);
														int r2 = Integer.parseInt(commands[4]);
														int c2 = Integer.parseInt(commands[5]);
														
														GameStates result = this.game.play(r1, c1, r2, c2);
														if ( result != GameStates.IN_PROGRESS ) {
															this.state = ProtocolStates.FINISHED;
														}
														response = "move OK";
													} else {
														response = "Invalid command for current game state";
													}
												}
												if ( this.game.getState() != GameStates.IN_PROGRESS ) {
													state = ProtocolStates.FINISHED;
												}
												break;
			case ProtocolStates.FINISHED:	if ( this.game.getState() == GameStates.PLAYER1_WINS ) {
												response = "Player 1 wins.\nScore: " + game.getP2Dead() + "-" + game.getP1Dead();
											} else if ( this.game.getState() == GameStates.PLAYER2_WINS ) {
												response = "Player 2 wins.\nScore: " + game.getP1Dead() + "-" + game.getP2Dead();
											} else {
												response = "Draw. Score:\n0-0";
											}
											break;
			default:	break;
		}
		assert !response.isEmpty();
		return response;
		
	}
	
	/**
	 * Returns the current ProtocolState of this BattleshipProtocol.
	 * @return	this.state
	 */
	public ProtocolStates getState() {
		return this.state;
	}
	
	private String[] parseCommand(String command) {
		return command.split(",");
	}
}