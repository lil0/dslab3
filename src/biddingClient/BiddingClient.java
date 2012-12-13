package biddingClient;

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
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Hex;

import auctionServer.AuctionServer;
import auctionServer.ServerThread;
import channels.TCPChannel;

/* Should not be needed anymore
import java.net.DatagramSocket;
 */

public class BiddingClient {
	//Input params
	public static String host;
	public static int tcpPort;
	public static String userName;
	public static Socket clientSocket;
	public static TCPChannel tcpChannel; 
	public static Key sharedKey;		// Shared secret key of client, shared with auctionServer
	
	/* UDP port should not be needed, therefore not handled as parameter?
	public static int udpPort;
	 */
	public static void main(String[] args) {
		clientSocket = null;
		/* Nont needed
		DatagramSocket udpSocket = null;
		 */
		PrintWriter out = null;
		BufferedReader stdin = null;
		userName = "";

		if (args.length == 5) {
			host = args[0];
			tcpPort = Integer.parseInt(args[1]);
			/* Not needed
			udpPort = Integer.parseInt(args[2]);
			 */
			if (checkPort(tcpPort)) {
				stdin = new BufferedReader(new InputStreamReader(System.in));
				String line;
				/* Should not be needed
				try {
					udpSocket = new DatagramSocket(udpPort);
				} catch (SocketException e1) {
					usage("Error binding UDP socket.");
					System.exit(-1);
				}
				new ClientUdpThread(udpSocket).start();
				 */

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
						// removed the udpPort from the !login command
						// out.println(line + " " + udpPort);
						tcpChannel.send(line);
						//out.println(line);
						userName = split[1];
						
						// Try reading client secret key
						try {
							byte[] keyBytes = new byte[1024];
							String pathToSecretKey = AuctionServer.clientsKeyDir + userName + ".key";
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
						//out.println(line);
						tcpChannel.send(line);
						userName = "";
						sharedKey = null;
					} else if (line.equals("!list")) {
						//out.println(line);
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
						int auctionId;
						double bidAmount;
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
		private static boolean compareHMAC (String message, String hmacToCompare) {
			Key secretKey = AuctionServer.userKeys.get(userName);
			byte[] hash = null;
			
			try {
				Mac hMac = Mac.getInstance("HmacSHA256"); 
				hMac.init(secretKey);
				// MESSAGE is the message to sign in bytes 
				hMac.update(message.getBytes());
				hash = hMac.doFinal();
			} catch (NoSuchAlgorithmException e) {
				System.out.println("Error: Algorithm unknown");
			} catch (InvalidKeyException e) {
				System.out.println("Error: Invalid key");
			}
			
			if (hash.toString().equals(hmacToCompare)) {
				return true;
			} else {
				return false;
			}
		}
	public static void usage(String message) {
		//String messag = tcpChannel.receive();
		if (message.equals("You have been logged out.")) {
			userName = "";
		} 
		System.out.println(message);
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