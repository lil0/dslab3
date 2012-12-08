package biddingClient;

import channels.TCPChannel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientTcpThread extends Thread{
	private Socket socket = null;
        private TCPChannel tcpChannel = null; 

	public ClientTcpThread(Socket socket) {	
		super("ClientThread");
		this.socket = socket;
                tcpChannel = new TCPChannel(this.socket);
	}
	public void run() {
            
            tcpChannel.receive();
            
		/*String inputLine = "";
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while ((inputLine = in.readLine()) != null) {
				BiddingClient.usage(inputLine);
			}
		} catch (IOException e) {
			System.out.println("Problem reading from Server with TCP.");
		}*/
			
	}
}
