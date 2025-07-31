package org.MyAmusementPark.src.nodes;

import org.MyAmusementPark.src.communication.ParkNodeCommunicator;
import org.MyAmusementPark.src.utilities.MessageTypes;
import org.MyAmusementPark.src.utilities.SharedResourceUtilities;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;



/**
 * 
 * @author 171100408
 *
 */
public class ParkNode {
	
	// Identification attributes
	private String id;                                    // Node id
	private String ipAddress;                             // Node ip address
	private String port;                                  // Node's port
	private int metricValue;                              // Node's metric value (the node with the highest one wins the election).
	private ParkNodeCommunicator parkNodeCommunicator;    // The socket communication is managed by the ParkNodeCommunication class.
	
	// Current coordinator info
	private String currentCoordinatorId;                  // The id of the current coordinator.
	private String currentCoordinatorIp;                  // The ip of the current coordinator.
	private String currentCoordinatorPort;                  // The port of the current coordinator.
	
	// Election related attributes
	private String currentElectionParentId;               // The parent's id for the current election. The election initialiser has no parent.
	private int neighBoursReplied;                         // Number of neighbour nodes that replied.
	private String maxMetricMessage;                      // The message containing the highest metric value, now.
 	private int currentMaxMetric;                         // The current max metric value that a node knows about.
	private String currentEligibleCoordinatorId;            // The current eligible/possible coordinator that the node knows about - election not over yet.
	private String currentEligibleCoordinatorIp;            // The current eligible/possible coordinator's IP that the node knows about - election not over yet.
	private String currentEligibleCoordinatorPort;            // The current eligible/possible coordinator's port that the node knows about - election not over yet.
	private boolean startedElection;                      // True if the node is the one that initiated the election.
	private boolean expectingCoordinatorInfo;             // True if the node is expecting message regarding who is the coordinator
	private boolean isDesignatedElectionStarter;
	
	// Park operation and mutual exclusion related attributes
	private boolean pendingCoordinatorReply;
	private String ticketIdToEnter;
	private String ticketIdToExit;
	
	// Neighbour nodes related attributes
	private ArrayList<NodeToCommunicateWith> neighbours;               // A node's NodeToCommunicateWith objects are stored within the neighbours, ArrayList.
	public ArrayList<NodeToCommunicateWith> possibleNeighbours;		// Possible neighbours are the ones that derive from node's input. After validating they are alive, they become neighbours
	
	// Being a Coordinator related attributes
	private boolean isCoordinator;                        // True if the node is the current coordinator.
	private Queue<NodeToCommunicateWith> queueOfRequestingNodes;
	private NodeToCommunicateWith currentResourceUtilizingNode;
	private NodeToCommunicateWith designatedElectionStarter;
	
	
	
	/**
	 * This constructor is not really used.
	 */
	public ParkNode() {
		
		currentMaxMetric = -1;
		metricValue = -1;
		maxMetricMessage = null;

		initializeGenericAttributes();
	}
	
	/**
	 * Constructor for the ParkNode.
	 * A node has id, ip, port, and metric value.
	 * @param id The node id
	 * @param ipAddress The ip address of the node
	 * @param port The port of the node
	 * @param metricValue The metric value of the node 
	 *     (the node with the highest wins the election)
	 */
	public ParkNode(String id, String ipAddress, String port, int metricValue) {
		
		this.id = id;
		this.ipAddress = ipAddress;
		this.port = port;
		this.metricValue = metricValue;
		
		System.out.print("Node " + id + " ");
		parkNodeCommunicator = new ParkNodeCommunicator(this); // Port to be defined by user or randomly
		currentMaxMetric = metricValue;
		maxMetricMessage = "[" + id + "," + metricValue + "," + ipAddress + "," + port + "]";
		currentEligibleCoordinatorId = id;
		currentEligibleCoordinatorIp = ipAddress;
		currentEligibleCoordinatorPort = port;
		
		initializeGenericAttributes();
	}
	
	
	/**
	 * Initialise attributes with false and null.
	 */
	public void initializeGenericAttributes() {
		
		currentCoordinatorId = null;
		currentCoordinatorIp = null;
		currentCoordinatorPort = null;
		isCoordinator = false;                                 // No node is a coordinator when they are first created
		
		neighbours = new ArrayList<NodeToCommunicateWith>();
		possibleNeighbours = new ArrayList<NodeToCommunicateWith>();
		
		currentElectionParentId = null;
		neighBoursReplied = 0;
		startedElection = false;
		expectingCoordinatorInfo = true;
		isDesignatedElectionStarter = false;
		
		pendingCoordinatorReply = false;
		ticketIdToEnter = null;
		ticketIdToExit = null;		
	}
	
	/**
	 * Establish communication.
	 * @param port The port the node is listening.
	 */
	public void startCommunicator(String port) {
		parkNodeCommunicator = new ParkNodeCommunicator(this); // Port to be defined by user or randomly
		try {
			parkNodeCommunicator.runListener();
		} catch (IOException e) {
			System.out.println("Socket error.");
		}
	}
	
