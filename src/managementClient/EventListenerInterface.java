package managementClient;

import java.rmi.Remote;
import java.rmi.RemoteException;
import event.Event;

public interface EventListenerInterface extends Remote {
	void processEvent(Event event) throws RemoteException;
	int getId() throws RemoteException;
	void setId(int id) throws RemoteException;
}