package org.MyAmusementPark.src.run;

import org.MyAmusementPark.src.nodes.ParkNode;
import org.MyAmusementPark.src.nodes.ParkNodesInputType;
import org.MyAmusementPark.src.utilities.MessageTypes;

import java.util.Scanner;

/**
 * 
 * @author dmalonas
 *
 */
public class MyAmusementParkMain {
	/**
	 * Derives the node information, and then
	 * sets up a node. Then the node just
	 * endlessly wait for input which is
	 * fetched through a Scanner object.
	 * If the input message is ELECTION,
	 * then the node starts an election
	 * if it is LEAVE then the node
	 * leaves the network.
	 *
	 * @param args
	 */
	public static void main(String args[]) {
//		if (args.length > 0) {
		ParkNodesInputType parkNodesInputType = new ParkNodesInputType(args[0]); //Create node.
		//		ParkNode parkNode = parkNodesInputType.returnParkNode(); //Return node.
		ParkNode parkNode = parkNodesInputType.returnParkNode();
		Scanner scanner = new Scanner(System.in);
		while (true) {
			String input = scanner.nextLine();
			if (input.equals(MessageTypes.ELECTION_MESSAGE) || input.equals(MessageTypes.LEAVE_MESSAGE))
				parkNode.receiveFromConsole(input); //handle console input
			else {
				System.out.println("Not a valid input. Please enter \"ELECTION\" or \"LEAVE\"");
			}
		}
	}
}
