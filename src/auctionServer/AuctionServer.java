package auctionServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.Key;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class AuctionServer {

	public static int tcpPort;
	public static String serverKey;
	public static String clientsKeyDir;
	
	// User data structures
	public static Map<String, String> userHostnames = Collections.synchronizedMap(new HashMap<String, String>());
	public static Map<String, String> userMissed = Collections.synchronizedMap(new HashMap<String, String>());
	//used for storing secret keys of clients
	public static Map<String, Key> userKeys = Collections.synchronizedMap(new HashMap<String, Key>());
	//used for storing last message of clients
	public static Map<String, String> userLastMessage = Collections.synchronizedMap(new HashMap<String, String>());
	
	// Auction data structures
	public static Map<Integer, String> auctionDescription = Collections.synchronizedMap(new HashMap<Integer, String>());
	public static Map<Integer, Timestamp> auctionEndtime = Collections.synchronizedMap(new HashMap<Integer, Timestamp>());
	public static Map<Integer, Double> auctionHighestBid = Collections.synchronizedMap(new HashMap<Integer, Double>());
	public static Map<Integer, String> auctionHighestBidder = Collections.synchronizedMap(new HashMap<Integer, String>());
	public static Map<Integer, String> auctionOwner = Collections.synchronizedMap(new HashMap<Integer, String>());
	public static Map<Integer, Integer> auctionDuration = Collections.synchronizedMap(new HashMap<Integer, Integer>());

	public static int auctionCounter = 0;
	public static boolean listening;
	public static ServerSocket serverSocket;

	public static void main(String[] args) {
		if (args.length == 5) {
			tcpPort = Integer.parseInt(args[0]);
			String analBind = args[1];
			String billBind = args[2];


			// TODO: ADD param 3 and 4 handling
			serverKey =  args[3];
			clientsKeyDir = args[4];

			if (tcpPort >= 1024 || tcpPort >= 65535) {
				serverSocket = null;
				listening = true;

				try {
					serverSocket = new ServerSocket(tcpPort);
				} catch (IOException e) {
					System.err.println("Could not listen on port: " + tcpPort + ".");
				}
				try {
					(new EnterKiller()).start();

					while (listening)
					{
						new ServerThread(serverSocket.accept(), analBind, billBind).start();
					}
					serverSocket.close();
				} catch (SocketException e) {
					// TODO Auto-generated catch block
					System.out.println("Socket was closed...stopped accepting requests.");
					System.exit(0);
				} catch (Exception e) {
					System.out.println("Error with Socket");
					System.exit(-1);
				}
			} else {
				System.out.println("Wrong argument value.");
			}
		} else {
			System.out.println("Wrong argument count.");
		}
	}
	public boolean containsUser(String username) {
		synchronized(userHostnames){

			if (userHostnames.containsKey(username)) {
				return true;
			} else {
				return false;
			}
		}
	}
	public String userHost(String username) {
		synchronized(userHostnames){

			return userHostnames.get(username);
		}
	}
	/*
	public int userUdpPort (String username) {
		return userPorts.get(username);
	}
	 */
	public void addUser(String username, String hostname, int udpPort) {
		synchronized(userHostnames){
			userHostnames.put(username, hostname);
			/* Should not be needed anymore
                    userPorts.put(username, udpPort);
			 */
		}
	}
	public void removeUser(String username) {
		synchronized(userHostnames){
			userHostnames.remove(username);
			/* Should not be needed anymore
                    userPorts.remove(username);
			 */
		}
	}
}
class EnterKiller extends Thread {
	public void run() {
		//Handle Closing with Enter 
		while (true) {
			String line = "";
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
			try {
				line = stdin.readLine();
			} catch (IOException e) {
				// Close ressources?
				System.exit(-1);
			}


			if (line.equals("")) {
				if (!AuctionServer.userHostnames.isEmpty()) {
					/*	To-Do: Add shutdown handling: Log out all users, free ressources 

					// If there are logged in users
					Iterator<?> iter = Server.userHostnames.entrySet().iterator();
					while (iter.hasNext()) {
						Map.Entry entry = (Map.Entry) iter.next();
						String hostname = entry.getValue().toString();
						String username = entry.getKey().toString();
						int port = Server.userPorts.get(entry.getKey());

						//DEBUG
						System.out.println("Current user name: " + username);
						//DEBUG

						// Connect to Socket, send Logoff - message
						Socket socket = null;
				        PrintWriter out = null;

						try {
							//DEBUG
							System.out.println("Trying to establish connection to client. Host: " + hostname + ", port: " + port);

							socket = new Socket(hostname, port);
				            out = new PrintWriter(socket.getOutputStream(), true);
				            System.out.print("You have been logged out.");
				            // Take further log out measures?
						} catch (UnknownHostException e) {
				            System.err.println("Don't know about host");
				            System.exit(1);
				        } catch (IOException e) {
				            System.err.println("Couldn't get I/O for the connection");
				            System.exit(1);
				        }
					}
					 */

				}
				try {

					AuctionServer.serverSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				AuctionServer.listening = false;
			}
		}

	}
}
