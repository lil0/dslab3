package event;

import java.util.ArrayList;
import java.util.List;

public class StatisticsEvent extends Event {

	private static final long serialVersionUID = -487372108197660940L;
	private double value;
	List<String> typeList = new ArrayList<String>();

	public double getValue() {
		return value;
	}
	public synchronized void setValue(double value) {
		this.value = value;
	}
	
	public StatisticsEvent(String type, long timestamp, double value) throws Exception {
		typeList.add("USER_SESSIONTIME_MIN");
		typeList.add("USER_SESSIONTIME_MAX");
		typeList.add("USER_SESSIONTIME_AVG");
		typeList.add("BID_PRICE_MAX");
		typeList.add("BID_COUNT_PER_MINUTE");
		typeList.add("AUCTION_TIME_AVG");
		typeList.add("AUCTION_SUCCESS_RATIO");

		// If Type is unknown
		if (!typeList.contains(type)) {
			throw new Exception();
		}
		this.type = type;
		this.timestamp = timestamp;
		this.value = value;
	}
}
