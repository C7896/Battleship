package hw3;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 * @author Konstantin Kuzmin
 */
public class BattleshipProtocolClient implements Connectable {
	public static final int DEFAULT_PORT = 8189;
	
	private Socket socket;
	private String server;
	private int port;
	private InputStream inStream;
	private OutputStream outStream;
	Scanner in;
	PrintWriter out;
	private Logger log;	
	
	public BattleshipProtocolClient(String server) throws IOException {
		this(server, DEFAULT_PORT);
	}
	
	public BattleshipProtocolClient(String server, int port) throws IOException {
		if ( port > 65535 || port < 0 ) {
			throw new IOException(String.format("Could not find server on port %d.\n", port));
		}
		
		this.log = Logger.getLogger("global");
		this.server = server;
		this.port = port;
	}

	@Override
	public void connect() throws IOException {
		this.socket = new Socket(this.server, this.port);
		
		log.info(String.format("Connection to server %s established at port %d.\n", server, port));
		this.inStream = this.socket.getInputStream();
		this.outStream = this.socket.getOutputStream();
		this.in = new Scanner(this.inStream);
		this.out = new PrintWriter(new OutputStreamWriter(this.outStream, StandardCharsets.UTF_8), true);
	}
	
	@Override
	public void disconnect() throws IOException {
		if ( socket != null && !socket.isClosed() ) {
	        try {
	        	socket.close();
	        	log.info("Connection to server was closed.\n");
	        } catch (IOException e) {
	        	log.info(String.format("Could not disconnect: %s.\n", e.getMessage()));
	        }
	    }
	}

	@Override
	public void send(String message) {
		this.out.println(message);
		log.info(String.format("Message %s sent.\n", message));
	}

	@Override
	public String receive() {
		String message = this.in.nextLine();
		log.info(String.format("Message %s received.\n", message));
		return message;
	}

	@Override
	public int getPort() {
		return this.port;
	}
}