
package loadTesting;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import managementClient.ManagementClient;

/**
 *
 * @author lisibauernhofer
 */
public class LoadTest {

    private static String host = "";
    private static int port = 0;
    private static String analBindingName = "";
    private static String billBindingName = "";
    private static int clientNr = 0;
    private static int auctionPerMin = 0;
    private static int auctionDuration = 0;
    private static int updateIntervalSec = 0;
    private static int bidsPerMin = 0;
    private static ArrayList<TestClient> testClient = null;
    private static long startTime;
    private static TestMC testMC;
    
    public static void main(String[] args) {    
        //System.out.println("im LoadTest");

        if (args.length == 4) {
            host = args[0];
            port = Integer.parseInt(args[1]);
            analBindingName = args[2];
            billBindingName = args[3];

            readProperties();

             testClient = new ArrayList<TestClient>();
             startTime = new Date().getTime();

             String[] argsh;
             argsh= new String[] {analBindingName, billBindingName, "!subscribe .*"};
             
             testMC = new TestMC(argsh);
             testMC.start();

            for(int client = 0; client<clientNr; client++){

                TestClient testClientStub = new TestClient(host, port, startTime, client, clientNr, auctionPerMin,
                        auctionDuration, updateIntervalSec, bidsPerMin);

                testClient.add(testClientStub);
                testClientStub.start();
            }


        }else {
            System.out.println("wrong argument count - LoadTest");
            System.exit(-1);
	}
        
    }

    private static void readProperties(){
        InputStream in = ClassLoader.getSystemResourceAsStream("loadtest.properties");
        if(in!=null){

            Properties props = new Properties();
            try{
                try {
                    props.load(in);
                } catch (IOException ex) {
                    System.out.println("Error:Loding Properties-File");
                }

                clientNr = Integer.parseInt(props.getProperty("clients"));
                auctionPerMin = Integer.parseInt(props.getProperty("auctionsPerMin"));
                auctionDuration = Integer.parseInt(props.getProperty("auctionDuration"));
                updateIntervalSec = Integer.parseInt(props.getProperty("updateIntervalSec"));
                bidsPerMin = Integer.parseInt(props.getProperty("bidsPerMin"));

           }finally{
                try {
                    in.close();
                } catch (IOException ex) {
                    System.out.println("Error: Closing InputStream");                 }
           }
        }else{
            System.out.println("Error: Properties-File not found!");            
        }
     }
}
