package analyticsServer;

import java.rmi.Remote;
import java.rmi.RemoteException;

import event.Event;
import event.EventInterface;

public interface AnalyticsRMIInterface extends Remote {

	String subscribe(EventInterface eventListener, String regex) throws RemoteException;

	String unsubscribe(int subscriptionId)  throws RemoteException;
	
	void processEvent(Event event)  throws RemoteException;
	
	String test() throws RemoteException;
}