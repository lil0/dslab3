package managementClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.ServerException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import analyticsServer.AnalyticsRMIHandler;
import analyticsServer.AnalyticsRMIInterface;
import analyticsServer.AnalyticsServer;
import billingServer.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import event.EventInterface;

import sun.rmi.log.LogOutputStream;

public class ManagementClient {
	/**
	 * Arg 0: Bindingname for AnalyticsServer
	 * Arg 1: BindingName for BillingServer
	 */	
	static String registryHost = "";
	static int registryPort = 0;
	static BillingServer billingServer = null;
	static BillingServerSecure billingServerSecure = null;
	static AnalyticsRMIInterface analyticsHandler = null;
	static String userName = "";
	static String loadTest = "";
	static String line = "";
	static boolean test = false;
	
	public static void main(String[] args) {
		if (args.length == 2 || args.length == 3) {
			String analBind = args[0];
			String billBind = args[1];
			String userPwd = "";
			double startPrice = 0.0;
			double endPrice = 0.0;
			double fixedPrice = 0.0;
			double variablePrice = 0.0;
			String userBill = "";
			String regex = "";
			int subscriptionId = 0;
			Registry registry = null;
			EventInterface eventListener = null;
			BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

			readProperties();

			// Connect to registry
			try {

				registry = LocateRegistry.getRegistry(registryHost,registryPort);
			} catch (Exception e) {
				System.out.println("Couldn't get registry.");
			}
			
			// get billing remote object
			try {
				billingServer = (BillingServer) registry.lookup(billBind);
			} catch (AccessException e1) {
				System.out.println("Access to registry denied.");
			} catch (NotBoundException e1) {
				System.out.println("Billing Server not found.");
			} catch (RemoteException e1) {
				System.out.println("Problem finding BillingServer remote object.");
			}

			// get analytics remote object
			try {
				analyticsHandler = (AnalyticsRMIInterface) registry.lookup(analBind);
			} catch (AccessException e1) {
				System.out.println("Access to registry denied.");
			} catch (NotBoundException e1) {
				System.out.println("Analytic Server not found.");
			} catch (RemoteException e1) {
				System.out.println("Problem finding AnalyticsServer remote object.");
			}

			/*
			 * LOADTEST
			 */
			if(args.length == 3){

				line = args[2];
				String[] split = line.split(" ");
				try {
					eventListener = new EventListener();
				} catch (RemoteException e) {
					System.out.println("EventListener Remote Exception");
				}
				regex = "";
				try {
					regex = split[1].replaceAll("\'", "");
				} catch (IndexOutOfBoundsException e) {
					System.out.println("Error: Wrong argument count.");
				}

				try {
					String answer;
					if (analyticsHandler == null) {
						try {
							analyticsHandler = (AnalyticsRMIInterface)  registry.lookup(analBind);
						} catch (NotBoundException e) {
							System.out.println("Error: Problem binding analytics Server");
						}
					}
					answer = analyticsHandler.subscribe(eventListener, regex);

					System.out.println(answer);
				} catch (RemoteException e) {
					System.out.println("Analytic Server Remote Exception");
				}
			}

			while (true) {
				try {
					System.out.print(userName + "> ");

					line = stdin.readLine();
				} catch (IOException e) {
					System.out.println("Error: IO Error.");
					System.exit(-1);
				}
				String[] split = line.split(" ");

				/*
					Billing commands
				 */
				if (line.startsWith("!login ") && split.length == 3) {
					userName = split[1];
					userPwd = split[2];
					BillingServerSecure bss;
					try {
						bss = billingServer.login(userName, userPwd);
						billingServerSecure = bss;
						if(bss != null){
							System.out.println(userName + " successfully logged in");
						}
					} catch (RemoteException e) {
						System.out.println("Login failed");
					} catch (NullPointerException e) {
						System.out.println("Wrong login credentials");
					}

				} else if (line.equals("!steps") && split.length == 1) {
					String steps;
					try {
						if (billingServerSecure != null) {
							steps = billingServerSecure.getPriceSteps().toString();
							System.out.println(steps);
						} else {
							System.out.println("Error: you must be logged in for this command to work");
						}
					} catch (RemoteException ex) {
						System.out.println("There are no price steps");
					}

				} else if (line.startsWith("!addStep") && split.length == 5) {
					try {
						startPrice 			= Double.parseDouble(split[1]);
						endPrice   			= Double.parseDouble(split[2]);
						fixedPrice 			= Double.parseDouble(split[3]);
						variablePrice 		= Double.parseDouble(split[4]);
						// Add step to BillingServer
                                                if (billingServerSecure == null) {
                                                    System.out.println("ERROR: You are currently not logged in");
                                                } else {
                                                    billingServerSecure.createPriceStep(startPrice, endPrice, fixedPrice, variablePrice);

						if(endPrice == 0){
							System.out.println("Step ["+startPrice+" INFINITY]" + " successfully added");
						} else {
							System.out.println("Step ["+startPrice+" "+endPrice +"]" + " successfully added");
						}
                                                }
					} catch (RemoteException ex) {
						System.out.println("Error: " + ex.getMessage());
					} catch (NumberFormatException e) {
						System.out.println("Error: " + e.getMessage());
					} catch (IllegalArgumentException ae){
						System.out.println("Error: " + ae.getMessage());

					}

				} else if (line.startsWith("!removeStep") && split.length == 3) {
					try {
						// Call RemoveStep from Billing Server
						startPrice = Double.parseDouble(split[1]);
						endPrice = Double.parseDouble(split[2]);

                                                if (billingServerSecure == null) {
                                                        System.out.println("ERROR: You are currently not logged in");
                                                } else {
                                                    billingServerSecure.deletePriceStep(startPrice, endPrice);
                                                    System.out.println("Price step ["+startPrice+ " "+  endPrice + "]" + " successfully removed");
                                                }
					} catch (RemoteException ex) {
						System.out.println(ex);
					} catch (NumberFormatException e) {
						System.out.println("Error: Please enter valid numbers");
					} catch (IllegalArgumentException ae){
						System.out.println("Error: " + ae.getMessage());

					}


				} else if (line.startsWith("!bill") && split.length == 2) {
					userBill = split[1];
					String bill;
					try {
                                            if (billingServerSecure == null) {
                                                System.out.println("ERROR: You are currently not logged in");
                                        } else {
						bill = billingServerSecure.getBill(userBill).toString();
						System.out.println(bill);
                                        }
					} catch (RemoteException ex) {
						System.out.println(" Error: There are no bill");
					}
				} else if (line.equals("!logout") && split.length == 1) {
					if (userName.equals("")) {
						System.out.println("Error: You are not logged in");
					} else {
						logout();
					}

					/*
					Analytics commands
					 */
				} else if (line.equals("!auto") && split.length == 1) {
					EventListener.setMode(0);

				} else if (line.equals("!hide") && split.length == 1) {
					EventListener.setMode(1);

					System.out.println("Buffering notifications..");
				} else if (line.equals("!print") && split.length == 1) {
					EventListener.printBuffer();

				} else if (line.startsWith("!subscribe") && split.length == 2) {
					try {
						eventListener = new EventListener();
					} catch (RemoteException e) {
						System.out.println("EventListener Remote Exception");
					}
					regex = "";
					try {
						regex = split[1].replaceAll("\'", "");
					} catch (IndexOutOfBoundsException e) {
						System.out.println("Error: Wrong argument count.");
					}

					try {
						String answer;
						if (analyticsHandler == null) {
							try {
								analyticsHandler = (AnalyticsRMIInterface) registry.lookup(analBind);
							} catch (NotBoundException e) {
								System.out.println("Error: Problem binding analytics Server");
							}
						}
						answer = analyticsHandler.subscribe(eventListener, regex);

						System.out.println(answer);
					} catch (RemoteException e) {
						System.out.println("Analytic Server Remote Exception");
					}
				} else if (line.startsWith("!unsubscribe") && split.length == 2) {
					String answer = "";

					try {
						subscriptionId = Integer.parseInt(split[1]);
					} catch (NumberFormatException e) {
						System.out.println("Error: Please enter a valid number");
					} 

					try {
						answer = analyticsHandler.unsubscribe(subscriptionId);
					} catch (RemoteException e) {
						System.out.println("Analytic Server Remote Exception");
					}
					System.out.println(answer);
				} else {
					System.out.println("Command not recognized.");
				}
			}
		} else {
			System.out.println("Wrong argument count. MC");
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
	

	private static void logout() {
		billingServerSecure = null;
		userName = "";
	}
}