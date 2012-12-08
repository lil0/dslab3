package analyticsServer;

import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import managementClient.EventListener;

import event.AuctionEvent;
import event.BidEvent;
import event.Event;
import event.EventInterface;
import event.StatisticsEvent;
import event.UserEvent;

public class AnalyticsRMIHandler implements AnalyticsRMIInterface {

	List<Subscription> subscriptionList = Collections.synchronizedList(new ArrayList<Subscription>());
	List<UserEvent> userList = Collections.synchronizedList(new ArrayList<UserEvent>());
	List<AuctionEvent> auctionList = Collections.synchronizedList(new ArrayList<AuctionEvent>());

	// Auction statistics variables
	StatisticsEvent avgAuctionDuration = null;
	StatisticsEvent auctionSucessRatio = null;

	// Bid statistics variables
	StatisticsEvent maxBid = null;
	StatisticsEvent bidsPerMinute = null;

	// User statistics variables
	StatisticsEvent minSessionTime = null;
	StatisticsEvent avgSessionTime = null;
	StatisticsEvent maxSessionTime = null;

	int auctionCounter = 0;
	int auctionsSucceded = 0;
	int bidCounter = 0;
	int auctionDurationMultiplicator = 0;
	int sessiontimeAvgMultiplicator = 0;
	String pattern = "";

	Timestamp systemStartT = new Timestamp(System.currentTimeMillis());
	long systemStart = systemStartT.getTime();
	
	// Mgmt clients call this to subscribe to events
	public String subscribe(EventInterface eventListener, String regex) throws RemoteException {
		pattern = regex;
		Subscription subscription = new Subscription(pattern, eventListener);
		subscriptionList.add(subscription);
		
		return "Created subscription with ID " + subscription.getId() + " for events using filter \'" + regex + "\'";
	}

	// Mgmt clients call this to unsubscribe from events
	public String unsubscribe(int subscriptionId)  throws RemoteException {
		for (int i = 0; i < subscriptionList.size(); i++) {
			if (subscriptionList.get(i).getId() == subscriptionId) {
				//System.out.println("The subscriptions match!");
				subscriptionList.remove(i);
				return "subscription " + subscriptionId + " terminated";
			}
		}
		return "Error: The Subscription ID couldn't be found!";
	}

	public String test() {
		return "Function call works!";
	}
	
	public void processEvent(Event event)  throws RemoteException {
		List<EventInterface> notificationListEvent = new ArrayList<EventInterface>();
		List<EventInterface> offlineList = new ArrayList<EventInterface>();
		
		Iterator<Subscription> iterator = subscriptionList.iterator();

		while (iterator.hasNext()) {
			Subscription subscription = iterator.next();
			pattern = subscription.getRegex();

			EventInterface eventListener = subscription.getEventListener();

			if (event.getType().matches(subscription.getRegex().toString())) {

				if (!notificationListEvent.isEmpty()) {
					if (!notificationListEvent.contains(eventListener)) {
						notificationListEvent.add(eventListener);
					} 
				} else {
					notificationListEvent.add(eventListener);
				}
			}
		}

		Iterator<EventInterface> it = notificationListEvent.iterator();

		while (it.hasNext()) {
			EventInterface eventListener = null;
			try {
				eventListener = it.next();
				eventListener.processEvent(event);

			} catch (RemoteException e) {
				offlineList.add(eventListener);
			}
			if (event instanceof AuctionEvent) {
				processAuctionEvent((AuctionEvent)event);
			} else if (event instanceof BidEvent) {
				processBidEvent((BidEvent)event);
			} else if (event instanceof UserEvent) {
				processUserEvent((UserEvent)event);
			}
		}

		// delete EventListeners for offline management clients
		if (!offlineList.isEmpty()) {
			for (EventInterface deleter: offlineList) {
				System.out.println("removing subscription because management client went offline");
				subscriptionList.remove(deleter);
			}
		}
		offlineList.clear();
		notificationListEvent.clear();
	}

	private void processAuctionEvent(AuctionEvent event) {

		if (event.getType().equals("AUCTION_STARTED")) {
			auctionList.add(event);
		}

		if (event.getType().equals("AUCTION_ENDED")) {
			if (avgAuctionDuration == null) {
				Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
				long timestamp = currentTimestamp.getTime();
				try {
					avgAuctionDuration = new StatisticsEvent("AUCTION_TIME_AVG", timestamp, 0);
				} catch (Exception e) {
					System.out.println("Error: Creating event failed");
				}
			}
			double avgDuration = (avgAuctionDuration.getValue() * auctionDurationMultiplicator + event.getDuration()) / (++auctionDurationMultiplicator);
			avgAuctionDuration.setValue(avgDuration);

			notifyManagementClients(avgAuctionDuration);

			auctionCounter += 1;
			
			if (!event.getAuctionWinner().equals("none")) {
				auctionsSucceded+=1;
			}
			
			if (auctionSucessRatio == null) {
				Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
				long timestamp = currentTimestamp.getTime();
				try {
					auctionSucessRatio = new StatisticsEvent("AUCTION_SUCCESS_RATIO", timestamp, 0);
				} catch (Exception e) {
					System.out.println("Error: Creating event failed");
				}
			}
			auctionSucessRatio.setValue(100/auctionCounter*auctionsSucceded);

			notifyManagementClients(auctionSucessRatio);
		}
		//notifyManagementClients(event);
	}

