package auctionServer;

// TODO: what happens if a client does not have a secret key? Should this be handled?

import analyticsServer.AnalyticsRMIInterface;
import billingServer.BillingServer;
import billingServer.BillingServerSecure;
//import java.net.InetAddress;
//import java.rmi.AccessException;
//import java.rmi.NotBoundException;
import java.rmi.RemoteException;
//import java.rmi.registry.LocateRegistry;
//import java.rmi.registry.Registry;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Mac;

import sun.misc.BASE64Encoder;

import event.AuctionEvent;
import event.BidEvent;

public class AuctionProtocol {
	protected static String userName;
	static boolean exists;
	protected AnalyticsRMIInterface analyticsHandler;

	public String processInput(String command) {
		String[] split = command.split(" ");
		String completeString = "";
		if (command.startsWith("!login ")) {
			userName = command.split(" ")[1];
		} else if (command.equals("!list")) {
			synchronized(AuctionServer.auctionDescription) {
				Iterator<?> iter = AuctionServer.auctionDescription.entrySet().iterator();

				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();
					completeString = completeString + entry.getKey() + ". '" + entry.getValue() + "' " + AuctionServer.auctionOwner.get(entry.getKey()) + " " + AuctionServer.auctionEndtime.get(entry.getKey()) + " " + AuctionServer.auctionHighestBid.get(entry.getKey()).toString() + " " + AuctionServer.auctionHighestBidder.get(entry.getKey()) + "\n";
				}
			}
			AuctionServer.userLastMessage.put(userName, completeString);
			
			//DEBUG
			//System.out.println("The full message is ::" + completeString + "::");
			//System.out.println("The full message plus appended is ::" + appendHMAC(completeString) + "::");
			//DEBUG
			
			return appendHMAC(completeString);	
		} else if (command.startsWith("!create ")) {
			synchronized(AuctionServer.auctionDescription) {
				synchronized(AuctionServer.auctionEndtime) {
					synchronized(AuctionServer.auctionHighestBid) {
						synchronized(AuctionServer.auctionHighestBidder) {
							synchronized(AuctionServer.auctionOwner) {

								int duration = Integer.parseInt(split[1]);
								String fullDescription = "";

								for (int i = 2; i < split.length; i++) {
									fullDescription += split[i] + " ";
								}
								fullDescription = fullDescription.substring(0, fullDescription.length() - 1);
								int newId = AuctionServer.auctionCounter + 1;

								Timestamp original = new Timestamp(System.currentTimeMillis());
								Calendar cal = Calendar.getInstance();
								cal.setTimeInMillis(original.getTime());
								cal.add(Calendar.SECOND, duration);
								Timestamp timestamp = new Timestamp(cal.getTime().getTime());
								Timer timer = new Timer("AuctionEnd");
								MyTask task = new MyTask(newId);
								timer.schedule(task, (Long.parseLong(split[1]) * 1000));

								AuctionServer.auctionDescription.put(newId, fullDescription);
								AuctionServer.auctionEndtime.put(newId, timestamp);
								AuctionServer.auctionHighestBid.put(newId, 0.0);
								AuctionServer.auctionHighestBidder.put(newId, "");
								AuctionServer.auctionOwner.put(newId, userName);
								AuctionServer.auctionCounter += 1;
								AuctionServer.auctionDuration.put(newId, duration);

								// Call ProcessEvent from AnalyticsHandler for AUCTION_STARTED
								Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
								long ts = logoutTimestamp.getTime();
								try {
									ServerThread.analyticsHandler.processEvent(new AuctionEvent("AUCTION_STARTED", ts, newId));

								} catch (RemoteException e) {
									System.out.println("Couldn't connect to Analytics Server");
								} catch (Exception e) {
									System.out.println("Error processing event " + e.getClass());
								}
								String returnString = "An auction '" + fullDescription + "' with id " + newId + " has been created and will end on " + timestamp + ".";
								AuctionServer.userLastMessage.put(userName, returnString);
								
								return appendHMAC(returnString);
							}
						}
					}
				}
			}
		} else if (command.startsWith("!bid ")) {
			synchronized(AuctionServer.auctionDescription) {
				synchronized(AuctionServer.auctionEndtime) {
					synchronized(AuctionServer.auctionHighestBid) {
						synchronized(AuctionServer.auctionHighestBidder) {
							synchronized(AuctionServer.auctionOwner) {

								int auctionId = Integer.parseInt(split[1]);
								exists = false;
								String returnString = "";

								// Add check for existing auction ID
								synchronized(this) {
									Iterator<?> iter = AuctionServer.auctionDescription.entrySet().iterator();

									while (iter.hasNext()) {
										Map.Entry entry = (Map.Entry) iter.next();
										if (entry.getKey().equals(auctionId)) {
											exists = true;
										}
									}
								}

								if (exists == true) {
									double oldValue = AuctionServer.auctionHighestBid.get(auctionId);
									double bidValue = Double.parseDouble(split[2]);
									//String oldUser = AuctionServer.auctionHighestBidder.get(auctionId);

									if (bidValue > AuctionServer.auctionHighestBid.get(auctionId)) {
										// New bid must be higher than last winning bid
										if (userName != AuctionServer.auctionHighestBidder.get(auctionId)) {

											AuctionServer.auctionHighestBid.put(auctionId, bidValue);
											AuctionServer.auctionHighestBidder.put(auctionId, userName);

											// Call BID_PLACED
											Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
											long ts = logoutTimestamp.getTime();
											try {
												ServerThread.analyticsHandler.processEvent(new BidEvent("BID_PLACED", ts, userName, auctionId, bidValue));

											} catch (RemoteException e) {
												System.out.println("Couldn't connect to Analytics Server");
											} catch (Exception e) {
												System.out.println("Error processing event " + e.getMessage());
											}

											if (oldValue != 0.0) {
												// Call BID_OVERBID
												logoutTimestamp = new Timestamp(System.currentTimeMillis());
												ts = logoutTimestamp.getTime();
												try {
													ServerThread.analyticsHandler.processEvent(new BidEvent("BID_OVERBID", ts, userName, auctionId, bidValue));
												} catch (RemoteException e) {
													System.out.println("Couldn't connect to Analytics Server");
												} catch (Exception e) {
													System.out.println("Error processing event " + e.getMessage());
												}
											}
											returnString = "You successfully bid with " + bidValue + " on '" + AuctionServer.auctionDescription.get(auctionId) + "'.";

										} else {
											returnString = "Error: You can't overbid yourself.";
										}
									} else {
										returnString = "Error: Bid must be higher than current highest bid.";
									}
								} else {	
									returnString = "Error: Auction doesn't exist.";
								}
								AuctionServer.userLastMessage.put(userName, returnString);
								
								return appendHMAC(returnString);
							}
						}
					}
				}
			}
		}
		return "";
	}

	// Static function to add an HMAC to a given String
	protected static String appendHMAC (String message) {
		Key secretKey = AuctionServer.userKeys.get(userName);
		byte[] hash = null;
		BASE64Encoder encoder = new BASE64Encoder();
		
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
		
		return message + "-999-" + encoder.encode(hash);
	}

}
class MyTask extends TimerTask {
	private int id;
	static BillingServer billingServer = null;
	static BillingServerSecure billingServerSecure = null;
	public MyTask(int newId) {
		id = newId;
		//String internString;
	}

