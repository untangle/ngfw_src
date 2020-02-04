/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONObject;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.ConnectivityTester;
import com.untangle.uvm.network.InterfaceSettings;

/**
 * Class for testing network connectivity
 */
public class ConnectivityTesterImpl implements ConnectivityTester
{
    private final Logger logger = Logger.getLogger(getClass());

    /* Name of the DNS test script */
    private static final String DNS_TEST_SCRIPT = System.getProperty("uvm.bin.dir") + "/ut-dns-test";

    /* Port the TCP test will try to connect to */
    private static final int TCP_TEST_PORT = 80;

    /* The amount of time before giving up on the tests in milliseconds */
    private static final int TCP_TEST_TIMEOUT_MS = 10000;

    private static ConnectivityTesterImpl INSTANCE = new ConnectivityTesterImpl();

    /**
     * Private constructor
     */
    private ConnectivityTesterImpl() {}
    
    /**
     * Retrieve the connectivity tester
     * 
     * @return The connectivity tester
     */
    public JSONObject getStatus()
    {
        boolean isDnsWorking = true;
        for (InterfaceSettings intfSettings : UvmContextFactory.context().networkManager().getNetworkSettings().getInterfaces()) {
            if (!intfSettings.getIsWan())
                continue;

            InetAddress dnsPrimary = UvmContextFactory.context().networkManager().getInterfaceStatus(intfSettings.getInterfaceId()).getV4Dns1();
            InetAddress dnsSecondary = UvmContextFactory.context().networkManager().getInterfaceStatus(intfSettings.getInterfaceId()).getV4Dns2();

            if (!isDnsWorking(dnsPrimary, dnsSecondary)) {
                isDnsWorking = false;
            }
        }

        /* Returns the lookuped address if DNS is working, or null if it is not */
        return makeJsonObject(isDnsWorking, isTcpWorking());
    }

    /**
     * Test that DNS is working
     * 
     * @param dnsPrimaryServer
     *        The primary DNS server
     * @param dnsSecondaryServer
     *        The secondary DNS server
     * @return True if either of the DNS servers give a reply, otherwise false
     */
    public boolean isDnsWorking(InetAddress dnsPrimaryServer, InetAddress dnsSecondaryServer)
    {
        boolean isWorking = true;
        String primaryServer = null;
        String secondaryServer = null;
        String domainName = null;

        if (dnsPrimaryServer != null) primaryServer = dnsPrimaryServer.getHostAddress();
        if (dnsSecondaryServer != null) secondaryServer = dnsSecondaryServer.getHostAddress();

        domainName = UvmContextFactory.context().uriManager().getSettings().getDnsTestHost();

        if (primaryServer != null && UvmContextFactory.context().execManager().execResult(DNS_TEST_SCRIPT + " " + primaryServer + " " + domainName) != 0) isWorking = false;
        if (secondaryServer != null && UvmContextFactory.context().execManager().execResult(DNS_TEST_SCRIPT + " " + secondaryServer + " " + domainName) != 0) isWorking = false;

        return isWorking;
    }

    /**
     * Test that TCP is working
     * 
     * @return True if TCP is working, otherwise false
     */
    private boolean isTcpWorking()
    {
        InetAddress testAddress;
        int testPort = TCP_TEST_PORT;

        try {
            testAddress = InetAddress.getByName(UvmContextFactory.context().uriManager().getSettings().getTcpTestHost());
        } catch (UnknownHostException e) {
            testAddress = null;
        }

        if (testAddress == null) {
            logger.warn("TCP test has no DNS, using 8.8.8.8");
            try {
                testAddress = InetAddress.getByName("8.8.8.8");
            } catch (UnknownHostException e) {
                logger.warn("Unable to resolve 8.8.8.8",e);
                return false;
            }
            testPort = 53;
        }
        
        TcpTest tcpTest = new TcpTest(testAddress,testPort);
        Thread test = new Thread(tcpTest);

        test.start();

        try {
            test.join(TCP_TEST_TIMEOUT_MS);
            if (test.isAlive()) {
                test.interrupt();
            }
        } catch (InterruptedException e) {
            logger.error("Interrupted while testing TCP connectivity.", e);
        }

        return tcpTest.isWorking;
    }

    /**
     * Makes a JSON object from the DNS and TCP status flags
     * 
     * @param dnsWorking
     *        The DNS flag
     * @param tcpWorking
     *        The TCP flag
     * @return A JSON object
     */
    private JSONObject makeJsonObject(boolean dnsWorking, boolean tcpWorking)
    {
        JSONObject result = new JSONObject();
        try {
            result.put("dnsWorking", dnsWorking);
            result.put("tcpWorking", tcpWorking);
        } catch (Exception e) {
            logger.warn("JSON exception: ", e);
        }
        return result;
    }

    /**
     * Get the singleton instance
     * 
     * @return The instance
     */
    static ConnectivityTesterImpl getInstance()
    {
        return INSTANCE;
    }

    /**
     * Class to test TCP connectivity by attempting to establish a connection to
     * the specified address and a defined test port.
     */
    class TcpTest implements Runnable
    {
        private final InetAddress address;
        private final int port;
        
        boolean isWorking = false;

        /**
         * Constructor
         * 
         * @param address
         *        The target address for testing
         * @param port
         *        The port to test
         */
        public TcpTest(InetAddress address, int port)
        {
            this.address = address;
            this.port = port;
        }

        /**
         * Main runnable function
         */
        public void run()
        {
            try {
                logger.debug("Trying to connect to " + this.address);
                Socket socket = new Socket(this.address, this.port);
                socket.close();
                this.isWorking = true;
                logger.debug("Completed TCP Connection test");
            } catch (IOException e) {
                this.isWorking = false;
                logger.warn("Unable to connect to " + this.address);
            }
        }
    }
}
