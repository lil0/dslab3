package auctionServer;

import java.net.*;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.Key;
import javax.crypto.spec.SecretKeySpec;

import java.sql.Timestamp;
import java.io.*;

import org.bouncycastle.util.encoders.Hex;

import event.AuctionEvent;
import event.UserEvent;

import analyticsServer.AnalyticsRMIHandler;
import analyticsServer.AnalyticsRMIInterface;
import billingServer.BillingServer;
import billingServer.BillingServerSecure;

import managementClient.EventListener;
import managementClient.EventListenerInterface;

public class ServerThread extends Thread {
	private Socket socket = null;
	public String userName;
	public boolean loggedIn;
	protected String analName;
	protected String billName;
	protected BillingServer billingServerHandler;
	public static BillingServerSecure billingServerSecureHandler;
	protected static AnalyticsRMIInterface analyticsHandler;
	Registry registry = null;
	protected static String registryHost;
	protected static int registryPort;

	public ServerThread(Socket socket, String analName, String billName) {
		super("ServerThread");
		this.socket = socket;
		this.analName = analName;
		this.billName = billName;
	}

	public void run() {
		try {
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String inputLine, outputLine;
			inputLine = "";
			AuctionProtocol auctionP = new AuctionProtocol();

			while (true) {
				try {

					readProperties();

					// Connect to registry
					try {

						registry = LocateRegistry.getRegistry(registryHost,registryPort);
					} catch (Exception e) {
						System.out.println("Couldn't find registry.");
					}
					// Bind Analytics remote object
					try {
						analyticsHandler = (AnalyticsRMIInterface) registry.lookup(analName);
					} catch (AccessException e) {
						System.out.println("Couldn't access registry");
					} catch (RemoteException e) {
						System.out.println("Couldn't connect to Analytics Server");
					} catch (NotBoundException e) {
						System.out.println("Analytics Server not bound to the registry");
					}

					//billingServerLogin();

					// Bind Billing remote object
					try {
						billingServerHandler = (BillingServer) registry.lookup(billName);
						billingServerLogin();
					} catch (AccessException e) {
						System.out.println("Couldn't access registry");
					} catch (RemoteException e) {
						System.out.println("Couldn't connect to Billing Server");
					} catch (NotBoundException e) {
						System.out.println("Billing Server not bound to the registry");
					}

					if (userName == null) {
						out.print(">");
					} else {
						out.print(userName + ">");
					}
					inputLine = in.readLine();
				} catch (IOException e) {
					// Close ressources?
					System.exit(-1);
				}
				
				if (inputLine.startsWith("!login")) {
					if (!loggedIn) {
						userName = inputLine.split(" ")[1];
						/* UDP port should not be passed on with !login command
	        			udpPort = Integer.parseInt(inputLine.split(" ")[2]);
						 */

						if (!AuctionServer.userHostnames.containsKey(userName)) {
							loggedIn = true;

							AuctionServer.userHostnames.put(userName, socket.getInetAddress().getHostAddress());

							// Call ProcessEvent from AnalyticsHandler for LOGIN Event
							Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
							long timestamp = logoutTimestamp.getTime();
							try {
								analyticsHandler.processEvent(new UserEvent("USER_LOGIN", timestamp, userName));
							} catch (RemoteException e) {
								System.out.println("Couldn't connect to Analytics Server");
							} catch (Exception e) {
								System.out.println("Error processing event USER_LOGIN");
							}

							try {
								byte[] keyBytes = new byte[1024];
								String pathToSecretKey = AuctionServer.clientsKeyDir + userName + ".key";
								FileInputStream fis = new FileInputStream(pathToSecretKey);
								fis.read(keyBytes);
								fis.close();
								byte[] input = Hex.decode(keyBytes);
								Key key = new SecretKeySpec(input,"HmacSHA256");
								AuctionServer.userKeys.put(userName, key);
							} catch (FileNotFoundException e) {
								// TODO: should sth be done here?
							}

							out.println("Successfully logged in as " + userName + "!");
							auctionP.processInput(inputLine);
						} else {
							out.println("User is already logged in!");
						}
					} else {
						out.println("You are already logged in, please log out first.");
					}
				} else if (inputLine.equals("!logout")) {
					if (loggedIn == true) {

						AuctionServer.userHostnames.remove(userName);

						// Call ProcessEvent from AnalyticsHandler for LOGOUT Event
						Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
						long timestamp = logoutTimestamp.getTime();
						try {
							System.out.println("Trying to call RMI USER_LOGOUT");
							analyticsHandler.processEvent(new UserEvent("USER_LOGOUT", timestamp, userName));
						} catch (RemoteException e) {
							System.out.println("Couldn't connect to Analytics Server");
						} catch (Exception e) {
							System.out.println("Error processing event USER_LOGOUT");
						}

						out.println("Successfully logged out as " + userName + "!");

						loggedIn = false;
						userName = null;
					} else {
						out.println("You have to log in first!");
					}
				} else if (inputLine.equals("!end")) {
					break;	
				} else if (inputLine.equals("!repeat")) {
					//Resend last message to client
					String lastMessage = AuctionServer.userLastMessage.get(userName);
					out.println(AuctionProtocol.appendHMAC(lastMessage));
				} else {
					if (loggedIn) {
						if (inputLine.startsWith("!create ") || inputLine.startsWith("!bid ") || inputLine.equals("!list")) {
							outputLine = auctionP.processInput(inputLine);
							out.println(outputLine);
						} else {
							out.println("Unrecognized command.");
						}
					} else {
						out.println("You must be logged in for this command to work.");
					}
				}
			}
			out.close();
			in.close();
			socket.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	/*
	private void answerClient(String message) {
		try {
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			out.println(message);
		} catch (Exception e) {
			System.out.println("Error answering client!");
		}
	}
	*/
	private void billingServerLogin() {
		if (billingServerHandler != null) {
			try {
				BillingServerSecure bss = billingServerHandler.login("auctionServer", "auctionServer123");
				billingServerSecureHandler =  bss; 

				if (billingServerSecureHandler == null) {
					System.out.println("Login to billing server failed");
				}
			} catch (RemoteException ex) {
				System.out.println("Billing Server login Remote Exception");
			}
		} else {
			System.out.println("Not connected to the billing server"); // log
		}
	}
	private static void readProperties() {
		java.io.InputStream is = ClassLoader.getSystemResourceAsStream("registry.properties");
		if (is != null) {
			java.util.Properties props = new java.util.Properties();
			try {
				try {
					props.load(is);
				} catch (IOException e) {
					System.out.println("Error handling configuration file.");
				}
				registryHost = props.getProperty("registry.host");
				registryPort = Integer.parseInt(props.getProperty("registry.port"));

			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("Properties file not found!");
		}
	}
}