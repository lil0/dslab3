/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package loadTesting;

import java.io.BufferedReader;
import java.io.IOException;

/**
 *
 * @author lisibauernhofer
 */
public class TestRead extends Thread{

        private BufferedReader in;

    TestRead(BufferedReader in) {
        this.in = in;
    }







     public void run(){
        String serverIn;
        //Antworten vom Server werden emfpangen und ausgegeben
        try {
            while ((serverIn = in.readLine()) != null) {
               
                 // System.out.println(serverIn);

                    
                }


            
            in.close();
        } catch (IOException ex) {
            System.out.println("fehler im ");
        }
    }


//

}
