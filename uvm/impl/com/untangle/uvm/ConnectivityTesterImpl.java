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

    private static final String UVM_BASE = System.getProperty("uvm.home");
    private static final String DNS_TEST_SCRIPT = UVM_BASE + "/bin/ut-dns-test";

    /* Name of the host to lookup */
    private static final String TEST_HOSTNAME = "updates.untangle.com";

    /* Port the TCP test will try to connect to */
    private static final int TCP_TEST_PORT = 80;

    /* The amount of time before giving up on the DNS attempt in milliseconds */
    private static final int DNS_TEST_TIMEOUT_MS = 5000;
    private static final int TCP_TEST_TIMEOUT_MS = 10000;

    private static ConnectivityTesterImpl INSTANCE = new ConnectivityTesterImpl();

    /* Address of updates */
    private InetAddress address;

    /**
     * Private constructor
     */
    private ConnectivityTesterImpl()
    {
        try {
            this.address = InetAddress.getByName(TEST_HOSTNAME);
        } catch (UnknownHostException e) {
            this.address = null;
        }
    }
    
    /**
     * Retrieve the connectivity tester
     * 
     * @return The connectivity tester
     */
    public JSONObject getStatus()
    {
        InterfaceSettings wan = UvmContextFactory.context().networkManager().findInterfaceFirstWan();

        if (wan == null) {
            logger.warn("Failed to find WAN interface");
            return makeJsonObject(false, false);
        }

        InetAddress dnsPrimary = UvmContextFactory.context().networkManager().getInterfaceStatus(wan.getInterfaceId()).getV4Dns1();
        InetAddress dnsSecondary = UvmContextFactory.context().networkManager().getInterfaceStatus(wan.getInterfaceId()).getV4Dns2();

        /* Returns the lookuped address if DNS is working, or null if it is not */
        return makeJsonObject(isDnsWorking(dnsPrimary, dnsSecondary), isTcpWorking());
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
        boolean isWorking = false;
        String primaryServer = null;
        String secondaryServer = null;

        if (dnsPrimaryServer != null) primaryServer = dnsPrimaryServer.getHostAddress();
        if (dnsSecondaryServer != null) secondaryServer = dnsSecondaryServer.getHostAddress();

        if (primaryServer != null && UvmContextFactory.context().execManager().execResult(DNS_TEST_SCRIPT + " " + primaryServer) == 0) isWorking = true;
        if (secondaryServer != null && UvmContextFactory.context().execManager().execResult(DNS_TEST_SCRIPT + " " + secondaryServer) == 0) isWorking = true;

        /* Now run the dns test just to get the address of updates */
        DnsLookup dnsLookup = new DnsLookup();
        Thread test = new Thread(dnsLookup);

        test.start();

        try {
            test.join(DNS_TEST_TIMEOUT_MS);
            if (test.isAlive()) test.interrupt();
        } catch (InterruptedException e) {
            logger.error("Interrupted while testing DNS connectivity.", e);
        }

        this.address = dnsLookup.address;

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
        int testPort;
        
        if (this.address != null) {
            testAddress = this.address;
            testPort = TCP_TEST_PORT;
        } else {
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
     * This isn't a test, it is just a method used to lookup the address of
     * updates.untangle.com with a timeout. The real test is now executed by the
     * script. For the original test, look at subversion R2828
     */
    class DnsLookup implements Runnable
    {
        InetAddress address = null;

        /**
         * Constructor
         */
        public DnsLookup()
        {
        }

        /**
         * Main runnable function
         */
        public void run()
        {
            /*
             * This always works after the first time, so it doesn't actually do
             * anything
             */
            try {
                logger.debug("Starting lookup");
                this.address = InetAddress.getByName(TEST_HOSTNAME);
                logger.debug("Found address: " + address);
                logger.debug("Completed lookup");
            } catch (UnknownHostException e) {
                this.address = null;
                logger.warn("Unable to look up host: " + TEST_HOSTNAME);
            }
        }
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
