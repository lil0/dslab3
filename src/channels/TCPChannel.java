
package channels;

import biddingClient.BiddingClient;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TCPChannel implements Channel{
	private Socket clientSocket;
	private BufferedReader in;
	private PrintWriter out;

	public TCPChannel(Socket socket){
		try {
			this.clientSocket = socket; 
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException ex) {
			Logger.getLogger(TCPChannel.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void send(String line) {
		out.println(line);
	}

	public void receive() {
		try {
			String inputLine = "";
			while ((inputLine = in.readLine()) != null) {
				BiddingClient.usage(inputLine);
			}
		} catch (IOException ex) {
			System.out.println("Problem reading from Server with TCP.");
		}

	}

	public void close() {
		try {
			clientSocket.close();
			in.close();
			out.close();
		} catch (IOException ex) {
			Logger.getLogger(TCPChannel.class.getName()).log(Level.SEVERE, null, ex);
		}
	}


}