	/**
	 * Add a neighbour.
	 * @param neighbour The node to add in the neighbours ArrayList of the node.
	 */
	public void addNeighbour(NodeToCommunicateWith newNeighbour) {
		neighbours.remove(returnNeighbourById(newNeighbour.getId()));
		neighbours.add(newNeighbour);
		parkNodeCommunicator.startCheckingStatusOfNode(newNeighbour);
	}
	
	
	/**
	 * Add a possible neighbour to the list of possible neighbours.
	 * @param newNeighbour the neighbour node.
	 */
	public void addPossibleNeighbour(NodeToCommunicateWith newNeighbour) {
		possibleNeighbours.add(newNeighbour);
	}
	
	
	/**
	 * Remove a neighbouring node as a neighbour
	 * because it left the network, by removing 
	 * it from the neighbours ArrayList.
	 * When a ParkNode leaves or gets terminated each 
	 * one of it ParkNode neighbours removes it from 
	 * their neighbours list. To avoid having this 
	 * list modified while at the same time it is used
	 * for another purpose, e.g. to broadcast an 
	 * ELECTION_MESSAGE message to neighbours
	 * (remember mutual exclusion has been implemented
	 * only for the common resource Resource.txt and not for 
	 * the neighbours list of the individual ParkNodes)
	 * after the neighbour is removed from the list 
	 * the thread in which the ParkNode runs sleeps 
	 * for 2 seconds. That way conflicts are avoided. 
	 * @param neighbourId the id of the neighbouring node that left.
	 */
	public void neighbourLeft(String neighbourId) {
		printToConsole("Neighbour " + neighbourId + " left the network");
		neighbours.remove(returnNeighbourById(neighbourId));
		sleep(2000);	// To avoid parallel modification of the neighbors' arraylist (node/coordinator removal & election related operations)
		if (neighbourId.equals(currentCoordinatorId)) {
			coordinatorLeft(null);
		}
		if (isCoordinator && designatedElectionStarter.getId().equals(neighbourId)) {
			pickDesignatedElectionStarter();
		}
	}
	
	/**
	 * Remove the information about the coordinator
	 * and then broadcast the message to the 
	 * neighbours (except from the parent node that 
	 * forwarded the information that the coordinator 
	 * has left if there is one) and then if the 
	 * node is a designated election starter will 
	 * also start the new election.
	 * @param doNotInformNodeId the node to except from 
	 *                          the broadcasting (the parent node).
	 */
	public void coordinatorLeft(String doNotInformNodeId) {
		currentCoordinatorId = null;
		currentCoordinatorIp = null;
		currentCoordinatorPort = null;
		pendingCoordinatorReply = false;
		broadcastToArrayListNodes(MessageTypes.COORDINATOR_LEFT_MESSAGE, neighbours, doNotInformNodeId);
		if (isDesignatedElectionStarter) {
			startedElection = true;
			broadcastToArrayListNodes(MessageTypes.ELECTION_MESSAGE, neighbours, null);
			isDesignatedElectionStarter = false;
		}
	}
	
