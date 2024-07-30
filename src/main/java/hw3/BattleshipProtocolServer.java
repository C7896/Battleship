package hw3;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Logger;
import java.nio.charset.*;

/**
 * @author Konstantin Kuzmin
 */
public class BattleshipProtocolServer implements Connectable {
	public static final int DEFAULT_PORT = 8189;
	
	private int port;
	private Socket socket;
	private ServerSocket servSocket;
	private InputStream inStream;
	private OutputStream outStream;
	Scanner in;
	PrintWriter out;
	private Logger log;
	
	public BattleshipProtocolServer() throws IOException {
		this(DEFAULT_PORT);
	}
	
	public BattleshipProtocolServer(int port) throws IOException {
		if ( port > 65535 || port < 0 ) {
			throw new IOException(String.format("Server socket could not be created on port %d.\n", port));
		}
		
		this.log = Logger.getLogger("global");
		
		this.port = port;
		this.servSocket = new ServerSocket(this.port);
		
		log.info(String.format("Server socket was created on port %d.\n", port));
	}

	@Override
	public void connect() throws IOException {
		this.socket = this.servSocket.accept();
		log.info(String.format("Incoming connection from a client at %s accepted.\n",
				this.socket.getRemoteSocketAddress().toString()));
		this.inStream = this.socket.getInputStream();
		this.outStream = this.socket.getOutputStream();
		this.in = new Scanner(this.inStream);
		this.out = new PrintWriter(new OutputStreamWriter(this.outStream,
				StandardCharsets.UTF_8), true /* auto-flush */);
	}
	
	@Override
	public void disconnect() throws IOException {
		if ( socket != null && !socket.isClosed() ) {
	        try {
	        	socket.close();
	        	log.info("Connection to client was closed.\n");
	        } catch (IOException e) {
	        	log.info(String.format("Could not disconnect: %s.\n", e.getMessage()));
	        }
	    }
		
		if ( servSocket != null ) {
	        try {
	            servSocket.close();
	        	log.info(String.format("Server socket at port %d was closed.\n", port));
	        } catch (IOException e) {
	        	log.info(String.format("Server socket could not be closed: %s.\n", e.getMessage()));
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
	
	public int getPort() {
		return this.port;
	}
}