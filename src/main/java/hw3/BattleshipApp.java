package hw3;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.Random;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

public class BattleshipApp extends Application {
	public static final int USE_DEFAULT_PORT = -1;
	/*
	 * for grids:
	 * 	0 = unoccupied ------------------ Light Blue -- #00CAFF
	 * 	1 = occupied -------------------- Green ------- #0F9420
	 * 	2 = occupied and hit ------------ Red --------- #F52A1F
	 * 	3 = unoccupied and hit ---------- Dark Blue --- #1F40F5
	 * 	4 = part of a destroyed ship ---- Dark Red ---- #881010
	 */
	private static final String[] grid_colors = {"-fx-background-color: #00CAFF", "-fx-background-color: #0F9420",
			"-fx-background-color: #F52A1F", "-fx-background-color: #1F40F5", "-fx-background-color: #881010"};
	/*												     gray		white		red   	  green   	 yellow   	brown */
	private static final String[] background_colors = {"#f0f0f5", "#ffffff", "#b3003b", "#00e64d", "#ffff66", "#ac7339"};

	private Preferences prefs;
	
	private Connectable opponent;
	private String serverName;
	private String backgroundColor;
	private int port;
	private Logger log;

	private BattleshipGame game;
	private BattleshipProtocol prot;

	private Thread connectThread;
	private Thread setUpThread;
	private Thread opponentMoveThread;
	
	private boolean waiting;
	private boolean host;
	private boolean setting_up;
	private boolean can_declare;
	private boolean selected;
	private Location selected_location;
	private HashMap<Location, Location> ship_index;
	
	private Scene scene;
	private BorderPane root;
	private VBox mainMenu;
	private MenuBar menuBar;
	
	private VBox configurations;
	
	private VBox waitingScreen;
	private VBox opponentMoveScreen;

	private GridPane board;
	private GridPane opponentBoard;
	private VBox info;
	
	private ToolBar setUpToolBar;
	
	private Timeline timeline;
    private int timeSeconds = 30;
    private Label timerLabel;
	
	
	public static void main(String args[]) {
		launch(args);
	}
	
	private Runnable setUpOpponentBoard = new Runnable() {
        @Override
        public void run() {
    		if ( waiting ) {
    			return;
    		}
    		
        	log.info("Thread now running setUpOpponentBoard\n");
        	while ( true ) {
	        	String move = "";
				do {
					move = opponent.receive();
				}
				while ( !move.contains(",move,") && !move.equals("confirmBoard") );
				
				if ( move.equals("confirmBoard") ) {
					break;
				}
				prot.process(move);
				
        	}
        	log.info("Thread finished running setUpOpponentBoard\n");
        }
	};
	
	private EventHandler<ActionEvent> declareTarget = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			if ( waiting ) {
				return;
			}
			
