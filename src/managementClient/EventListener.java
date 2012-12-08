package managementClient;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import event.*;

public class EventListener extends UnicastRemoteObject implements EventInterface {
	/* mode: 
		0..!auto (display notifications)
		1..!hide (notificationsBuffer new notifications)
	*/
	
	// Displays incoming events in user-friendly form
	
	private static final long serialVersionUID = -1918210732395070306L;
	private static int mode = 0;
	private static List<String> notificationsBuffer = new ArrayList<String>();
	private int id = 0;
	public EventListener() throws RemoteException {
		super();
	}
	
	public void processEvent(Event event) throws RemoteException {
		
		//System.out.println("Incoming event received: " + event.getType() + " " + event.getClass());
		
		Timestamp myTimestamp = new Timestamp(event.getTimestamp());
		String timestamp = new SimpleDateFormat("dd.MM.yyyy - hh:mm:ss z").format(myTimestamp);

		// AuctionEvent Handling
		if (event instanceof AuctionEvent) {
			AuctionEvent auctionEvent = (AuctionEvent) event;

			if (auctionEvent.getType().equals("AUCTION_STARTED")) {
				String message = auctionEvent.getType() + ": " + timestamp + " auction " + auctionEvent.getAuctionId() + " started";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}

			} else if (auctionEvent.getType().equals("AUCTION_ENDED")) {
				String message = auctionEvent.getType() + ": " + timestamp + " auction " + auctionEvent.getAuctionId() + " ended";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			}
		}

		// BidEvent Handling
		if (event instanceof BidEvent) {
			BidEvent bidEvent = (BidEvent) event;

			if (bidEvent.getType().equals("BID_PLACED")) {
				String message = bidEvent.getType() + ": " + timestamp + " " + "user " + bidEvent.getUserName() + " placed bid " + bidEvent.getPrice() + " on auction " + bidEvent.getAuctionId();
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}

			} else if (bidEvent.getType().equals("BID_OVERBID")) {
				String message = bidEvent.getType() + ": " + timestamp + " " + "user " + bidEvent.getUserName() + " overbid the auction " + bidEvent.getAuctionId() + " with " + bidEvent.getPrice();
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}

			} else if (bidEvent.getType().equals("BID_WON")) {
				String message = bidEvent.getType() + ": " + timestamp + " " + "user " + bidEvent.getUserName() + " won the auction " + bidEvent.getAuctionId() + " with " + bidEvent.getPrice();
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			}
		}
	
		// StatisticsEvent Handling
		if (event instanceof StatisticsEvent) {
			StatisticsEvent statisticsEvent = (StatisticsEvent) event;

			if (statisticsEvent.getType().equals("AUCTION_SUCCESS_RATIO")) {
				String message = statisticsEvent.getType() + ": " + timestamp + " auction success ratio is " + statisticsEvent.getValue() + "%";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			} else if (statisticsEvent.getType().equals("AUCTION_TIME_AVG")) {
				String message = statisticsEvent.getType() + ": " + timestamp + " average auction time is " + (Math.round(statisticsEvent.getValue()*100)/100) + " seconds";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			} else if (statisticsEvent.getType().equals("BID_COUNT_PER_MINUTE")) {
				String message = statisticsEvent.getType() + ": " + timestamp + " current bids per minute is " + Math.round(statisticsEvent.getValue()*100)/100;
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			} else if (statisticsEvent.getType().equals("BID_PRICE_MAX")) {
				String message = statisticsEvent.getType() + ": " + timestamp + " maximum bid price seen so far is " + statisticsEvent.getValue();
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			} else if (statisticsEvent.getType().equals("USER_SESSIONTIME_AVG")) {
				String message = statisticsEvent.getType() + ": " + timestamp + " average session time is " + (Math.round(statisticsEvent.getValue()/10)/100) + " seconds";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			} else if (statisticsEvent.getType().equals("USER_SESSIONTIME_MAX")) {
				String message = statisticsEvent.getType() + ": " + timestamp + " maximum session time is " + (Math.round(statisticsEvent.getValue()/10)/100) + " seconds";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			} else if (statisticsEvent.getType().equals("USER_SESSIONTIME_MIN")) {
				String message = statisticsEvent.getType() + ": " + timestamp + " minimum session time is " + (Math.round(statisticsEvent.getValue()/10)/100) + " seconds";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			}
		}
		
		// UserEvent Handling
		if (event instanceof UserEvent) {
			UserEvent userEvent = (UserEvent) event;

			if (userEvent.getType().equals("USER_LOGIN")) {
				String message = userEvent.getType() + ": " + timestamp + " " + "user " + userEvent.getUserString() + " logged in";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			} else if (userEvent.getType().equals("USER_LOGOUT")) {
				String message = userEvent.getType() + ": " + timestamp + " " + "user " + userEvent.getUserString() + " logged out";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			} else if (userEvent.getType().equals("USER_DISCONNECTED")) {
				String message = userEvent.getType() + ": " + timestamp + " " + "user " + userEvent.getUserString() + " diskonnected";
				if (mode == 0) {
					System.out.println(message);
				} else {
					notificationsBuffer.add(message);
				}
			}
		}

	}

	protected int getState() {
		return mode;
	}

	protected static synchronized void setMode(int mode) {
		EventListener.mode = mode;
	}

	public int getId() throws RemoteException {
		return this.id;
	}

	public void setId(int id) throws RemoteException {
		this.id = id;
	}
	
	protected static void printBuffer() {
		for (int i = 0; i < notificationsBuffer.size(); i++) {
			System.out.println(notificationsBuffer.get(i));
		}
		notificationsBuffer.clear();
	}
}