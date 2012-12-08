

package billingServer;

import billingServer.PriceSteps.PriceStep;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lisibauernhofer
 */
public class BillingServerSecureImpl implements BillingServerSecure{
  
     private static PriceSteps priceSteps = new PriceSteps();
     private static ArrayList<UserBill> userBill = new ArrayList<UserBill>();


    @Override
    public PriceSteps getPriceSteps() throws RemoteException {
        
        return BillingServerSecureImpl.priceSteps;
    }

    @Override
    public void createPriceStep(double startPrice, double endPrice, 
            double fixedPrice, double variablePricePercent)throws RemoteException{

        priceSteps.addPriceStep(startPrice, endPrice, fixedPrice, variablePricePercent);


    }
    @Override
    public void deletePriceStep(double startPrice, double endPrice) throws RemoteException{
        priceSteps.deleteStep(startPrice, endPrice);
    }

    public void billAuction(String user, long auctionID, double price){

         synchronized(userBill){
            UserBill uB = new UserBill(user, auctionID, price);
            userBill.add(uB);
        }
    }

     

    @Override
    public Bill getBill(String user) throws RemoteException{

        return new Bill(user, userBill, priceSteps);  
    }
}

