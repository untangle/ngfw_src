/**
 * $Id$
 */

package com.untangle.app.openvpn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTableEntry;

/**
 * Monitors persistent instances of the OpenVPN daemon and handles restart if
 * any that should be running are found to have disappeared.
 * 
 * @author mahotz
 * 
 */
class OpenVpnMonitor implements Runnable
{
    /* Poll every 5 seconds */
    private static final long SLEEP_TIME_MSEC = 5 * 1000;

    /* Log every 5 minutes */
    private static final long KILL_UNDEF_TIME_MSEC = 5 * 60 * 1000;

    /* Delay a second while the thread is joining */
    private static final long THREAD_JOIN_TIME_MSEC = 1000;

    /* Interrupt if there is no traffic for 2 seconds */
    private static final int READ_TIMEOUT = 2000;

    private static final String KILL_CMD = "kill";
    private static final String STATUS_CMD = "status 2";
    private static final String KILL_UNDEF = KILL_CMD + " UNDEF";
    private static final String END_MARKER = "end";

    private static final int TYPE_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int ADDRESS_INDEX = 2;
    private static final int ADDRESS_POOL_INDEX = 3;
    private static final int RX_INDEX = 5;
    private static final int TX_INDEX = 6;
    private static final int START_INDEX = 8;
    private static final int TOTAL_INDEX = 12;

    protected static final Logger logger = Logger.getLogger(OpenVpnMonitor.class);

    /* Maps the active client names to their current state and stats */
    private Map<String, ClientState> activeMap = null;

    private final OpenVpnAppImpl app;

    /* The thread the monitor is running on */
    private Thread thread = null;

    /* Status of the monitor */
    private volatile boolean isAlive = true;

    /* Whether or not openvpn is started */
    private volatile boolean isEnabled = false;

    /**
     * Constructor
     * 
     * @param app
     *        The OpenVPN application
     */
    protected OpenVpnMonitor(OpenVpnAppImpl app)
    {
        this.app = app;
    }

    /**
     * The main run function
     */
    public void run()
    {
        if (!isAlive) {
            logger.error("died before starting");
            return;
        }

        logger.debug("Starting");
        activeMap = new ConcurrentHashMap<String, ClientState>();

        long nextKillUndefTime = System.currentTimeMillis() + KILL_UNDEF_TIME_MSEC;
        long now = System.currentTimeMillis();

        while (true) {
            if (!isAlive) break;
            try {
                Thread.sleep(SLEEP_TIME_MSEC);
            } catch (InterruptedException e) {
                logger.info("openvpn monitor was interrupted");
            }
            if (!isAlive) break;

            // Grab lock, such that a concurrent read of the "activeMap"
            // doesn't happen during an update
            synchronized (this) {
                try {
                    /*
                     * Cleanup UNDEF sessions every time you are going to update
                     * the stats
                     */
                    now = System.currentTimeMillis();

                    if (now > nextKillUndefTime) updateServerStatus(true);
                    else updateServerStatus(false);

                } catch (java.net.ConnectException e) {
                    logger.debug("Unable to connect to OpenVPN - trying again in " + SLEEP_TIME_MSEC + " ms.");
                } catch (Exception e) {
                    logger.info("Error updating status", e);
                }

                if (now > nextKillUndefTime) {
                    nextKillUndefTime = System.currentTimeMillis() + KILL_UNDEF_TIME_MSEC;
                }
            }

            /**
             * Check that all necessary clients are running
             */
            checkRemoteServerProcesses();
            checkServerProcess();
        }

        /* Flush out DISCONNECTS for all connected clients */
        for (ClientState stats : activeMap.values())
            stats.isActive = false;
        flushLogEvents();

        logger.debug("Finished");
    }

    /**
     * Returns a list of active clients without an end date
     * 
     * @return List of active clients
     */
    public synchronized List<OpenVpnStatusEvent> getOpenConnectionsAsEvents()
    {
        List<OpenVpnStatusEvent> list = new ArrayList<OpenVpnStatusEvent>();

        if (activeMap == null) return list;

        synchronized (activeMap) {
            for (ClientState stats : activeMap.values()) {
                if (stats.isActive) {
                    OpenVpnStatusEvent statusEvent = new OpenVpnStatusEvent(new Timestamp(stats.start.getTime()), stats.address, stats.port, stats.poolAddress, stats.name, stats.bytesRxTotal, stats.bytesTxTotal, stats.bytesRxDelta, stats.bytesTxDelta);
                    statusEvent.setEnd(null);
                    list.add(statusEvent);
                }
            }
        }

        return list;
    }

