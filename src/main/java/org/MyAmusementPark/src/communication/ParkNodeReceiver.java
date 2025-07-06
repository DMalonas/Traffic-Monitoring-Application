package org.MyAmusementPark.src.communication;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * The ParkNodeReceiver receives messages for
 * the node, forwards to the ParkNodeCommunicator
 * and the ParkNodeCommunicator forwards them to
 * the internal logic, in the ParkNode where the
 * message is handled.
 * @author dmalonas
 *
 */
public class ParkNodeReceiver implements Runnable {
	
	private ParkNodeCommunicator parkNodeCommunicator;
	ServerSocket serverSocket;
	int port;
	
	/**
	 * ParkNodeReceiver constructor.
	 * @param parkNodeCommunicator 
	 * @param port
	 */
	public ParkNodeReceiver(ParkNodeCommunicator parkNodeCommunicator, int port) {
		this.parkNodeCommunicator = parkNodeCommunicator;
		this.port = port;
	}
	
	/**
	 * Start a thread that listens
	 * for incoming messages.
	 */
	public void start() {
		Thread thread = new Thread(this);
		thread.start();
	}
	
	/**
	 * Stop thread.
	 * @throws IOException
	 */
	public void stop() throws IOException {
		serverSocket.close();
	}

	/**
	 * Overriden Runnable interface method run().
	 * Open an buffered InputStream and wait for
	 * input. When any message is received it is
	 * forwarded to the ParkNodeCommunicator of 
	 * the ParkNodeReceiver.
	 */
	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Listening from port " + port);
			while(true) {
				Socket socket = serverSocket.accept();
				// Receiving communication code here
				InputStreamReader inputStreamReader = new InputStreamReader(socket.getInputStream());
				BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
				String message = bufferedReader.readLine();
				// Process incoming message
				parkNodeCommunicator.processIncomingMessage(message);
			}
		} catch (IOException e) {
			System.out.println("Port " + port + " listening part closed");
			System.exit(0);
		}
		
	}
}