			if ( can_declare ) {
				switchToOpponentMove();
				
				Button location = (Button) event.getSource();
				selected_location.setRow(GridPane.getRowIndex(location));
				selected_location.setCol(GridPane.getColumnIndex(location));
				
				selected = true;
				can_declare = false;

				/* send my move and wait for opponent's move */
				String my_strike = String.format("%d,%d", selected_location.row(), selected_location.col());
				opponent.send(my_strike);
				
				opponentMoveThread = new Thread(() -> {
					String opponent_strike = "";
					do {
						opponent_strike = opponent.receive();
					}
					while ( !opponent_strike.contains(",") );

					/* both process the moves */
					String command = "";
					if ( host ) {
						command = "host,play," + my_strike + "," + opponent_strike;
					} else {
						command = "client,play," + opponent_strike + "," + my_strike;
					}
					prot.process(command);
					
					waiting = false;
					
					Platform.runLater(() -> {
						/* update GUI */
						updateBoards();
						switchToGame();
					});
					
					if ( prot.getState() == ProtocolStates.FINISHED ) {
						Platform.runLater(() -> {
							selected = false;
							can_declare = false;
							
							/* display results of the game */
							VBox resultsBox = new VBox();
							resultsBox.setAlignment(Pos.CENTER);
							
							String result = prot.process("");
							Label resultsLabel = new Label(result);
							Button homeButton = new Button("Return Home");
							
							resultsLabel.setStyle("-fx-font-size: 20");
							resultsLabel.setTextAlignment(TextAlignment.CENTER);
							
							homeButton.setOnAction(goHome);
							resultsBox.getChildren().addAll(resultsLabel, homeButton);
							root.setCenter(resultsBox);
						});
					} else {
						can_declare = true;
					}
				});
				
				opponentMoveThread.start();
			}
		}
	};
	
	private EventHandler<ActionEvent> goHome = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			waiting = false;
			
			try {
				opponent.disconnect();
			}
			catch(IOException error) {
				log.info(String.format("Failed to close server or disconnect from client: %s", error.getMessage()));
			}
			
			switchToHome();
		}
	};
	
	private EventHandler<ActionEvent> cancelWait = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			waiting = false;
			
			if (connectThread != null && connectThread.isAlive()) {
				connectThread.interrupt(); // Interrupt the waiting
	            System.out.println("Server socket waiting cancelled.");
	        }

			goHome.handle(event);
		}
	};
	
	private EventHandler<ActionEvent> confirmBoard = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			if ( waiting ) {
				return;
			}
			
			setting_up = false;
			selected = false;

			opponent.send("confirmBoard");
			
			switchToOpponentMove();
			opponentMoveThread = new Thread(() -> {
				try {
		            setUpThread.join(); // Main thread waits for setUpThread to complete
		        } catch (InterruptedException e) {
		            log.info(String.format("setUpThread failed to join: %s.\n", e.getMessage()));
		        }
				
				Platform.runLater(() -> {
					log.info("Finished setup\n");
					waiting = false;
					playGame();
				});
			});
			
			opponentMoveThread.start();
		}
	};
	
	private EventHandler<ActionEvent> rotateShip = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			if ( waiting ) {
				return;
			}
			
			if ( selected ) {
				/* change orientation */
				String horizontal = !game.getOrientation(host, selected_location.row(), selected_location.col()) ?
						"h" : "v";
				String coords = String.format("%d,%d", selected_location.row(), selected_location.col());
				String command;
				if ( host ) {
					command = String.format("host,move,%s,%s,%s", coords, coords, horizontal);
				} else {
					command = String.format("client,move,%s,%s,%s", coords, coords, horizontal);
				}
				opponent.send(command);
				prot.process(command);

				/* update visual */
				updateBoards();
				
				/* update ship_index */
				ship_index = game.getShipIndex(host);
				
				selected = false;
			}
		}
	};
	
	private EventHandler<ActionEvent> moveShip = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			if ( waiting ) {
				return;
			}
			
			log.info("Pressed");
			if ( selected ) {
				log.info("Selected");
				/* remove highlight from the selected location */
				for ( Node node : board.getChildren() ) {
					Location tmp = new Location(GridPane.getRowIndex(node), GridPane.getColumnIndex(node));
					if ( tmp.equals(selected_location) ) {
						String buttonStyle = node.getStyle();
						buttonStyle = buttonStyle.replace("; -fx-background-color: white;", "");
						node.setStyle(buttonStyle);
					}
				}
				
				/* get the new location */
				Button location = (Button) event.getSource();
				Location new_location = new Location(GridPane.getRowIndex(location), GridPane.getColumnIndex(location));
				
				/* move the ship with the same orientation */
				String horizontal = game.getOrientation(host, selected_location.row(), selected_location.col()) ?
						"h" : "v";
				String old_coords = String.format("%d,%d", selected_location.row(), selected_location.col());
				String coords = String.format("%d,%d", new_location.row(), new_location.col());
				String command;
				if ( host ) {
					command = String.format("host,move,%s,%s,%s", old_coords, coords, horizontal);
				} else {
					command = String.format("client,move,%s,%s,%s", old_coords, coords, horizontal);
				}
				opponent.send(command);
				prot.process(command);
				
				/* update visual */
				updateBoards();

				/* update ship_index */
				ship_index = game.getShipIndex(host);
				selected = false;
			} else if ( setting_up ) {
				log.info("Setting up");
				Button location = (Button) event.getSource();
				Location new_location = new Location(GridPane.getRowIndex(location), GridPane.getColumnIndex(location));
				
				/* set selected_location to selected ship's head */
				if ( ship_index.get(new_location) == null ) {
					log.info("Not a ship");
					return;
				}
				selected_location = ship_index.get(new_location);
				
				/*  highlight newly selected location's head */
				for ( Node node : board.getChildren() ) {
					Location tmp = new Location(GridPane.getRowIndex(node), GridPane.getColumnIndex(node));
					if ( tmp.equals(selected_location) ) {
						node.setStyle(node.getStyle() + "; -fx-background-color: white;");
					}
				}
				
				selected = true;
			}
		}
	};
	
	private void setBackgroundColor() {
		int color;
		switch(backgroundColor) {
			case "Gray":	color = 0; break;
			case "White":	color = 1; break;
			case "Red":		color = 2; break;
			case "Green":	color = 3; break;
			case "Yellow":	color = 4; break;
			case "Brown":	color = 5; break;
			default:		color = 0;
		}
		
		root.setStyle("-fx-background-color: " + background_colors[color]);
		mainMenu.setStyle("-fx-background-color: " + background_colors[color]);
		menuBar.setStyle("-fx-background-color: " + background_colors[color]);
		
		configurations.setStyle("-fx-background-color: " + background_colors[color]);
		
		waitingScreen.setStyle("-fx-background-color: " + background_colors[color]);
		opponentMoveScreen.setStyle("-fx-background-color: " + background_colors[color]);

		board.setStyle("-fx-background-color: " + background_colors[color]);
		opponentBoard.setStyle("-fx-background-color: " + background_colors[color]);
		info.setStyle("-fx-background-color: " + background_colors[color]);
		
		setUpToolBar.setStyle("-fx-background-color: " + background_colors[color]);
	}
	
	private void switchToWaiting() {
		waiting = true;
		log.info("Switching to waiting screen\n");
		
		root.setLeft(null);
		root.setCenter(waitingScreen);
		root.setRight(null);
		root.setTop(null);
		root.setBottom(null);
	}
	
	private void switchToOpponentMove() {
		waiting = true;
		log.info("Switching to opponent's move screen\n");
		
		root.setLeft(null);
		root.setCenter(opponentMoveScreen);
		root.setRight(null);
		root.setTop(null);
		root.setBottom(null);
	}
	
	private void switchToHome() {
		root.setTop(menuBar);
		root.setBottom(null);
		root.setLeft(null);
		root.setRight(null);
		root.setCenter(mainMenu);
	}
	
	private void switchToConfigs() {
		root.setLeft(null);
		root.setCenter(configurations);
		root.setRight(null);
		root.setTop(null);
		root.setBottom(null);
	}
	
	private void switchToSetUp() {
		updateBoards();
		
		root.setCenter(info);
		root.setTop(null);
		root.setBottom(setUpToolBar);
	}
	
	private void switchToGame() {
		updateBoards();
		
		root.setCenter(info);
		root.setTop(null);
		root.setBottom(null);
		
		startTimer();
	}
	
	private void setUpGame() {
	    if (waiting) {
	        return;
	    }

	    /* Switch to waiting screen */
	    switchToWaiting();

	    connectThread = new Thread(() -> {
	        try {
	            if (host) {
	                /* Create server */
	                if (port != USE_DEFAULT_PORT) {
	                    opponent = new BattleshipProtocolServer(port);
	                } else {
	                    opponent = new BattleshipProtocolServer();
	                }
	            } else {
	                /* Prepare to join server at serverName */
	                if (serverName.equals("")) {
	                	Platform.runLater(() -> {
                	    	waiting = false;
	                	    Alert alert = new Alert(AlertType.ERROR, "ERROR: Server (host) name cannot be empty.\nOpen configurations and enter a valid IP address.");
	                	    Optional<ButtonType> error_result = alert.showAndWait();
	                	    if (error_result.isPresent() && error_result.get() == ButtonType.OK) {
	                	        switchToHome();
	                	    }
	                	});
	                }
	                
	                if (port != USE_DEFAULT_PORT) {
	                    opponent = new BattleshipProtocolClient(serverName, port);
	                } else {
	                    opponent = new BattleshipProtocolClient(serverName);
	                }
	            }

	            /* Connect to opponent */
	            opponent.connect();

	            /* Create game */
	            game = new BattleshipGame();
	            prot = new BattleshipProtocol(game);

	            Platform.runLater(() -> {
	                updateBoards();
	                switchToSetUp();

	                /* Let players set up ships */
	                ship_index = game.getShipIndex(host);
	                setting_up = true;
	                selected = false;
	                can_declare = false;

	                /* Start setUpOpponentBoard thread */
	                setUpThread = new Thread(setUpOpponentBoard);
	                setUpThread.start();
	            });

	            waiting = false;
	            log.info("connectThread finished");
	        } catch (IOException e) {
	            Platform.runLater(() -> {
        	    	waiting = false;
	            	if (e.getMessage().equals("Socket closed")) {
	            		Alert alert = new Alert(AlertType.INFORMATION, "Server closed");
		                Optional<ButtonType> information_result = alert.showAndWait();
						if ( information_result.isPresent() && information_result.get() == ButtonType.OK) {
			                switchToHome();
						}
	            	} else {
		                Alert alert = new Alert(AlertType.ERROR, "ERROR: " + e.getMessage());
		                Optional<ButtonType> error_result = alert.showAndWait();
						if ( error_result.isPresent() && error_result.get() == ButtonType.OK) {
			                switchToHome();
						}
	            	}
	            });
	        }
	    });

	    connectThread.start();
	}

	
	private void playGame() {
		if ( waiting ) {
			return;
		}
		
		selected = false;
		setting_up = false;
		can_declare = true;
		selected_location = new Location();
		
		switchToGame();
	}
	
	private void updateBoards() {
		int[][] my_grid = game.getGrid(host);
		int[][] their_grid = game.getGrid(!host);
		
		board = gridToBoard(my_grid, true);
		opponentBoard = gridToBoard(their_grid, false);
		
		VBox yourBoard = new VBox();
		VBox theirBoard = new VBox();
		
		yourBoard.setAlignment(Pos.CENTER);
		theirBoard.setAlignment(Pos.CENTER);
		
		Label yourLabel;
		Label otherLabel;
		if ( host ) {
			yourLabel = new Label("Player 1\nYou");
			otherLabel = new Label("Player 2\nOpponent");
		} else {
			yourLabel = new Label("Player 2\nYou");
			otherLabel = new Label("Player 1\nOpponent");
		}
		yourLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
		otherLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
		
		yourLabel.setTextAlignment(TextAlignment.CENTER);
		otherLabel.setTextAlignment(TextAlignment.CENTER);
		
		yourBoard.getChildren().addAll(board, yourLabel);
		theirBoard.getChildren().addAll(opponentBoard, otherLabel);
		
		root.setLeft(yourBoard);
		root.setRight(theirBoard);
	}
	
	private GridPane gridToBoard(int[][] grid, boolean ships_visible) {
		GridPane ret = new GridPane();
        ret.setPadding(new Insets(10));
		ret.setHgap(0);
		ret.setVgap(0);
		int rows = grid.length;
		int cols = grid[0].length;
		for ( int r = 0; r < rows; r++ ) {
			for ( int c = 0; c < cols; c++ ) {
				SquareButton tile = new SquareButton("");
				if ( ships_visible ) {
					tile.setStyle(grid_colors[grid[r][c]]);
					/* for game setup only */
					tile.setOnAction(moveShip);
				} else {
					switch(grid[r][c]) {
						case 2:		tile.setStyle(grid_colors[2]); break; /* hit */
						case 3:		tile.setStyle(grid_colors[3]); break; /* miss */
						case 4:		tile.setStyle(grid_colors[4]); break; /* destroyed */
						default:	tile.setStyle(grid_colors[0]); /* unknown */
					}
					/* for game playing only */
					tile.setOnAction(declareTarget);
				}
				
				tile.setStyle(tile.getStyle() + "; -fx-border-color: black; -fx-border-width: 2px");
				
				GridPane.setMargin(tile, new Insets(0));

		        GridPane.setHalignment(tile, HPos.CENTER);
		        GridPane.setValignment(tile, javafx.geometry.VPos.CENTER);
				
				ret.add(tile, c, r, 1, 1);
			}
		}
		
		return ret;
	}
	
	private void startTimer() {
        if (timeline != null) {
            timeline.stop();
        }

        timeSeconds = 30;
        timerLabel.setText(String.valueOf(timeSeconds));

        timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(1),
                        new EventHandler<ActionEvent>() {
                            @Override
                            public void handle(ActionEvent event) {
                                timeSeconds--;
                                timerLabel.setText(String.valueOf(timeSeconds));
                                if (timeSeconds <= 0) {
                                    timeline.stop();
                                    endTurn();
                                }
                            }
                        }));
        timeline.play();
    }
    
    private void endTurn() {
    	/* only run if not declared yet */
    	if ( !can_declare ) {
    		return;
    	}
    	
    	Random random = new Random();

        selected_location.setRow(random.nextInt(10));
        selected_location.setCol(random.nextInt(10));
    	
    	selected = true;
		can_declare = false;

		/* send my move and wait for opponent's move */
		String my_strike = String.format("%d,%d", selected_location.row(), selected_location.col());
		opponent.send(my_strike);
		
		opponentMoveThread = new Thread(() -> {
			String opponent_strike = "";
			do {
				opponent_strike = opponent.receive();
			}
			while ( !opponent_strike.contains(",") );

			/* both process the moves */
			String command = "";
			if ( host ) {
				command = "host,play," + my_strike + "," + opponent_strike;
			} else {
				command = "client,play," + opponent_strike + "," + my_strike;
			}
			prot.process(command);
			
			waiting = false;
			
			Platform.runLater(() -> {
				/* update GUI */
				updateBoards();
				switchToGame();
			});
			
			if ( prot.getState() == ProtocolStates.FINISHED ) {
				Platform.runLater(() -> {
					selected = false;
					can_declare = false;
					
					/* display results of the game */
					VBox resultsBox = new VBox();
					resultsBox.setAlignment(Pos.CENTER);
					
					String result = prot.process("");
					Label resultsLabel = new Label(result);
					Button homeButton = new Button("Return Home");
					
					resultsLabel.setStyle("-fx-font-size: 20");
					resultsLabel.setTextAlignment(TextAlignment.CENTER);
					
					homeButton.setOnAction(goHome);
					resultsBox.getChildren().addAll(resultsLabel, homeButton);
					root.setCenter(resultsBox);
				});
			} else {
				can_declare = true;
			}
		});
		
		opponentMoveThread.start();
    }

	@Override
	public void start(Stage stage) throws Exception {
		prefs = Preferences.userNodeForPackage(hw3.BattleshipApp.class);
		serverName = prefs.get("serverName", "localHost");
		port = Integer.parseInt(prefs.get("port", String.valueOf(BattleshipApp.USE_DEFAULT_PORT)));
		backgroundColor = prefs.get("backgroundColor", "Gray");
		
		log = Logger.getLogger("BattleshipApp");
		
		waiting = false;
		setting_up = false;
		can_declare = false;
		selected = false;
		selected_location = new Location(0, 0);
		
		root = new BorderPane();
		board = new GridPane();
		mainMenu = new VBox();
		
		configurations = new VBox();
		
		waitingScreen = new VBox();
		opponentMoveScreen = new VBox();
		
		menuBar = new MenuBar();
		opponentBoard = new GridPane();
		info = new VBox();

		setUpToolBar = new ToolBar();
		
		setBackgroundColor();
    	log.info(String.format("Retrieved preferences\nServer name: %s\nPort: %d\nBackground color: %s\n", serverName, port, backgroundColor));
    	
		/* Build mainMenu */
		mainMenu.setAlignment(Pos.CENTER);
		
		Label title = new Label("Battleship");
		title.setStyle("-fx-font-size: 45");
		Button hostButton = new Button("Host a Game");
		Button joinButton = new Button("Join a Game");
		
		hostButton.setMaxSize(200, 100);
		hostButton.setMinSize(100, 50);
		joinButton.setMaxSize(200, 100);
		joinButton.setMinSize(100, 50);
		
		GridPane.setVgrow(hostButton, Priority.ALWAYS);
		GridPane.setVgrow(joinButton, Priority.ALWAYS);
		GridPane.setHgrow(hostButton, Priority.ALWAYS);
		GridPane.setHgrow(joinButton, Priority.ALWAYS);
		
		hostButton.setOnAction(e -> {
			host = true;
			setUpGame();
		});
		joinButton.setOnAction(e -> {
			host = false;
			setUpGame();
		});
		
		mainMenu.getChildren().addAll(title, hostButton, joinButton);
		
		/* Build configurations */
		configurations.setAlignment(Pos.CENTER);
		
		Label serverLabel = new Label("Server Address");
		TextField serverInput = new TextField(serverName);
		Label portLabel = new Label("Port (-1 for default)");
		TextField portInput = new TextField(String.format("%d", port));
		portInput.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("-?\\d*")) {
                portInput.setText(newValue.replaceAll("[^\\d]", ""));
            }
        });
		Label backgroundLabel = new Label("Background Color");
		ObservableList<String> backgrounds =
				FXCollections.observableArrayList(
						"Gray",
						"White",
						"Red",
						"Green",
						"Yellow",
						"Brown"
				);
		ComboBox<String> backgroundsComboBox = new ComboBox<String>(backgrounds);
		String start_color = backgroundColor;
		backgroundsComboBox.setValue(start_color);
		backgroundsComboBox.setEditable(true);
		backgroundsComboBox.valueProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue observable, String oldValue, String newValue) {
				backgroundColor = newValue;
				setBackgroundColor();
				
				switchToConfigs();
			}
		});
		Button saveButton = new Button("Save Changes");
		saveButton.setOnAction(new EventHandler<ActionEvent>() {
    		@Override
    		public void handle(ActionEvent event) {
    			serverName = serverInput.getText();
    			port = Integer.parseInt(portInput.getText());
    			/* Save changes */
    			prefs.put("serverName", serverName);
    			prefs.put("port", String.valueOf(port));
    			prefs.put("backgroundColor", backgroundColor);
                
                log.info(String.format("Saved preferences\nServer name: %s\nPort: %s\nBackground color: %s\n", prefs.get("serverName", "not saved"), prefs.get("port", "not saved"), prefs.get("backgroundColor", "not saved")));
    			
    			switchToHome();
    		}
		});
		
		serverInput.setMaxWidth(300);
		portInput.setMaxWidth(300);
		
		configurations.getChildren().addAll(serverLabel, serverInput, portLabel, portInput, backgroundLabel, backgroundsComboBox, saveButton);
		
		
		/* Build waitingScreen */
		waitingScreen.setAlignment(Pos.CENTER);
		
		Label waitingLabel = new Label("Waiting for opponent...");
		Button homeButton = new Button("Return Home");
		
		homeButton.setOnAction(cancelWait);

		waitingLabel.setStyle("-fx-font-size: 32");
		
		waitingScreen.getChildren().addAll(waitingLabel, homeButton);
		
		/* Build opponentMoveScreen */
		opponentMoveScreen.setAlignment(Pos.CENTER);

		Label waitingLabel2 = new Label("Waiting for opponent...");

		waitingLabel2.setStyle("-fx-font-size: 32");
		
		opponentMoveScreen.getChildren().add(waitingLabel2);

		/* Build menuBar */
		Menu playMenu = new Menu("Play");
		MenuItem hostItem = new MenuItem("Host a Game");
		MenuItem joinItem = new MenuItem("Join a Game");
		Menu configurationsMenu = new Menu("Configurations");
		MenuItem editItem = new MenuItem("Edit Configs");
		
		hostItem.setOnAction(e -> {
			host = true;
			setUpGame();
		});
		joinItem.setOnAction(e -> {
			host = false;
			setUpGame();
		});
		editItem.setOnAction(e -> switchToConfigs());
		
		playMenu.getItems().addAll(hostItem, joinItem);
		configurationsMenu.getItems().add(editItem);
		menuBar.getMenus().addAll(playMenu, configurationsMenu);
		
		/* Build setUpToolBar */
		Button readyButton = new Button("Ready to Play");
		Button rotateButton = new Button("Rotate");
		
		readyButton.setOnAction(confirmBoard);
		rotateButton.setOnAction(rotateShip);
		
		setUpToolBar.getItems().addAll(readyButton, rotateButton);

		/* Build info */
		timerLabel = new Label("30");
		timerLabel.setStyle("-fx-font-size: 30");
        
        info = new VBox(10);
        info.getChildren().addAll(timerLabel);
        info.setPrefSize(100, 50);
        info.setStyle("-fx-alignment: center; -fx-padding: 10px;");
		
		/* Set up root as main menu */
		root.setTop(menuBar);
		root.setCenter(mainMenu);
		
		/* Set stage */
		scene = new Scene(root, 800, 450);
		stage.setTitle("Battleship");
		stage.setScene(scene);
		stage.setMinWidth(600);
		stage.setMinHeight(300);
		stage.show();
	}
	
	private class SquareButton extends Button {
        public SquareButton(String text) {
            super(text);
            setPrefSize(30, 30);
            setMinSize(getPrefHeight(), getPrefWidth());
            setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            
            minWidthProperty().bind(heightProperty());
            minHeightProperty().bind(widthProperty());
        }
    }
}