    /**
     * Called to start monitoring
     */
    public synchronized void start()
    {
        isAlive = true;
        isEnabled = false;

        logger.debug("Starting OpenVpn monitor");

        /*
         * If thread is not-null, there is a running thread that thinks it is
         * alive
         */
        if (thread != null) {
            logger.debug("OpenVpn monitor is already running");
            return;
        }

        thread = UvmContextFactory.context().newThread(this);
        thread.start();
    }

    /**
     * Enabled monitoring
     */
    public synchronized void enable()
    {
        isEnabled = true;
    }

    /**
     * Disable monitoring
     */
    public synchronized void disable()
    {
        isEnabled = false;
    }

    /**
     * Called to stop monitoring
     */
    public synchronized void stop()
    {
        if (thread != null) {
            logger.debug("Stopping OpenVpn monitor");

            isAlive = false;
            try {
                thread.interrupt();
                thread.join(THREAD_JOIN_TIME_MSEC);
            } catch (SecurityException e) {
                logger.error("security exception, impossible", e);
            } catch (InterruptedException e) {
                logger.error("interrupted while stopping", e);
            }
            thread = null;
        }
    }

    /**
     * Called to get the status of the OpenVPN server
     * 
     * @param killUndef
     *        True if undefined clients should be killed
     * @throws UnknownHostException
     * @throws SocketException
     * @throws IOException
     */
    private void updateServerStatus(boolean killUndef) throws UnknownHostException, SocketException, IOException
    {
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;

        try {
            /* Connect to the management port */
            socket = new Socket((String) null, OpenVpnSettings.MANAGEMENT_PORT);

            socket.setSoTimeout(READ_TIMEOUT);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            /* Read out the hello message */
            in.readLine();

            /* First kill all of the undefined connections if necessary */
            if (killUndef) {
                logger.debug("Killing all undefined clients");
                writeCommandAndFlush(out, in, KILL_UNDEF);
            }

            /* Now get the status */
            writeCommand(out, STATUS_CMD);

            /* Set all of the stats to not updated */
            for (ClientState stats : activeMap.values())
                stats.updated = false;

            /* Preload, so it is is safe to send commands while processeing */
            List<String> clientStatus = new LinkedList<String>();

            while (true) {
                String line = in.readLine().trim();
                if (line.equalsIgnoreCase(END_MARKER)) break;
                clientStatus.add(line);
            }

            for (String line : clientStatus)
                processLine(line);

            /* Log disconnects and connects */
            flushLogEvents();

            /* Check for any dead connections */
            killDeadConnections(out, in);
        } finally {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null){
                try{
                    socket.close();
                }catch( Exception e){
                    logger.warn("Failed to close socket.", e);
                }
            }
        }
    }

    /**
     * Flush out any remainging log events and indicate that all of the active
     * sessions have completed
     */
    private void flushLogEvents()
    {
        Timestamp now = new Timestamp((new Date()).getTime());

        for (ClientState stats : activeMap.values()) {
            OpenVpnStatusEvent statusEvent = new OpenVpnStatusEvent(new Timestamp(stats.start.getTime()), stats.address, stats.port, stats.poolAddress, stats.name, stats.bytesRxTotal, stats.bytesTxTotal, stats.bytesRxDelta, stats.bytesTxDelta);
            statusEvent.setEnd(now);

            if (logger.isDebugEnabled()) logger.debug("Logging stats for " + stats.name);
            app.logEvent(statusEvent);
        }
    }

    /**
     * Kill dead client connections
     * 
     * @param out
     *        Output to the management channel
     * @param in
     *        Input from the management channel
     * @throws IOException
     */
    private void killDeadConnections(BufferedWriter out, BufferedReader in) throws IOException
    {
        for (Iterator<ClientState> iter = activeMap.values().iterator(); iter.hasNext();) {
            ClientState stats = iter.next();

            if (stats.isActive && stats.updated) continue;

            /* Remove any apps that are not active */
            iter.remove();

            /* If this client was in the current list of clients, then kill it */
            if (stats.updated) {
                String command = KILL_CMD + " " + stats.address.getHostAddress() + ":" + stats.port;
                writeCommandAndFlush(out, in, command);
            }

            /* log event */
            logger.info("OpenVPN client disconnected: " + stats.name);
            OpenVpnEvent connectEvent = new OpenVpnEvent(stats.address, stats.poolAddress, stats.name, OpenVpnEvent.EventType.DISCONNECT);
            app.logEvent(connectEvent);

            /* set the openvpn username of the host back to null */
            if (stats.name != null) {
                HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(stats.poolAddress, false);
                if (entry != null) {
                    entry.setUsernameOpenVpn(null);
                }
            }
        }
    }

    /**
     * Process a line read from the management channel
     * 
     * @param line
     *        The line to process
     */
    private void processLine(String line)
    {
        String valueArray[] = line.split(",");
        if (!valueArray[TYPE_INDEX].equals("CLIENT_LIST")) return;

        if (valueArray.length != TOTAL_INDEX) {
            logger.info("Unexpected client description length: " + valueArray.length + " Ignoring: " + line);
            return;
        }

        String name = valueArray[NAME_INDEX];

        /* Ignore undef entries */
        if (name.equalsIgnoreCase("undef")) return;

        String addressAndPort[] = valueArray[ADDRESS_INDEX].split(":");
        if (addressAndPort.length != 2) {
            logger.info("Strange address description, ignoring: " + line);
            return;
        }

        String poolAddressStr = valueArray[ADDRESS_POOL_INDEX];
        if ("127.0.0.1".equals(poolAddressStr)) {
            logger.warn("Ignoring client with 127.0.0.1 address: " + name + " " + poolAddressStr);
            return;
        }

        InetAddress address = null;
        InetAddress poolAddress = null;
        int port = 0;
        long bytesRx = 0;
        long bytesTx = 0;
        Date start = null;

        try {
            address = InetAddress.getByName(addressAndPort[0]);
            port = Integer.parseInt(addressAndPort[1]);
            poolAddress = InetAddress.getByName(poolAddressStr);
            bytesRx = Long.parseLong(valueArray[RX_INDEX]);
            bytesTx = Long.parseLong(valueArray[TX_INDEX]);
            start = new Date(Long.parseLong(valueArray[START_INDEX]) * 1000);
        } catch (Exception e) {
            logger.warn("Unable to parse line: " + line, e);
            return;
        }

        if ("127.0.0.1".equals(poolAddress.getHostAddress())) {
            logger.warn("Ignoring client with 127.0.0.1 address: " + name + " " + poolAddress.getHostAddress());
            return;
        }

        // set the global openvpn username in the host table
        if (poolAddress != null) {
            HostTableEntry entry = UvmContextFactory.context().hostTable().getHostTableEntry(poolAddress, true);
            entry.setHostnameOpenVpn(name);
            entry.setUsernameOpenVpn(name);
        }

        ClientState stats = activeMap.get(name);

        if (stats != null) {
            if (logger.isDebugEnabled()) logger.debug("OpenVPN client status updated: " + stats.name);
            stats.update(bytesRx, bytesTx);

            return;
        }

        if (stats == null) {
            app.incrementConnectCount();

            OpenVpnEvent connectEvent = new OpenVpnEvent(address, poolAddress, name, OpenVpnEvent.EventType.CONNECT);
            app.logEvent(connectEvent);

            stats = new ClientState(name, address, port, poolAddress, start, bytesRx, bytesTx);
            stats.isActive = true;
            stats.updated = true;

            logger.info("OpenVPN client connected: " + stats.name);
            activeMap.put(name, stats);
        }

    }

    /**
     * Write a command to the management channel
     * 
     * @param out
     *        The output to the management channel
     * @param command
     *        The command to write
     * @throws IOException
     */
    private void writeCommand(BufferedWriter out, String command) throws IOException
    {
        out.write(command + "\n");
        out.flush();
    }

    /**
     * Write a command to the management channel, read and ignore the result
     * 
     * @param out
     *        The output to the management channel
     * @param in
     *        The input from the management channel
     * @param command
     *        The command to write
     * @throws IOException
     */
    private void writeCommandAndFlush(BufferedWriter out, BufferedReader in, String command) throws IOException
    {
        writeCommand(out, command);

        /* Read out the response, ignore it */
        in.readLine();
    }

    /**
     * Checks that all enabled remote servers have a running OpenVpn process. If
     * one is missing it restarts it
     */
    private void checkRemoteServerProcesses()
    {
        BufferedReader reader;
        for (OpenVpnRemoteServer server : app.getSettings().getRemoteServers()) {
            if (!server.getEnabled()) continue;

            reader = null;
            try {
                File pidFile = new File("/var/run/openvpn." + server.getName() + ".pid");
                if (!pidFile.exists()) continue;

                reader = new BufferedReader(new FileReader(pidFile));
                String currentLine;
                String contents = "";
                while ((currentLine = reader.readLine()) != null) {
                    contents += currentLine;
                }

                int pid;
                try {
                    pid = Integer.parseInt(contents);
                } catch (Exception e) {
                    logger.warn("Unable to parse pid file: " + contents);
                    continue;
                }

                File procFile = new File("/proc/" + pid);
                if (!procFile.exists()) {
                    logger.warn("OpenVpn process for " + server.getName() + " (" + pid + ") missing. Restarting...");
                    UvmContextFactory.context().execManager().exec("systemctl restart openvpn@" + server.getName() + ".service");
                }

            } catch (Exception e) {
                logger.warn("Failed to check openvpn pid file.", e);
            } finally {
                if (reader != null){
                    try{
                        reader.close();
                    } catch (Exception e) {
                        logger.warn("Failed to check openvpn pid file.", e);
                    }
                }
            }
        }
    }

    /**
     * Checks that the main server has a running OpenVpn process. If it is
     * missing it restarts it
     */
    private void checkServerProcess()
    {
        if (!this.app.getSettings().getServerEnabled()) return;

        BufferedReader reader = null;
        try {
            File pidFile = new File("/var/run/openvpn.server.pid");
            if (!pidFile.exists()) return;

            reader = new BufferedReader(new FileReader(pidFile));
            String currentLine;
            String contents = "";
            while ((currentLine = reader.readLine()) != null) {
                contents += currentLine;
            }

            int pid;
            try {
                pid = Integer.parseInt(contents);
            } catch (Exception e) {
                logger.warn("Unable to parse pid file: " + contents);
                return;
            }

            File procFile = new File("/proc/" + pid);
            if (!procFile.exists()) {
                logger.warn("OpenVpn server process (" + pid + ") missing. Restarting...");
                UvmContextFactory.context().execManager().exec("systemctl restart openvpn@server.service");
            }
        } catch (Exception e) {
            logger.warn("Failed to check openvpn pid file.", e);
        } finally {
            if (reader != null){
                try{
                    reader.close();
                } catch (Exception e) {
                    logger.warn("Failed to check openvpn pid file.", e);
                }
            }
        }
    }

}

