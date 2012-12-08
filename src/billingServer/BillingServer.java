package billingServer;

import java.rmi.*;

/**
 *
 * @author lisibauernhofer
 */
public interface BillingServer extends Remote{

     BillingServerSecure login(String username, String password) throws RemoteException;

}

