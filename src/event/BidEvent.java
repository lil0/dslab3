package event;

public class BidEvent extends Event {

	private static final long serialVersionUID = 328606804260642877L;
	private String userName;
	private int auctionId;
	private double price;

	public BidEvent(String type, long timestamp, String userName, int auctionId, double price) throws Exception {
		if ((type.equals("BID_PLACED") || type.equals("BID_OVERBID") || type.equals("BID_WON"))) {
			this.type = type;
			this.timestamp = timestamp;
			this.userName = userName;
			this.auctionId = auctionId;
			this.price = price;
		} else {
			throw new Exception();
		}
	}

	public String getUserName() {
		return userName;
	}

	public synchronized void setUserName(String userName) {
		this.userName = userName;
	}

	public long getAuctionId() {
		return auctionId;
	}

	public synchronized void setAuctionId(int auctionId) {
		this.auctionId = auctionId;
	}

	public double getPrice() {
		return price;
	}

	public synchronized void setPrice(double price) {
		this.price = price;
	}
}
