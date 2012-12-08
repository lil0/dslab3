
package channels;

import biddingClient.BiddingClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bouncycastle.util.encoders.Base64;



public class Base64Channel implements Channel{
    private Channel channel; 
    private Socket socket; 
    private BufferedReader in;
    
    public Base64Channel(Channel channel){
        this.channel = channel; 
    }
    
    public Base64Channel(Socket socket){
        try {
            this.socket = socket; 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException ex) {
            Logger.getLogger(Base64Channel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void send(String line) {
        try {
            //throw new UnsupportedOperationException("Not supported yet.");
           byte[] b = stringToByte(line);
            String msg; 
           
            byte[] bs = Base64.encode(b);
            msg = new String(b, "UTF-8");
            channel.send(msg);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Base64Channel.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }

    public void receive() {
 
        try { 
                String inputLine = "";
                while ((inputLine = in.readLine()) != null) {
                    byte [] decode = Base64.decode(inputLine);
                    String msg = new String(decode);
                    
                   BiddingClient.usage(msg);  
                }
                               
            } catch (IOException ex) {
               System.out.println("Problem reading from Server with TCP.");
            }
        
    }

    public void close() {
        channel.close();
    }
    
    public static byte[] stringToByte(String text) {
          final String[] split = text.split("\\s+");
          final byte[] result = new byte[split.length];
          int i = 0;
          for (String b : split) result[i++] = (byte)Integer.parseInt(b, 16);
          return result;
    }
    
}