/**
 * Class to hold the state of a client
 * 
 * @author mahotz
 * 
 */
class ClientState
{
    String name; /* The client name */
    InetAddress address; /* The remove client address */
    InetAddress poolAddress; /* The pool address given to the client */
    int port; /* The remote client port */
    Date start; /* The date the client connected */
    long bytesRxDelta; /* Total bytes received since the last event */
    long bytesRxTotal; /* Total bytes received */
    long bytesTxDelta; /* Total bytes transferred since the last event */
    long bytesTxTotal; /* Total bytes transferred */
    Date lastUpdate; /* date of last update */
    boolean updated; /*
                      * stores whether this client state was updated/seen when
                      * last communicating with openvpn
                      */
    boolean isActive; /* stores if this client is active/connected */

    /**
     * Constructore
     * 
     * @param name
     *        Client name
     * @param address
     *        Client address
     * @param port
     *        Client port
     * @param poolAddress
     *        Assigned address
     * @param start
     *        Connection start time
     * @param bytesRx
     *        Bytes received
     * @param bytesTx
     *        Bytes transmitted
     */
    protected ClientState(String name, InetAddress address, int port, InetAddress poolAddress, Date start, long bytesRx, long bytesTx)
    {
        this.name = name;
        this.address = address;
        this.port = port;
        this.poolAddress = poolAddress;
        this.start = start;
        this.bytesRxTotal = bytesRx;
        this.bytesRxDelta = bytesRx;
        this.bytesTxTotal = bytesTx;
        this.bytesTxDelta = bytesTx;
        this.lastUpdate = new Date();
        this.isActive = true;
    }

    /**
     * Update the traffic counters
     * 
     * @param newBytesRxTotal
     *        New total RX bytes
     * @param newBytesTxTotal
     *        New total TX bytes
     */
    void update(long newBytesRxTotal, long newBytesTxTotal)
    {
        long prevBytesRxTotal = this.bytesRxTotal;
        long prevBytesTxTotal = this.bytesTxTotal;
        this.bytesRxTotal = newBytesRxTotal;
        this.bytesTxTotal = newBytesTxTotal;
        this.bytesRxDelta = this.bytesRxTotal - prevBytesRxTotal;
        this.bytesTxDelta = this.bytesTxTotal - prevBytesTxTotal;
        this.updated = true;
    }
}
