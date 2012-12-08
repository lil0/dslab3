package loadTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lisibauernhofer
 */
public class TestClient extends Thread{
    private String host;
    private int port;
    private int clientNr;
    private int clientAll;
    private int auctionPerMin;
    private int auctionDuration;
    private int updateIntervalSec;
    private int bidsPerMin;

    private Socket socket = null;
    private PrintWriter out = null;
    private  BufferedReader in = null;

    private int auctionNr = 1;
    private boolean createAuction = false;
    private long startTime;
    private int ID = 1;
    private static TestRead testRead;


    TestClient(String host, int port,long time, int client,int clientAll,  int auctionPerMin, int auctionDuration, int updateIntervalSec,
            int bidsPerMin) {

        this.host = host;
        this.port = port;
        this.clientNr = client;
        this.clientAll = clientAll;
        this.auctionPerMin = auctionPerMin;
        this.auctionDuration = auctionDuration;
        this.updateIntervalSec = updateIntervalSec;
        this.bidsPerMin = bidsPerMin;

        this.startTime = time;


    }

    public void run(){
        try {
            socket = new Socket(host, port); //Socket wird erstellt fuer die Verbindung mir dem Server
            out = new PrintWriter(socket.getOutputStream(), true); //damit werden Daten an den Server uebergeben
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));//Daten vom Server koennen damit gelesen werden

            testRead = new TestRead(in);
            testRead.start();
        } catch (UnknownHostException ex) {
            System.out.println("Error: UnknownHostException");
        } catch (IOException ex) {
            System.out.println("Error: IOException");
        }

        
        
        login();

        

      // scheduling the task creat at fixed rate
      //  1min = 60 s = 60000 ms
      Timer timerCreate = new Timer();
      timerCreate.scheduleAtFixedRate(
              new TimerTask() {
                    public void run(){
                        create();
                    }
              }
      ,new Date(),60000/auctionPerMin);

      // scheduling the task List at fixed rate
      //  1min = 60 s = 60000 ms
      Timer timerList = new Timer();
      timerList.scheduleAtFixedRate(
              new TimerTask() {
                    public void run(){
                        list();
                    }
              }
      ,new Date(),updateIntervalSec * 1000);


      // scheduling the task bid at fixed rate
      //  1min = 60 s = 60000 ms
      if(createAuction == true){

           Timer timerBid = new Timer();
           timerBid.scheduleAtFixedRate(
              new TimerTask() {
                    public void run(){
                        bid();
                    }
              }
            ,new Date(),60000/bidsPerMin);
      }
   }

    

    private void login() {
            String login = "!login client"+clientNr;
            out.println(login);
        
        

    }

    private void create() {
        synchronized(out){
            String create = "!create "+auctionDuration+" book"+Math.round(Math.random()*100);
            out.println(create);
            auctionNr++;
            createAuction = true;
        }
   }

    private void bid() {
        int auctions = auctionNr * clientAll;
        long time = new Date().getTime() - startTime;
        double price = time/10.0f;

        price = price * 100;
        price = Math.round(price);
        price = price/100;
        Random rand = new Random();
        int id = rand.nextInt(auctions);
        String bid = "!bid " +id+" "+price;
        ID++; 
        out.println(bid);
    }

    private void list() {
        String list = "!list";
        out.println(list);      
    }

}