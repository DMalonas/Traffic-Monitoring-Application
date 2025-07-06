package org.MyAmusementPark.src.utilities;

import org.MyAmusementPark.src.nodes.NodeToCommunicateWith;
import org.MyAmusementPark.src.nodes.ParkNode;

import java.util.Scanner;



/**
 * 
 * @author dmalonas
 *
 */
public class ParkNodeUtilities {
	/**
	 * Method which adds neighbours to a node.
	 * @param parkNode The node to add the neighbours to
	 */
	public static void addNeighboursInFlexibleNetworkManually(ParkNode parkNode) {
		boolean finished = false;
		Scanner scanner = new Scanner(System.in);
		while (!finished) {
			System.out.print("Please provide the id of the neighbour: ");
			String id = scanner.nextLine();
			System.out.print("Please provide the IP of the neighbour: ");
			String ip = scanner.nextLine();
			System.out.print("Please provide the port of the neighbour: ");
			String port = scanner.nextLine();
			NodeToCommunicateWith newNeighbour = new NodeToCommunicateWith(id, ip, port);
			parkNode.addNeighbour(newNeighbour);
			System.out.print("Please 'a' to add another neighbour or any other key to finish: ");
			String cont = scanner.nextLine();
			if (!cont.equals("a"))
				finished = true;
		}
		scanner.close();
		System.out.println("Neighbours added successfully");
	}
	
	/**
	 * Method that adds neighbours to a node from a file.
	 * @param parkNode the node to add the neighbours to.
	 */
	public static void addNeighborsInFlexibleNetworkFromFile(ParkNode parkNode) {
		
		parkNode.broadcastToArrayListNodes(MessageTypes.NEW_NEIGHBOUR_MESSAGE, parkNode.possibleNeighbours, null);
	}
}
