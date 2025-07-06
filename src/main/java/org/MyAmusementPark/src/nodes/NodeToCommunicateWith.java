package org.MyAmusementPark.src.nodes;

/**
 * 
 * @author 171100408
 *
 */
public class NodeToCommunicateWith {
	
	private String id;
	private String ipAddress;
	private String port;
	
	/**
	 * Constructor to initialise class attributes.
	 * @param id
	 * @param ipAddress
	 * @param port
	 */
	public NodeToCommunicateWith(String id, String ipAddress, String port) {
		this.id = id;
		this.ipAddress = ipAddress;
		this.port = port;
	}

	/**
	 * Get id
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set id
	 * @param id the id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Get ip address
	 * @return the ip address
	 */
	public String getIpAddress() {
		return ipAddress;
	}

	/**
	 * Set ip address
	 * @param ipAddress the ip address
	 */
	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * Get port
	 * @return the port
	 */
	public String getPort() {
		return port;
	}

	/**
	 * Set port
	 * @param port the port
	 */
	public void setPort(String port) {
		this.port = port;
	}

}