	private void processBidEvent(BidEvent event) {
		if (event.getType().equals("BID_PLACED") || event.getType().equals("BID_OVERBID")) {
			if (maxBid == null) {
				Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
				long timestamp = currentTimestamp.getTime();
				try {
					maxBid = new StatisticsEvent("BID_PRICE_MAX", timestamp, event.getPrice());
				} catch (Exception e) {
					System.out.println("Error: Creating event failed");
				}
			}
			if (event.getPrice() > maxBid.getValue()) {
				maxBid.setValue(event.getPrice());
			}
			notifyManagementClients(maxBid);

			Timestamp nowTimeStamp = new Timestamp(System.currentTimeMillis());
			long now = nowTimeStamp.getTime();

			long differenceMinutes = (now - systemStart)/1000/60;
			if ((differenceMinutes) < 1) {
				differenceMinutes = 1;
			}

			double bpm = (double)++bidCounter / differenceMinutes;

			if (bidsPerMinute == null) {
				try {
					bidsPerMinute = new StatisticsEvent("BID_COUNT_PER_MINUTE", now, bpm);
				} catch (Exception e) {
					System.out.println("Error: Creating event failed");
				}
			}
			else {
				bidsPerMinute.setValue(bpm);
			}
			notifyManagementClients(bidsPerMinute);
		}
		//notifyManagementClients(event);
	}

	private void processUserEvent(UserEvent event) {
		if (event.getType().equals("USER_LOGIN")) {
			userList.add(event);
		}

		// get login event, calculate USER - Statistics
		if (event.getType().equals("USER_LOGOUT") || event.getType().equals("USER_DISCONNECTED")) {
			UserEvent loginEvent;
			long difference = 0;

			Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());
			long timestamp = currentTimestamp.getTime();

			for (int i = 0; i < userList.size(); i++) {
				if (userList.get(i).getUserString().equals(event.getUserString())) {
					loginEvent = userList.get(i);
					difference = event.getTimestamp() - loginEvent.getTimestamp(); // new session time
				}
			}
			if (minSessionTime == null) {
				try {
					minSessionTime = new StatisticsEvent("USER_SESSIONTIME_MIN", timestamp, difference);
					notifyManagementClients(minSessionTime);
				} catch (Exception e) {
					System.out.println("Error: Creating event failed");
				}
			} else {
				// create new minimum session time if the current session time is shorter
				if (minSessionTime.getValue() > difference) {
					try {
						minSessionTime = new StatisticsEvent("USER_SESSIONTIME_MIN", timestamp, difference);
						notifyManagementClients(minSessionTime);
					} catch (Exception e) {
						System.out.println("Error: Creating event failed");
					}
				}
			}
			
			// Initialize maxSessionTime event
			if (maxSessionTime == null) {
				try {
					maxSessionTime = new StatisticsEvent("USER_SESSIONTIME_MAX", timestamp, difference);
					notifyManagementClients(maxSessionTime);
				} catch (Exception e) {
					System.out.println("Error: Creating event failed");
				}
			} else {
				// current session time is longer than last one, override maxSessionTime
				if (maxSessionTime.getValue() < difference) {	
					try {
						maxSessionTime= new StatisticsEvent("USER_SESSIONTIME_MAX", timestamp, difference);
						notifyManagementClients(maxSessionTime);
					} catch (Exception e) {
						System.out.println("Error: Creating event failed");
					}
				}
			}

			// set avg
			double oldAvgValue;
			if (avgSessionTime == null) {
				oldAvgValue = 0;
			} else {
				oldAvgValue = avgSessionTime.getValue();
			}

			double newValue = (oldAvgValue * sessiontimeAvgMultiplicator + difference) / (1+sessiontimeAvgMultiplicator);
			sessiontimeAvgMultiplicator++;
			try {
				avgSessionTime = new StatisticsEvent("USER_SESSIONTIME_AVG", timestamp, newValue);
				notifyManagementClients(avgSessionTime);
			} catch (Exception e) {
				System.out.println("Error: Creating event failed");
			}
		}
	}

	private void notifyManagementClients(Event event) {
		List<EventInterface> deleteList = new ArrayList<EventInterface>();
		List<EventInterface> notificationListForThisEvent = new ArrayList<EventInterface>(); 
		Iterator<Subscription> iterator = subscriptionList.iterator();

		while (iterator.hasNext()) {

			Subscription subscription = iterator.next();
			pattern = subscription.getRegex();
			EventInterface eventListener = subscription.getEventListener();

			if (event.getType().matches(pattern)) {
				if (!notificationListForThisEvent.isEmpty()) {
					if (!notificationListForThisEvent.contains(eventListener)) {
						notificationListForThisEvent.add(eventListener);
					} 
				} else {
					notificationListForThisEvent.add(subscription.getEventListener());
				}
			}
		}

		Iterator<EventInterface> it = notificationListForThisEvent.iterator();
		while (it.hasNext()) {
			EventInterface eventListener = null;
			try {
				it.next().processEvent(event);
			} catch (RemoteException e) {
				deleteList.add(eventListener);
			}
		}

		if (!deleteList.isEmpty()) {
			for (EventInterface deleter: deleteList) {
				System.out.println("removing subscription for offline user");
				subscriptionList.remove(deleter);
			}
		}
		deleteList.clear();
		notificationListForThisEvent.clear();
	}
}