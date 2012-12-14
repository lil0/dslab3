package biddingClient;

//TODO: Cleanup terminal message logic, print ">" through client and not server
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;

import sun.misc.BASE64Encoder;

import auctionServer.AuctionServer;
import auctionServer.ServerThread;
import channels.TCPChannel;

//TODO: test wrong hmacs with wrong keyfile on server

public class BiddingClient {
	//Input params
	public static String host;
	public static int tcpPort;
	public static String keyFolder;
	public static String userName;
	public static Socket clientSocket;
	public static TCPChannel tcpChannel;
	// Shared secret key of client, shared with auctionServer
	public static Key sharedKey;		
	private static boolean repeated;
	private static String listMessage;
	
	public static void main(String[] args) {
		clientSocket = null;
		listMessage = "";
		PrintWriter out = null;
		BufferedReader stdin = null;
		userName = "";
		repeated = false;
		
		if (args.length == 5) {
			host = args[0];
			tcpPort = Integer.parseInt(args[1]);
			keyFolder = args[4];
			
			if (checkPort(tcpPort)) {
				stdin = new BufferedReader(new InputStreamReader(System.in));
				String line;

				// Try opening Sockets and Channels
				try {
					clientSocket = new Socket(args[0], Integer.parseInt(args[1]));
					tcpChannel = new TCPChannel(clientSocket);
					//out = new PrintWriter(clientSocket.getOutputStream(), true);
					new ClientTcpThread(clientSocket).start();

				} catch (UnknownHostException e) {
					usage("Unknown host.");
					System.exit(-1);
				} catch (IOException e) {
					usage("Connection failed.");
					System.exit(-1);
				}

				while (true) {
					line = "";
					try {
						System.out.print(userName + "> ");
						line = stdin.readLine();
					} catch (IOException e) {
						// Close ressources?
						System.exit(-1);
					}
					String[] split = line.split(" ");

					if (line.startsWith("!login ") && split.length == 2) {
						tcpChannel.send(line);
						userName = split[1];
						
						// Try reading client secret key
						try {
							byte[] keyBytes = new byte[1024];
							String pathToSecretKey = keyFolder + userName + ".key";
							FileInputStream fis = new FileInputStream(pathToSecretKey);
							fis.read(keyBytes);
							fis.close();
							byte[] input = Hex.decode(keyBytes);
							Key key = new SecretKeySpec(input,"HmacSHA256");
							sharedKey = key;
						} catch (Exception e) {
							System.out.println("Error: Could not load Secret key");
						}
						
					} else if (line.equals("!logout")) {
						tcpChannel.send(line);
						userName = "";
						sharedKey = null;
					} else if (line.equals("!list")) {
						tcpChannel.send(line);
					} else if ((line.startsWith("!create ")) && (split.length >= 3)) {
						try {
							if ((Integer.parseInt(split[1]) > 0) && (Integer.parseInt(split[1]) < 1000000)) {
								//out.println(line);
								tcpChannel.send(line);
							}
						} catch (NumberFormatException e) {
							System.out.println("Enter a valid duration");
						}
					} else if (line.startsWith("!bid ") && split.length == 3) {
						try {
							Integer.parseInt(line.split(" ")[1]);
							Double.parseDouble(line.split(" ")[2]);
						} catch (NumberFormatException e) {
							System.out.println("Error: Please enter correct values");
						}
						//out.println(line);
						tcpChannel.send(line);
					} else if (line.equals("!end")) {
						//out.println(line);
						tcpChannel.send(line);
						try {
							//out.close();
							tcpChannel.close(); 
							stdin.close();
							clientSocket.close();
							/* Not necessary, since no UDPsocket is used
							udpSocket.close();
							 */
						} catch (IOException e) {
							usage("error freeing ressources");
							System.exit(-1);
						}
						shutdown();
					} else {
						System.out.println("command not recognized.");
					}
					try {
						Thread.sleep(100);
					} catch(InterruptedException ex) {
						Thread.currentThread().interrupt();
					}
				}
			} else {
				usage("wrong port value(s).");
				System.exit(-1);
			}
		} else {
			usage("wrong argument count.");
			System.exit(-1);
		}	
	}
	// Static function to calculate an HMAC for a given String
		private static boolean validHMAC (String message, String hmacToCompare) {
			byte[] hash = null;
			BASE64Encoder encoder = new BASE64Encoder();
			
			try {
				Mac hMac = Mac.getInstance("HmacSHA256"); 
				hMac.init(sharedKey);
				hMac.update(message.getBytes());
				hash = hMac.doFinal();
			} catch (NoSuchAlgorithmException e) {
				System.out.println("Error: Algorithm unknown");
			} catch (InvalidKeyException e) {
				System.out.println("Error: Invalid key");
			}
			
			return encoder.encode(hash).equals(hmacToCompare);
		}
	public static void usage(String message) {
		if (message.equals("You have been logged out.")) {
			userName = "";
		} 
		
		// if message contains hmac
		String controlSequence = "-999-";
		String helpString = message;
		//DEBUG
		//System.out.println("Received message ::" + message + "::");
		//DEBUG
		
		if (message.contains(">") && message.split(">").length > 1) {
			helpString = message.split(">")[1];
		} 
		/* 3 different cases:
		1. the message is part of the list command: it starts with a digit or with an empty message
		2. the message is a crucial message with controlSequence
		3. the message is a non-crucial message
		*/
		if (Character.isDigit(helpString.charAt(0)) || helpString.split(controlSequence)[0].equals("")) {
			// Check if its part of list command or already the HMAC
			if (!helpString.contains(controlSequence)) {
				listMessage = listMessage + helpString + "\n";
				
				//DEBUG
				//System.out.println("String matches controlSequence? ::" + helpString.matches(controlSequence) + "::");
				//System.out.println("the current listMessage ::" + listMessage + "::");
				//
			} else {
				String listHash = helpString.split(controlSequence)[1];
				if (!validHMAC(listMessage, listHash)) {
					// if message hasn't been repeated
					
					//DEBUG
					//System.out.println("The listmessage could not be matched :(");
					//DEBUG
					
					if (repeated == false) {
						tcpChannel.send("!repeat");
						repeated = true;
					} else {
						repeated = false;
					}
				} else {
					//DEBUG
					//System.out.println("The listmessage could be matched!!!");
					//DEBUG
					repeated = false;
				}
				//DEBUG
				//System.out.println("The complete listmessage is ::" + listMessage + "::");
				//DEBUG
				System.out.println(listMessage);	// IM HERE
				listMessage = "";
			}
		} else if (message.contains(controlSequence)) {
			String realMessage = message.split(controlSequence)[0];
			String hmac = message.split(controlSequence)[1];
			
			if (message.contains(">") && realMessage.split(">").length > 1) {
				realMessage = realMessage.split(">")[1];
			} 
			
			// if hmac is incorrect
			if (!validHMAC(realMessage, hmac)) {
				
				// if message hasn't been repeated
				if (repeated == false) {
					tcpChannel.send("!repeat");
					repeated = true;
				} else {
					repeated = false;
				}
			} else {
				repeated = false;
			}
			System.out.println(realMessage);
		} else {
			System.out.println(message);
		}
	}
	public static void shutdown() {
		// Kill everything properly and shut down
		// Kill udpSocket?
		System.exit(1);
	}
	public static boolean checkPort(int port) {
		if ((port < 1024) || (port > 65535)) {
			return false;
		} else {
			return true;
		}
	}
}