	public void run() {
		String highestBidder = AuctionServer.auctionHighestBidder.get(id);
		//String auctionOwner = AuctionServer.auctionOwner.get(id);
		Double highestBid = AuctionServer.auctionHighestBid.get(id);
		int duration = AuctionServer.auctionDuration.get(id);

		int auctionId = id;
		try {
			ServerThread.billingServerSecureHandler.billAuction(highestBidder, auctionId, highestBid);
		} catch (RemoteException ex) {
			Logger.getLogger(MyTask.class.getName()).log(Level.SEVERE, null, ex);
		}

		// Create BID_WON event
		if (highestBid != 0.0) {		// Someone made an offer aka there is a winner
			Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
			long ts = logoutTimestamp.getTime();
			try {
				ServerThread.analyticsHandler.processEvent(new BidEvent("BID_WON", ts, highestBidder, auctionId, highestBid));
			} catch (RemoteException e) {
				System.out.println("Couldn't connect to Analytics Server");
			} catch (Exception e) {
				System.out.println("Error processing event");
			}

		}

		// Create AUCTION_ENDED event
		Timestamp logoutTimestamp = new Timestamp(System.currentTimeMillis());
		long ts = logoutTimestamp.getTime();
		try {
			ServerThread.analyticsHandler.processEvent(new AuctionEvent("AUCTION_ENDED", ts, auctionId, duration, highestBidder));
		} catch (RemoteException e) {
			System.out.println("Couldn't connect to Analytics Server");
		} catch (Exception e) {
			System.out.println("Error processing event" + e.getClass());
		}

		//remove auction from system
		AuctionServer.auctionDescription.remove(id);
		AuctionServer.auctionEndtime.remove(id);
		AuctionServer.auctionHighestBid.remove(id);
		AuctionServer.auctionHighestBidder.remove(id);
		AuctionServer.auctionOwner.remove(id);
		AuctionServer.auctionDuration.remove(id);
		this.cancel();
	}
}