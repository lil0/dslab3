
package channels;


public interface Channel {
    
    public void send(String line); 
    
    public void receive();
    
    public void close();
}
