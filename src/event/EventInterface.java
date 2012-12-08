package event;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface EventInterface extends Remote {
	void processEvent(Event e) throws RemoteException;
	int getId() throws RemoteException;
	void setId(int id) throws RemoteException;
}