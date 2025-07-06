package org.MyAmusementPark.src.communication;

import org.MyAmusementPark.src.utilities.MessageTypes;

import java.io.PrintStream;
import java.net.Socket;

/**
 * 
 * @author dmalonas
 *
 */
public class NodeAliveChecker implements Runnable {
	
	private ParkNodeCommunicator parkNodeCommunicator;
	private String neighbourId;
	private String neighbourIp;
	private int neighbourPort;
	
	/**
	 * NodeAliveChecker constructor.
	 * @param parkNodeCommunicator parknode communicator object
	 * @param neighbourId the neighbour id
	 * @param neighbourIp the neighbour ip
	 * @param neighbourPort the neighbour port
	 */
	public NodeAliveChecker(ParkNodeCommunicator parkNodeCommunicator, String neighbourId, String neighbourIp, int neighbourPort) {
		this.parkNodeCommunicator = parkNodeCommunicator;
		this.neighbourId = neighbourId;
		this.neighbourIp = neighbourIp;
		this.neighbourPort = neighbourPort;
	}
	
	/**
	 * Start thread that checks
	 * if neighbour node is alive
	 * every 5 seconds.
	 */
	public void start() {
		Thread thread = new Thread(this);
		thread.start();
	}

	/**
	 * Send a message to the neighbour 
	 * node every 5 seconds and if the 
	 * message fails an exception is 
	 * thrown and we know that the 
	 * node is down. Then let the
	 * ParkNodeCommunicator know
	 * that the node has left.
	 */
	@Override
	public void run() {
		try {
			
			while(true) {
				Thread.sleep(5000);
				Socket socket = new Socket(neighbourIp, neighbourPort);
				PrintStream printStream = new PrintStream(socket.getOutputStream());
				printStream.println(MessageTypes.HEARTBEAT_MESSAGE);
			}
		} catch (Exception e) {
			parkNodeCommunicator.nodeLeft(neighbourId);
		}
		
	}

}