	/**
	 * Sleep for int millis milliseconds.
	 * @param millis number of milliseconds
	 */
	public void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			printToConsole("Sleep error");
		}
	}
	
	
	/**
	 * 
	 * @param message The broadcast message that is sent.
	 * By design that can be either ELECTION_MESSAGE,
	 * when the node initiates an election, or 
	 * NEW_NEIGHBOUR_MESSAGE when a new flexible node
	 * informs its potential neighbours that it exists, 
	 * or a LEAVE message when the node is leaving.
	 * @param arraylist all neighbours from node's file
	 * @param exceptionNodeId a neighbour node we wish to exclude from the broadcast because it is
	 *        the node from which we received the election message in the first place.
	 */
	public void broadcastToArrayListNodes(String message, ArrayList<NodeToCommunicateWith> arraylist, String exceptionNodeId) {
		Iterator<NodeToCommunicateWith> arraylistIterator = arraylist.iterator(); //all neighbours from the node's file.
		while (arraylistIterator.hasNext()) {
			NodeToCommunicateWith arrayListItem = arraylistIterator.next();
			if (!arrayListItem.getId().equals(exceptionNodeId))
				sendToNode(arrayListItem, message);
		}
	}
	
	/**
	 * Send message to node.
	 * @param nodeId  The node ID.
	 * @param message The message to send.
	 */
	public void sendToNode(String nodeId, String message) {
		NodeToCommunicateWith neighbor = returnNeighbourById(nodeId);
		if (neighbor != null)
			sendToNode(neighbor, message);
	}
	
	/**
	 * Send message to node.
	 * @param node The node to send a message to.
	 * @param message The actual message.
	 */
	public void sendToNode(NodeToCommunicateWith node, String message) {
		try {
			printToConsole("Send to " + node.getId() + ": " + message);
			parkNodeCommunicator.sendOutgoingMessage(node, message);
		} catch (Exception e) {
			printToConsole("Error sending message " + message + " to node " + node);
		}
	}
	
	
	/**
	 * Send message to coordinator.
	 * @param message message to send to the coordinator.
	 */
	public void sendToCoordinator(String message) {
		try {
			printToConsole("Send to Coordinator " + currentCoordinatorId + ": " + message);
			parkNodeCommunicator.sendOutgoingMessage(new NodeToCommunicateWith(currentCoordinatorId,
					currentCoordinatorIp, currentCoordinatorPort), message);
		} catch (Exception e) {
			printToConsole("Error sending message " + message + " to coordinator " + currentCoordinatorId);
		}
	}
	
	
	/**
	 * Get neighbour ID.
	 * @param nodeId The node ID.
	 * @return The NodeToCommunicateWith object with the given ID.
	 */
	public NodeToCommunicateWith returnNeighbourById(String nodeId) {
		Iterator<NodeToCommunicateWith> neighboursIterator = neighbours.iterator();
		while (neighboursIterator.hasNext()) {
			NodeToCommunicateWith neighbour = neighboursIterator.next();
			if (neighbour.getId().equals(nodeId))
				return neighbour;
		}
		return null;
	}
	
	
	/**
	 * Received message from client (ENTER, or EXIT request).
	 * @param messageType ENTER_MESSAGE, EXIT_MESSAGE, the message type
	 * @param ticketID The id of the ticket.
	 */
	public void receiveFromClient(String messageType, String ticketID) {
		if(!pendingCoordinatorReply && (messageType.equals(MessageTypes.ENTER_MESSAGE) || messageType.equals(MessageTypes.EXIT_MESSAGE))) {
			pendingCoordinatorReply = true;
			if (messageType.equals(MessageTypes.ENTER_MESSAGE)) {
				ticketIdToEnter = ticketID;
			}
			else {
				ticketIdToExit = ticketID;
			}
			printToConsole(messageType + " REQUEST BY " + ticketID);
			sendRequestToCoordinator();
		}
		else {
			printToConsole(messageType + " REQUEST BY " + ticketID + ": BAD REQUEST OR NODE BUSY ON ANOTHER REQUEST");
		}
	}
	
	/**
	 * Send a request to the coordinator. 
	 * First check if I am the coordinator.
	 * If not the send "ENTER/EXIT REQUEST BY" + ticketID
	 */
	public void sendRequestToCoordinator() {
		pendingCoordinatorReply = true;
		if (currentCoordinatorId == null) {
			printToConsole(MessageTypes.ENTER_DENIED_NO_COORDINATOR_MESSAGE);
			pendingCoordinatorReply = false;
		}
		else if (isCoordinator) {
			handleMessageAsCoordinator(id, MessageTypes.MYSELF_MESSAGE, MessageTypes.MYSELF_MESSAGE, MessageTypes.REQUEST_RESOURCE_ACCESS_MESSAGE);
		}
		else {
			sendToCoordinator(MessageTypes.REQUEST_RESOURCE_ACCESS_MESSAGE);
		}
	}
	
	/**
	 * Handles messages. A message can be: ELECTION, ELECTION-ACK,
	 *     ELECTION-ACK [nodeID, metricValue], or COORDINATOR.   WHEN A NODE RECEIVES THE COORDINATOR MESSAGE IT BROADCASTS IT EVERYWHERE EVEN TO ITS PARENT AND THAT WHILE IT ADDS A LITTLE BIT OF OVERHEAD IT ALSO MAKE THE SYSTEM MORE FAULT TOLERANCE THROUGH REDUNDANCY, SINCE IT CATERS FOR THE CASE THAT A NODE GOES DOWN.
	 * @param parkNodeId The node id.
	 * @param message The message.
	 */
	/**
	 * Handles messages. A message can be: 
	 * 			NEW_NEIGHBOUR_MESSAGE
	 * 			WELCOME_MESSAGE
	 * 			REQUEST_RESOURCE_ACCESS_MESSAGE
	 * 			RELEASING_RESOURCE_MESSAGE
	 * 			ELECTION_MESSAGE
	 * 			ELECTION_ACK_MESSAGE
	 * 			NODE_LEAVING_MESSAGE
	 * 			COORDINATOR_LEFT_MESSAGE
	 * 			DESIGNATED_ELECTION_STARTER_MESSAGE.
	 * 			or an ELECTION_ACK_MESSAGE+metric
	 * 			or a coordinator message.
	 * @param parkNodeId   The id of the node that sent/forwarded the message.
	 * @param parkNodeIp   The ip of the node that sent/forwarded the message.
	 * @param parkNodePort The port of the node that sent/forwarded the message.
	 * @param message      The message.
	 */
	public void receiveFromNode(String parkNodeId, String parkNodeIp, String parkNodePort, String message) {
		printToConsole("Receive from " + parkNodeId + ": " + message);
		
		// If the message is NEW_NEIGHBOUR ("Flexible" network scenario)
		if (message.equals(MessageTypes.NEW_NEIGHBOUR_MESSAGE)) {
			handleNewNeighbourMessage(parkNodeId, parkNodeIp, parkNodePort);
		}
		// If the neighbour has answered our NEW_NEIGHBOUR_MESSAGE with WELCOME_MESSAGE
		else if (message.equals(MessageTypes.WELCOME_MESSAGE)) {
			handleWelcomeMessage(parkNodeId, parkNodeIp, parkNodePort);
		}
		// If the current node is a coordinator and it receives a request
		// to access or release the shared resource
		else if (message.equals(MessageTypes.REQUEST_RESOURCE_ACCESS_MESSAGE) || message.equals(MessageTypes.RELEASING_RESOURCE_MESSAGE)) {
			handleMessageAsCoordinator(parkNodeId, parkNodeIp, parkNodePort, message);
		}
		// If the message is ELECTION
		else if (message.equals(MessageTypes.ELECTION_MESSAGE)) {
			handleElectionMessage(parkNodeId);
		}
		// If the message is ELECTION-ACK
		else if (message.equals(MessageTypes.ELECTION_ACK_MESSAGE)) {
			handleElectionAckMessage();
		}
		//If the messsage is about a neighbour node that left the network.
		else if (message.equals(MessageTypes.NODE_LEAVING_MESSAGE)) {
			handleNodeLeavingMessage(parkNodeId);
		}
		//If the message is about the coordinator that left.
		else if (message.equals(MessageTypes.COORDINATOR_LEFT_MESSAGE)) {
			handleCoordinatorLeftMessage(parkNodeId);
		}
		/* If the message is from the coordinator making this ParkNode
		 the designated election starter node
		   that will start the election if the coordinator leaves or gets terminated. 
		*/
		else if (message.equals(MessageTypes.DESIGNATED_ELECTION_STARTER_MESSAGE)) {
			isDesignatedElectionStarter = true;
		}
		
		/* If the message is not ELECTION, or ELECTION-ACK, or any of the above,
		 * that means that one of the children sends a message with metric info
		 * to its parent,or that we are dealing with a COORDINATOR message.
		 */
		else {
			String[] tokenizedMessage = message.split(" ");  // split the message.
			// One of the child nodes has responded with its best
			// metric value (ELECTION_ACK <id> <metric> <ip> <port>).
			// This message is sent upwards in the tree so each node can compare candidates.
			if (tokenizedMessage[0].equals(MessageTypes.ELECTION_ACK_MESSAGE)) {
				handleMetricReceivedMessage(tokenizedMessage[1]); //the last word of tokenizedMessage[1] contains the metric.
			}
			// The root of the election (the initiator) has now decided the best coordinator.
			// It sends a NEW_COORDINATOR message to inform everyone who the winner is.
			else if (tokenizedMessage[0].equals(MessageTypes.NEW_COORDINATOR_MESSAGE)) {
				handleNewCoordinatorMessage(parkNodeId, tokenizedMessage);
			}
			// When a new flexible node joins the network, it needs to know who
			// the current coordinator is.
			// The coordinator sends an EXISTING_COORDINATOR message to help the new node sync up.
            else if (tokenizedMessage[0].equals(MessageTypes.EXISTING_COORDINATOR_MESSAGE)) {
				handleExistingCoordinatorMessage(parkNodeId, parkNodeIp, parkNodePort, tokenizedMessage);
			}
			// This ParkNode has been granted access to Resource.txt by the coordinator.
			else if (tokenizedMessage[0].equals(MessageTypes.GRANTED_RESOURCE_ACCESS_MESSAGE)) {
				handleGrantedResourceAccessMessage(parkNodeId);
			}
			// This ParkNode has been denied access to Resource.txt by the coordinator.
			else if (tokenizedMessage[0].equals(MessageTypes.DENIED_RESOURCE_ACCESS_MESSAGE)) {
				handleDeniedResourceAccessMessage(parkNodeId);
			}
		}
	}
	
	/**
	 * A node has sent to this node a NEW_NEIGHBOUR message.
	 * @param parkNodeId   The id of the node that sent the message.
	 * @param parkNodeIp   The ip of the node that sent the message.
	 * @param parkNodePort The port of the node that sent the message.
	 */
	public void handleNewNeighbourMessage(String parkNodeId, String parkNodeIp, String parkNodePort) {
		NodeToCommunicateWith newNeighbour = new NodeToCommunicateWith(parkNodeId, parkNodeIp, parkNodePort);
		addNeighbour(newNeighbour);
		if (currentCoordinatorId != null)
			sendToNode(parkNodeId, MessageTypes.EXISTING_COORDINATOR_MESSAGE + " " + currentCoordinatorId + " " + currentCoordinatorIp + " " + currentCoordinatorPort);
		else
			sendToNode(parkNodeId, MessageTypes.WELCOME_MESSAGE);
	}
	
	/**
	 * Handle WELCOME_MESSAGE message from neighbour node this ParkNode has
	 * on its list of potential neighbours, and to which it sent a NEW_NEIGHBOUR_MESSAGE,
	 * to which the node from which this node is now receiving the WELCOME_MESSAGE, 
	 * has replied with the WELCOME_MESSAGE to confirm that it added this node to 
	 * its own list of neighbours as well.
	 * @param parkNodeId   The id of the ParkNode that sent the message.
	 * @param parkNodeIp   The ip of the ParkNode that sent the message.
	 * @param parkNodePort The port of the ParkNode from which it sent the message.
	 */
	public void handleWelcomeMessage(String parkNodeId, String parkNodeIp, String parkNodePort) {
		/* Create new neighbour. */
		NodeToCommunicateWith newNeighbour = new NodeToCommunicateWith(parkNodeId, parkNodeIp, parkNodePort);
		/* Add new neighbour to the list of neighbours. */
		addNeighbour(newNeighbour);
	}

	/**
	 * This method is utilised by a coordinator node.
	 * Messages handled by a node that has the attribute
	 * of the coordinator is access requests to the common resource
	 * Resource.txt, messages that inform the coordinator that
	 * the resource has been released.
	 * @param parkNodeId   The id of the ParkNode that sent/forwarded the message
	 * @param parkNodeIp   The ip of the ParkNode that sent/forwarded the message
	 * @param parkNodePort The port from which the ParkNode that sent/forwarded to the coordinator
	 *                     the message is listening.
	 * @param message      The actual message (REQUEST_RESOURCE_ACCESS_MESSAGE/RELEASING_RESOURCE_MESSAGE)
	 *                     and the information of the node that (originally) sent it.
	 */
	public void handleMessageAsCoordinator(String parkNodeId, String parkNodeIp, String parkNodePort, String message) {
		if (isCoordinator) {

			// External node requests access and no one is using the resource → grant immediately
			if (!parkNodeId.equals(id) && message.equals(MessageTypes.REQUEST_RESOURCE_ACCESS_MESSAGE) && currentResourceUtilizingNode == null) {
				NodeToCommunicateWith nodeExpectingReply = new NodeToCommunicateWith(parkNodeId, parkNodeIp, parkNodePort);
				sendToNode(nodeExpectingReply, MessageTypes.GRANTED_RESOURCE_ACCESS_MESSAGE);
				currentResourceUtilizingNode = nodeExpectingReply;
			}

			// External node requests access but resource is occupied → add to queue
			else if (!parkNodeId.equals(id) && message.equals(MessageTypes.REQUEST_RESOURCE_ACCESS_MESSAGE) && currentResourceUtilizingNode != null) {
				NodeToCommunicateWith nodeExpectingReply = new NodeToCommunicateWith(parkNodeId, parkNodeIp, parkNodePort);
				queueOfRequestingNodes.add(nodeExpectingReply);
			}

			// Current user releases the resource → grant access to next in queue (external or self)
			else if (message.equals(MessageTypes.RELEASING_RESOURCE_MESSAGE) && currentResourceUtilizingNode.getId().equals(parkNodeId)) {
				currentResourceUtilizingNode = null;
				if (!queueOfRequestingNodes.isEmpty()) {
					NodeToCommunicateWith nodeExpectingReply = queueOfRequestingNodes.remove();

					// External node → send GRANTED message
					if (!nodeExpectingReply.getId().equals(id)) {
						currentResourceUtilizingNode = nodeExpectingReply;
						sendToNode(nodeExpectingReply, MessageTypes.GRANTED_RESOURCE_ACCESS_MESSAGE);
					}
					// Self node → proceed to accessResource directly
					else {
						pendingCoordinatorReply = false;
						currentResourceUtilizingNode = nodeExpectingReply;
						accessResource();
					}
				}
			}

			// This node is the requester, resource is
			// free → directly access the resource
			else if (parkNodeId.equals(id) && message.equals(MessageTypes.REQUEST_RESOURCE_ACCESS_MESSAGE) && currentResourceUtilizingNode == null) {
				NodeToCommunicateWith nodeExpectingReply = new NodeToCommunicateWith(parkNodeId, parkNodeIp, parkNodePort);
				pendingCoordinatorReply = false;
				currentResourceUtilizingNode = nodeExpectingReply;
				accessResource();
			}

			// This node is the requester,
			// but resource is busy → add to queue and wait
			else if (parkNodeId.equals(id) && message.equals(MessageTypes.REQUEST_RESOURCE_ACCESS_MESSAGE) && currentResourceUtilizingNode != null) {
				NodeToCommunicateWith nodeExpectingReply = new NodeToCommunicateWith(parkNodeId, parkNodeIp, parkNodePort);
				queueOfRequestingNodes.add(nodeExpectingReply);
			}
		}
	}
	
	/**
	 * Sends the information of the node with the
	 * highest metric (per this node) to the 
	 * parent node, if all the neighbours have 
	 * replied with ELECTION_ACK, or ELECTION_ACK + maxMetricMessage.
	 */
	public void handleElectionAckMessage() {
		neighBoursReplied++;
		if (allNeighboursReplied()) {
			//if all neighbours replied send to parent my current coordinator.
			sendToNode(currentElectionParentId, MessageTypes.ELECTION_ACK_MESSAGE + " " + maxMetricMessage);
			clearElection();
		}
	}
	
	/**
	 * When a ParkNode leaves or gets terminated each 
	 * one of it ParkNode neighbours removes it from 
	 * their neighbours list. To avoid having this 
	 * list modified while at the same time it is used
	 * for another purpose, e.g. to broadcast an 
	 * ELECTION_MESSAGE message to neighbours
	 * (remember mutual exclusion has been implemented
	 * only for the common resource Resource.txt and not for 
	 * the neighbours list of the individual ParkNodes)
	 * after the neighbour is removed from the list 
	 * the thread in which the ParkNode runs sleeps 
	 * for 2 seconds. That way conflicts are avoided. 
	 * If the node that leaves or gets terminated is 
	 * the coordinator node, then a neighbour of the 
	 * node is assigned with the attribute of being 
	 * the new election designated starter.
	 * @param parkNodeId The id of the node to remove.
	 */
	public void handleNodeLeavingMessage(String parkNodeId) {
		neighbours.remove(returnNeighbourById(parkNodeId));
		sleep(2000); // To avoid parallel modification of the neighbors' arraylist (node/coordinator removal & election related operations)
		if (parkNodeId.equals(currentCoordinatorId)) {
			coordinatorLeft(null);
		}
		if (isCoordinator && designatedElectionStarter.getId().equals(parkNodeId)) {
			pickDesignatedElectionStarter();
		}
	}
	
	/**
	 * Node lets all the neighbours know that the coordinator is
	 * down and then remove it as well from itself. 
	 * @param parkNodeId
	 */
	public void handleCoordinatorLeftMessage(String parkNodeId) {
		if (currentCoordinatorId != null) {
			neighbours.remove(returnNeighbourById(currentCoordinatorId));
			sleep(2000); 	// To avoid parallel modification of the neighbors' arraylist (node/coordinator removal & election related operations)
			coordinatorLeft(parkNodeId);
		}
	}
	
	/**
	 * Method that manages the value of max metric.
	 * During an election, when the ParkNode receives
	 * back a message from any of its children nodes
	 * it has to keep the highest metric received, 
	 * and when all of its children nodes have provided
	 * a highest metric, it sends the one that is the 
	 * highest of all the metrics provided from its 
	 * children and its own to its own parent, or 
	 * if the parent node is also the coordinator 
	 * it just sends a broadcast message with information
	 * about which ParkNode is the new coordinator and 
	 * then ends the election.
	 * @param maxMetricReplyParameter The maximum metric.
	 */
	public void handleMetricReceivedMessage(String maxMetricReplyParameter) {
		neighBoursReplied++;
		String trimmedMaxMetricReplyParameter = maxMetricReplyParameter.
		    substring(1, maxMetricReplyParameter.length() - 1); // https://stackoverflow.com/questions/8846173/how-to-remove-first-and-last-character-of-a-string
		String[] tokenizedTrimmedMaxMetricReplyParameter = trimmedMaxMetricReplyParameter.split(",");
		String receivedMaxMetricId = tokenizedTrimmedMaxMetricReplyParameter[0];
		int receivedMaxMetricValue = Integer.parseInt(tokenizedTrimmedMaxMetricReplyParameter[1]);
		String receivedMaxMetricIp = tokenizedTrimmedMaxMetricReplyParameter[2];
		String receivedMaxMetricPort = tokenizedTrimmedMaxMetricReplyParameter[3];
		/* The child provided its parent with a metric value higher than the one the parent value 
		 * currently holds. Now, the parent node will consider the value received by its child 
		 * node as the best candidate and will forward that info unless another child node provides
		 *  info about a node with even higher metric value.
		 */
		if (receivedMaxMetricValue > currentMaxMetric) {
			currentEligibleCoordinatorId = receivedMaxMetricId;
			currentEligibleCoordinatorIp = receivedMaxMetricIp;
			currentEligibleCoordinatorPort = receivedMaxMetricPort;
			currentMaxMetric = receivedMaxMetricValue;
			maxMetricMessage = maxMetricReplyParameter;
		}
		/*
		 * In addition with the previous if statement, 
		 * if all the neighbours have replied and this 
		 * is the node that started the election we
		 * send the COORDINATOR message. 
		 */
		if (startedElection && allNeighboursReplied()) {
			currentCoordinatorId = currentEligibleCoordinatorId;
			currentCoordinatorIp = currentEligibleCoordinatorIp;
			currentCoordinatorPort = currentEligibleCoordinatorPort;
			if (currentCoordinatorId.equals(id)) {
				setSelfAsCoordinator();
			}
			//broadcast new coordinator message to all neighbours.
			broadcastToArrayListNodes(MessageTypes.NEW_COORDINATOR_MESSAGE + " " +
						currentEligibleCoordinatorId + " " + currentEligibleCoordinatorIp +
						" " + currentEligibleCoordinatorPort, neighbours, null);
			startedElection = false;
			expectingCoordinatorInfo = false;
			clearElection();
		}
		/**
		 * If we are not the node that started the election,
		 * then we forward the ELECTION-ACK + max metric value we have 
		 * derived by communicating with the neighbours, to our 
		 * parent node.
		 */
		else if (!startedElection && allNeighboursReplied()) {
			sendMaxMetricMessageToParent();
		}
	}
	
	/**
	 * Method that puts together the NEW_COORDINATOR_MESSAGE message.
	 * If the ParkNode happens to be the coordinator 
	 * then it sets itself as coordinator. When the message is structured
	 * it is broadcasted to the list of neighbours.
	 * @param parkNodeId       The id of the node that sent the message.
	 * @param tokenizedMessage Contains the information for the new coordinator.
	 */
	public void handleNewCoordinatorMessage(String parkNodeId, String[] tokenizedMessage) {
		if (expectingCoordinatorInfo) {
			currentCoordinatorId = tokenizedMessage[1];
			currentCoordinatorIp = tokenizedMessage[2];
			currentCoordinatorPort = tokenizedMessage[3];
			broadcastToArrayListNodes(MessageTypes.NEW_COORDINATOR_MESSAGE+ " " + currentCoordinatorId + " " + currentCoordinatorIp + " " + currentCoordinatorPort, neighbours, parkNodeId);
			//
			if (currentCoordinatorId.equals(id)) {
				setSelfAsCoordinator();
			}
			expectingCoordinatorInfo = false;
		}
	}
	
	/**
	 * Update the information for the new coordinator and add 
	 * the node that informed you about  which is the new
	 * coordinator to the neighbours list. Then set the 
	 * expectingCoordinatorInfo boolean to false.
	 * @param parkNodeId        The id of the node that sent/forwarded the new coordinator information.
	 * @param parkNodeIp        The ip of the node that sent/forwarded the new coordinator information.
	 * @param parkNodePort      The port that the node that sent/forwarded the new coordinator information is listening from.
	 * @param tokenizedMessage  The information about the new coordinator (id, ip, port).
	 */
	public void handleExistingCoordinatorMessage(String parkNodeId, String parkNodeIp, String parkNodePort, String[] tokenizedMessage) {
		NodeToCommunicateWith newNeighbour = new NodeToCommunicateWith(parkNodeId, parkNodeIp, parkNodePort);
		addNeighbour(newNeighbour);
		currentCoordinatorId = tokenizedMessage[1];
		currentCoordinatorIp = tokenizedMessage[2];
		currentCoordinatorPort = tokenizedMessage[3];
		expectingCoordinatorInfo = false;
	}
	
	/**
	 * When a node requests access to the common resource 
	 * by the coordinator, it obtains the permission to 
	 * access it by receiving a message directly from the 
	 * coordinator with the ACCESS_GRANTED relevant message.
	 * If that ACCESS_GRANTED is indeed from the node that 
	 * everyone in the network knows as the coordinator and 
	 * the node does actually expect a reply from the 
	 * coordinator then it accesses the common resource, Resource.txt 
	 * @param parkNodeId The coordinator's id.
	 */
	public void handleGrantedResourceAccessMessage(String parkNodeId) {
		if (parkNodeId.equals(currentCoordinatorId) && pendingCoordinatorReply) {
			pendingCoordinatorReply = false;
			accessResource();
		}
	}
	
	/**
	 * The node was denied access to the resource by the coordinator.
	 * @param parkNodeId the id of the coordinator.
	 */
	public void handleDeniedResourceAccessMessage(String parkNodeId) {
		if (parkNodeId.equals(currentCoordinatorId) && pendingCoordinatorReply) {
			pendingCoordinatorReply = false;
		}
	}
	
	/**
	 * Send highest metric to parent node. 
	 */
	public void sendMaxMetricMessageToParent() {
		sendToNode(currentElectionParentId, MessageTypes.ELECTION_ACK_MESSAGE + " " + maxMetricMessage);
		clearElection();
	}
	
	/**
	 * Check if all neighbours have replied.
	 * @return True if all neighbours have replied.
	 */
	public boolean allNeighboursReplied() {
		if (startedElection) {
			return neighBoursReplied >= neighbours.size(); //he who starts the election has no parent so expects reply from everyone.
		}
		return neighBoursReplied >= neighbours.size() - 1; //they have a parent
	}
	
	/**
	 * Set parent node. If node has already a
	 * parent-node, then it sends back to the 
	 * node from which received the current 
	 * ELECTION message (this is at least the 
	 * second election message the node receives
	 * if it has already been assigned with a 
	 * parent node) an ELECTION-ACK message.
	 * When the ParkNode sets a parent ParkNode,
	 * it sends the ELECTION_MESSAGE to all of 
	 * its neighbours but it parent and when 
	 * all of them have replied with results 
	 * it forwards the result with the info
	 * about the ParkNode with the highest
	 * metric back to its parent ParkNode.
	 * @param parkNodeId the parent's node ID (the node that 
	 *                  sent/forwarded to us the election message)
	 */
	public void handleElectionMessage(String parkNodeId) {
		//Does not have parent node.
		if (currentElectionParentId == null && !startedElection) {
			currentElectionParentId = parkNodeId;
			printToConsole("Set parent: " + currentElectionParentId);
			expectingCoordinatorInfo = true;
			broadcastToArrayListNodes(MessageTypes.ELECTION_MESSAGE, neighbours, currentElectionParentId);
			if (allNeighboursReplied()) //All neighbours have replied so send max-metric back to parent.
				sendMaxMetricMessageToParent();
		}
		//It does have parent node.
		else {
			sendToNode(parkNodeId, MessageTypes.ELECTION_ACK_MESSAGE);
		}
	}
	
	/**
	 * Prints the message received from the console.
	 * A user can start an ELECTION by typing ELECTION
	 * to the console of one of the running nodes, or
	 * terminate the node by typing LEAVE in the console.
	 * @param message The message entered. Valid messages ELECTION, LEAVE.
	 */
	public void receiveFromConsole(String message) {
		printToConsole("Received from Console: " + message);
		if (message.equals(MessageTypes.ELECTION_MESSAGE)) {
			startedElection = true;
			broadcastToArrayListNodes(message, neighbours, null); //broadcast ELECTION_MESSAGE message to neighbours.
		}
		else if (message.equals(MessageTypes.LEAVE_MESSAGE))
			leaveNetwork();
	}
	
	/**
	 * Check that there are at least two neighbours
	 * in the neighbours ArrayList.
	 * @return true if there are more than two neighbours in the 
	 *         neighbours ArrayList, false otherwise.
	 */
	public boolean checkAtLeastTwoNeighbors() {
		if (neighbours.size() < 2)
			return false;
		return true;
	}
	
	/**
	 * Print message to console proceeded by 
	 * date and the node's id.
	 * @param message The message to print.
	 */
	public void printToConsole(String message) {
		System.out.println("[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(new Date()) + " | " + id + " ] " + message);
	}
	
	/**
	 * Leave network.
	 */
	public void leaveNetwork() {

		broadcastToArrayListNodes(MessageTypes.NODE_LEAVING_MESSAGE, neighbours, null);
		printToConsole("LEAVING...");
		System.exit(0);
	}
	
	/**
	 * Clear election.
	 */
	public void clearElection() {
		currentElectionParentId = null;
		neighBoursReplied = 0;
		currentMaxMetric = metricValue;
		currentEligibleCoordinatorId = id;
		currentEligibleCoordinatorIp = ipAddress;
		currentEligibleCoordinatorPort = port;
		maxMetricMessage = "[" + id + "," + metricValue + "," + ipAddress + "," + port + "]";
	}
	
	/**
	 * Access the common resource, Resource.txt
	 * (after permission has been
	 * granted by the coordinator.
	 */
	public void accessResource() {
		
		if (ticketIdToEnter != null) {
			printToConsole(MessageTypes.CHECKING_ENTER_ELIGIBILITY_MESSAGE + ticketIdToEnter);
			
			if (SharedResourceUtilities.enterFunctionality(ticketIdToEnter)) {
				printToConsole(MessageTypes.ENTRY_ALLOWED_MESSAGE + ticketIdToEnter);
			}
			else {
				printToConsole(MessageTypes.ENTRY_DENIED_MESSAGE + ticketIdToEnter + ". " + MessageTypes.NO_SPACE_AVAILABLE_MESSAGE);
			}
			ticketIdToEnter = null;
		}
		if (ticketIdToExit != null) {
			printToConsole(MessageTypes.CHECKING_ENTER_ELIGIBILITY_MESSAGE + ticketIdToExit);
			// Code for actual check and editing
			if (SharedResourceUtilities.exitFunctionality(ticketIdToExit)) {
				printToConsole(MessageTypes.EXIT_ALLOWED_MESSAGE + ticketIdToExit);
			}
			else {
				printToConsole(MessageTypes.EXIT_DENIED_MESSAGE + ticketIdToExit + ". " + MessageTypes.NOT_A_VALID_ENTRY_MESSAGE);
			}
			ticketIdToExit = null;
		}
		if (!isCoordinator)
			sendToCoordinator(MessageTypes.RELEASING_RESOURCE_MESSAGE);
		else
			handleMessageAsCoordinator(id, MessageTypes.MYSELF_MESSAGE, MessageTypes.MYSELF_MESSAGE, MessageTypes.RELEASING_RESOURCE_MESSAGE);
	}
	
	/**
	 * Get id.
	 * @return The node id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set ID.
	 * @param id The node ID.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get ip address.
	 * @return the ip address.
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Set ip address.
	 * @param ipAddress the ip address.
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * Get port number.
	 * @return Return the port number.
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Set port number.
	 * @param port the port number.
	 */
	public void setPort(String port) {
		this.port = port;
	}

	/**
	 * Get current coordinator.
	 * @return The current coordinator
	 */
	public String getCurrentCoordinator() {
		return currentCoordinatorId;
	}

	/**
	 * Set current coordinator.
	 * @param currentCoordinator The current coordinator.
	 */
	public void setCurrentCoordinator(String currentCoordinator) {
		this.currentCoordinatorId = currentCoordinator;
	}

	/**
	 * Check if the node is a coordinator.
	 * @return true if coordinator.
	 */
	public boolean isCoordinator() {
		return isCoordinator;
	}

	/**
	 * Set coordinator
	 * @param isCoordinator set to true if coordinator.
	 */
	public void setSelfAsCoordinator() {
		this.isCoordinator = true;
		queueOfRequestingNodes = new LinkedList<NodeToCommunicateWith>();
		currentResourceUtilizingNode = null;
		pickDesignatedElectionStarter();
	}
	
	/**
	 * Pick a designated election starter node from 
	 * the list of neighbour nodes of the coordinator
	 * node. Specifically, the node chosen is by 
	 * default the first in the neighbours ArrayList.
	 */
	public void pickDesignatedElectionStarter() {
		if (!neighbours.isEmpty()) {
			designatedElectionStarter = neighbours.get(0);
			sendToNode(designatedElectionStarter, MessageTypes.DESIGNATED_ELECTION_STARTER_MESSAGE);
		}
	}
	
	/**
	 * Set up socket communication.
	 * @param ip      the node ip.
	 * @param port    the port of the node.
	 * @param message the message.
	 * @throws UnknownHostException host not found.
	 * @throws IOException IO problems.
	 */
	public void runMessenger(String id, String ip, int port, String message) throws UnknownHostException, IOException {
		parkNodeCommunicator.runMessenger(id, ip, port, message);
	}

	/**
	 * Get current max metric
	 * @return the current max metric the node is aware about.
	 */
	public int getCurrentMaxMetric() {
		return currentMaxMetric;
	}

	/**
	 * Set the current max metric.
	 * @param currentMaxMetric The current max metric.
	 */
	public void setCurrentMaxMetric(int currentMaxMetric) {
		this.currentMaxMetric = currentMaxMetric;
	}

	/**
	 * Get metric value.
	 * @return The metric value.
	 */
	public int getMetricValue() {
		return metricValue;
	}

	/**
	 * Set metric value of node.
	 * @param metricValue the metric value.
	 */
	public void setMetricValue(int metricValue) {
		this.metricValue = metricValue;
	}
}
