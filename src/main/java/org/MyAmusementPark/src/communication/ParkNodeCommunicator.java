package org.MyAmusementPark.src.communication;

import org.MyAmusementPark.src.nodes.NodeToCommunicateWith;
import org.MyAmusementPark.src.nodes.ParkNode;
import org.MyAmusementPark.src.utilities.MessageTypes;

import java.io.IOException;
import java.io.PrintStream;
import java.net.*;



/**
 * 
 * @author dmalonas
 *
 */
public class ParkNodeCommunicator {

	private int port;
	private ParkNode parentParkNode;
	private ParkNodeReceiver parkNodeReceiver;
	
	/**
	 * Each ParkNodeCommunicator has a ParkNodeReceiver
	 * for handling incoming messages. The ParkNodeCommunicator
	 * forwards messages from the ParkNode to other ParkNode(s)
	 * and forwards messages coming from other ParkNode(s) through
	 * its ParkNodeReceiver, to the ParknNode's logic for handling.
	 * @param parentParkNode The ParkNode for which we set
	 * 		up the ParkNodeCommunicator.
	 */
	public ParkNodeCommunicator(ParkNode parentParkNode) {
		this.port = Integer.parseInt(parentParkNode.getPort());
		this.parentParkNode = parentParkNode;
		parkNodeReceiver = new ParkNodeReceiver(this, port);
		parkNodeReceiver.start();
	}
	
	/**
	 * Initiate the thread that checks if node is alive.
	 * @param newNeighbour node to check if alive.
	 */
	public void startCheckingStatusOfNode(NodeToCommunicateWith newNeighbour) {
		NodeAliveChecker nodeAliveChecker = new NodeAliveChecker(this, newNeighbour.getId(), newNeighbour.getIpAddress(), Integer.parseInt(newNeighbour.getPort()));
		nodeAliveChecker.start();
	}
	
	/**
	 * Node has left.
	 * @param nodeId id of the node that left.
	 */
	public void nodeLeft(String nodeId) {
		parentParkNode.neighbourLeft(nodeId);
	}
	
	/**
	 * Run listener that starts the incoming messages
	 * mechanism of the node.
	 * @throws IOException
	 */
	public void runListener() throws IOException {
		parkNodeReceiver.start();
	}
	
	/**
	 * Sends messages to server (happens instantly).
	 * @param ip The receiving's node id.
	 * @param port The receiving's node port.
	 * @param message The receiving's node message,
	 * 		including the information of the 
	 * 		id, ip, and port of the node which is sending
	 * 		the message.
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void runMessenger(String id, String ip, int port, String message) throws UnknownHostException, IOException {
		try {
			Socket socket = new Socket(ip, port);
			PrintStream printStream = new PrintStream(socket.getOutputStream());
			printStream.println(message); //Send the message through the output stream.
			socket.close(); //Close the socket.
		}
		catch (UnknownHostException ue) {
			parentParkNode.printToConsole("Node's " + id + " IP not accessible");
		}
		catch (IOException ue) {
			parentParkNode.printToConsole("Node " + id + " is not up or not accepting messages");
		}
	}

	/**
	 * Processes an incoming message received by the ParkNodeReceiver.
	 * Determines if it's from a client (ENTER/EXIT) or another node,
	 * and delegates to the ParkNode accordingly.
	 *
	 * @param incomingMessage The raw message (e.g. "ENTER Z1235 127.0.0.1 8000")
	 * @throws UnknownHostException if the sender's host cannot be resolved
	 * @throws IOException if an I/O error occurs
	 */
	public void processIncomingMessage(String incomingMessage) throws UnknownHostException, IOException {
		// Split the message into two parts: the message type and the rest
		// https://stackoverflow.com/questions/5067942/what-is-the-best-way-to-extract-the-first-word-from-a-string-in-java
		String arr[] = incomingMessage.split(" ", 2);

		if (arr.length == 2) {

			// Message from the client (ENTER or EXIT), no need to unpack further
			if (arr[0].equals(MessageTypes.ENTER_MESSAGE) || arr[0].equals(MessageTypes.EXIT_MESSAGE)) {
				// arr[0] = message type (ENTER or EXIT)
				// arr[1] = details (e.g., node ID)
				parentParkNode.receiveFromClient(arr[0], arr[1]);
			}

			// Message from another node
			else {
				// arr[1] = "<sender_id> <sender_ip> <sender_port> <actual_message>"
				String tokenizedMessage[] = arr[1].split(" ", 3);

				// Forward to ParkNode with sender info and message
				parentParkNode.receiveFromNode(
					arr[0],                     // message type (e.g., ELECTION)
					tokenizedMessage[0],       // sender ID
					tokenizedMessage[1],       // sender IP
					tokenizedMessage[2]        // sender port + actual message
				);
			}
		}
	}
	
	/**
	 * Forward message from a node to another node.
	 * @param receivingParkNode the node to forward the message to.
	 * @param message The message.
	 * @throws NumberFormatException Exception.
	 * @throws UnknownHostException Exception.
	 * @throws IOException Exception.
	 */
	public void sendOutgoingMessage(NodeToCommunicateWith receivingParkNode, String message) 
			throws NumberFormatException, UnknownHostException, IOException {
		runMessenger(receivingParkNode.getId(), receivingParkNode.getIpAddress(), Integer.parseInt(receivingParkNode.getPort()), 
				parentParkNode.getId() + " " + parentParkNode.getIpAddress() + " " + parentParkNode.getPort() + " " + message);
	}

}
