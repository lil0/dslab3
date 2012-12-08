
package billingServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lisibauernhofer
 */
public class BillingServerImpl implements BillingServer{

    public BillingServerImpl() throws RemoteException{}

    private static String bindingName = "";
    private static String registryHost = "";
    private static int registryPort = 0;

    public static void main(String[] args) {
        if(args.length==1){

            //args 0 = binging Name

            bindingName = args[0];
            readProperties();
            BillingServer billingserver;
            try {
                billingserver = new BillingServerImpl();
                BillingServer billingStub = (BillingServer) UnicastRemoteObject.exportObject(billingserver, 0);
                Registry registry = LocateRegistry.getRegistry(registryPort);
                try{
                    registry.list();
                }catch(ConnectException e){
                    LocateRegistry.createRegistry(registryPort);

                }
                registry.rebind(bindingName, billingStub);

                try {
                    System.in.read();
                } catch (IOException ex) {
                }

                System.out.println("down...");
                try {
                    registry.unbind(bindingName);
                    UnicastRemoteObject.unexportObject(billingserver, true);

                } catch (NotBoundException ex) {
                    System.out.println("Error: Not bound");

                } catch (AccessException ex) {
                    System.out.println("Error: AccessException");

                }


            } catch (RemoteException ex) {
                System.out.println("Error: binding failt");
            }
            
        }else{
            System.out.println("Wrong argument count.");
        }
        
    }

    //read registryProperties
    private static void readProperties(){
        InputStream in = ClassLoader.getSystemResourceAsStream("registry.properties");
        if(in!=null){

            Properties props = new Properties();
            try{
                try {
                    props.load(in);
                } catch (IOException ex) {
                    System.out.println("Error:Loding Properties-File");
                }

                registryHost = props.getProperty("registry.host");
                registryPort = Integer.parseInt(props.getProperty("registry.port"));

           }finally{
                try {
                    in.close();
                } catch (IOException ex) {
                    System.out.println("Error: Closing InputStream");
                }

           }
        }else{
            System.out.println("Error: Properties-File not found!");
        }
     }

    
   @Override
   public BillingServerSecure login(String username, String password) throws RemoteException{
       	   
       String psw = readProperties(username);
       String passwordhash = pwtoMD5(password);

       if(psw.equals(passwordhash)){
            BillingServerSecure billingServerSecure = new BillingServerSecureImpl();
            BillingServerSecure billingServerSecureStub = (BillingServerSecure) UnicastRemoteObject.exportObject(billingServerSecure, 0);
            return billingServerSecureStub;

           
       } else {
           System.out.println("Wrong password");
       }

       if(passwordhash == null){
           System.out.println("User does not exist!");
           return null;
       }



       return null;
   }
   //read UserProperties
   private String readProperties(String username){
        InputStream in = ClassLoader.getSystemResourceAsStream("user.properties");
        if(in!=null){

            Properties props = new Properties();
            try{
                try {
                    props.load(in);
                } catch (IOException ex) {
                    System.out.println("Error:Loding Properties-File");
                }

                String psw = props.getProperty(username);
                return psw;
           }finally{
                try {
                    in.close();
                } catch (IOException ex) {
                    System.out.println("Error: Closing InputStream");
                }

           }
        }else{
            System.out.println("Error: Properties-File not found!");
            return null;
        }
     }


   private String pwtoMD5(String pw){
        try {
            byte[] byteOfPw = pw.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(byteOfPw);
            String pwtohash = String.format("%1$032x", new BigInteger(1, thedigest));
            return pwtohash;
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(BillingServerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(BillingServerImpl.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null; 
   }
}
