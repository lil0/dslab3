

package billingServer;

/**
 *
 * @author lisibauernhofer
 */
public class UserBill {

    public String user;
    public long auctionID;
    public double price;

   public UserBill(String user, long auctionID, double price){
        this.user = user;
        this.auctionID = auctionID;
        this.price = price;
    }

}
