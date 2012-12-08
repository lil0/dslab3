/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package loadTesting;

import managementClient.ManagementClient;

/**
 *
 * @author lisibauernhofer
 */
public class TestMC extends Thread{

    private static String[] args;

    TestMC(String[] argsh) {
        this.args = argsh;
    }

    public void run() {
        ManagementClient.main(args);

    }



}
