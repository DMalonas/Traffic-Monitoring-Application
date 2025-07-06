package org.MyAmusementPark.src.utilities;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 
 * @author dmalonas
 *
 */
public class SharedResourceUtilities {
	
	private final static String sharedResource = "Shared/Resource.txt";
	private static int capacity; //How many people we should allow in the park.
	private static int currentNumberOfVisitors; //How many people are right now inside the park.
	private static ArrayList<String> validTickets; //ArrayList of tickets that are inside the park (so they are valid for exiting it).
	
	/**
	 * Enter functionality. Read file from the client
	 * and if there is room for more visitors within the
	 * park add the ticket id to the validTickets ArrayList.
	 * Then write the ticket id to the common resource, Resource.txt.
	 * @param ticketIdToEnter the ticket  id.
	 * @return true if ENTER request was successful, false otherwise.
	 */
	public static boolean enterFunctionality(String ticketIdToEnter) {
		readFile();
		if (currentNumberOfVisitors < capacity) {
			validTickets.add(ticketIdToEnter);
			currentNumberOfVisitors++;
			writeFile();
			return true;
		}
		return false;		
	}
	
	/**
	 * Method for a ticket to exit the park. 
	 * If the ticket id is already inside 
	 * the validTickets arrayList then the 
	 * ticket can exit the park because it 
	 * is already inside it, otherwise not.
	 * @param ticketIdToEnter the ticket id we used to enter.
	 * @return true if exit successful, false otherwise.
	 */
	public static boolean exitFunctionality(String ticketIdToEnter) {
		readFile();
		if (validTickets.contains(ticketIdToEnter)) {
			validTickets.remove(ticketIdToEnter);
			currentNumberOfVisitors--;
			writeFile();
			return true;
		}
		return false;
	}
	
	/**
	 * 	Reads from the common resource.
	 * (capacity, people inside the park, ticket ids).
	 */
	public static void readFile() {
		capacity = 0;
		currentNumberOfVisitors = 0;
		validTickets = new ArrayList<String>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(sharedResource));
			capacity = Integer.parseInt(br.readLine());
			currentNumberOfVisitors = Integer.parseInt(br.readLine());
			String nextLine;
			while ((nextLine = br.readLine()) != null) {
				validTickets.add(nextLine);
			}
			br.close();
		} catch (NumberFormatException | IOException e) {
			System.out.println("Error reading file");
		}
	}
	
	
	/**
	 * Opens the common resource and 
	 * writes the capacity, the 
	 * current number of visitors
	 * and the ticket ids that 
	 * are inside the park.
	 */
	public static void writeFile() {
		BufferedWriter bw;
		try {
			bw = new BufferedWriter(new FileWriter(sharedResource));
			bw.write(((Integer)capacity).toString());
			bw.newLine();
			bw.write(((Integer)currentNumberOfVisitors).toString());
			int size = validTickets.size();
			int i;
			for (i = 0; i < size; i++) {
				bw.newLine();
				bw.write(validTickets.get(i));
			}
			bw.close();
		}
		catch (IOException e) {
			System.out.println("Error writing to file");
		}
	}

}
