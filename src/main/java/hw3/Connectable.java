package hw3;

import java.io.IOException;

/**
 * @author Konstantin Kuzmin
 *
 */
public interface Connectable {
	public void connect() throws IOException;
	public void disconnect() throws IOException;
	public void send(String message);	
	public String receive();
	public int getPort();
}