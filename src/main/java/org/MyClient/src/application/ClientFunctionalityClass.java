package org.MyClient.src.application;

import org.MyAmusementPark.src.nodes.ParkNodesInputType;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

import javax.swing.JOptionPane;

/**
 * 
 * @author dmalonas
 *
 */
public class ClientFunctionalityClass {
	private Socket socket;
	private PrintWriter out;
	private BufferedReader stdIn;
	
	public ClientFunctionalityClass() throws IOException {
		sendRequestToNode();
	}
		
	public void sendRequestToNode() {
	   
		String hostIp = null;
		int hostPort = 0;
		InputStream inputStream = ParkNodesInputType.class.getResourceAsStream("/ClientFile/ClientFile.txt");
		if (inputStream == null) {
			try {
				throw new FileNotFoundException("Resource not found: /ClientFile.txt");
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	    try(BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
	        String line = br.readLine();

	        while (line != null) {
	        	String[] tokenizedMessage = line.split(" ");
	        	hostIp = tokenizedMessage[2];
	        	hostPort = Integer.parseInt(tokenizedMessage[3]);
	        	try {
		        	socket = new Socket(hostIp, hostPort);//Integer.parseInt(tokenizedMessage[3])
				    out = new PrintWriter(socket.getOutputStream(), true);
			    	out.println(tokenizedMessage[0] + " " + tokenizedMessage[1]);
			    	out.close();
	        	}
	        	catch (Exception e) {
	    	    	System.out.println("Cannot find host " + hostIp + ":" + hostPort);
	    	    }
	        	line = br.readLine();
	        }
	    }
	    catch (Exception e) {
	    	System.out.println("Cannot read file");
	    }
	}
}
