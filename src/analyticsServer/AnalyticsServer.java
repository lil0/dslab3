package analyticsServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import analyticsServer.AnalyticsRMIInterface;

public class AnalyticsServer {

	/**
	 * Arg 1: Bindingname for AnalyticsServer
	 */
	static String registryHost = "";
	static String analBind = "";
	static int registryPort = 0;
	static Registry registry;
	static AnalyticsRMIInterface stub;
	static BufferedReader stdin = null;
	static AnalyticsRMIHandler rmiHandler = null;
	public static void main(String[] args) {

		if (args.length == 1) {
			analBind = args[0];

			readProperties();

			rmiHandler = new AnalyticsRMIHandler();
			// Create stub
			try {
				stub = (AnalyticsRMIInterface) UnicastRemoteObject.exportObject(rmiHandler, 0);
			} catch (RemoteException e) {
				System.out.println("couldn't export AnalyticsRMIInterface.");
			}

			// bind stub to Registry
			try {
				registry = LocateRegistry.getRegistry(registryHost,registryPort);
				try {
					registry.list();
				} catch (ConnectException e) {
					// Create Registry if no Registry exists
					registry = LocateRegistry.createRegistry(registryPort);		
				}
				// Bind AnalyticsStub to Registry
				registry.rebind(analBind, stub);									
			} catch (RemoteException e) {
				System.out.println("Could not bind Registry to port " + registryPort);
			}

			stdin = new BufferedReader(new InputStreamReader(System.in));
			String cmd;
			try {
				while ((cmd = stdin.readLine()) != null) {
					if (cmd.equals("!exit")) {
						shutdown();
						break;
					}
				}
			} catch (IOException e) {
				System.out.println("Error reading IO");
			}
		} else {
			System.out.println("Wrong argument count.");
		}
	}

	private static void readProperties() {
		java.io.InputStream is = ClassLoader.getSystemResourceAsStream("registry.properties"); 
		if (is != null) {
			java.util.Properties props = new java.util.Properties();
			try {
				try {
					props.load(is);
				} catch (IOException e) {
					System.out.println("Error handling configuration file.");
				}
				registryHost = props.getProperty("registry.host");
				registryPort = Integer.parseInt(props.getProperty("registry.port"));

			} finally {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			System.err.println("Properties file not found!");
		}
	}
	private static void shutdown() {
		try {
			stdin.close();
		} catch (IOException e) {
			System.out.println("Error freeing ressources");
		}
		try {
			registry.unbind(analBind);
		} catch (Exception e) {
			System.out.println("Error unbinding registry");
		}
		try {
			UnicastRemoteObject.unexportObject(rmiHandler, true);
		} catch (NoSuchObjectException e) {
			System.out.println("Error unexporting remote object");
		}
	}
}
