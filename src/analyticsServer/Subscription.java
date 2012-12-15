package analyticsServer;

//import java.util.regex.Pattern;

import event.EventInterface;

public class Subscription {
	private static int idCount = 0;
	private final int id = Subscription.createId();
	private String regex;
	private EventInterface eventListener;

	public Subscription(String regex, EventInterface eventListener) {
		this.regex = regex;
		this.eventListener = eventListener;
	}

	public String getRegex() {
		return regex;
	}

	public synchronized void setRegex(String regex) {
		this.regex = regex;
	}

	public EventInterface getEventListener() {
		return eventListener;
	}

	public synchronized void setEventListener(EventInterface eventListener) {
		this.eventListener = eventListener;
	}

	public int getId() {
		return id;
	}

	private static synchronized int createId() {
		return idCount+=1;
	}
}
