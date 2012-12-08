package event;

import java.io.Serializable;

public abstract class Event implements Serializable {
	
	private static final long serialVersionUID = -8613329932020034114L;
	protected int id;
	protected String type;
	protected long timestamp;

	public int getId() {
		return id;
	}
	public synchronized void setId(int id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public synchronized void setType(String type) {
		this.type = type;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public synchronized void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
