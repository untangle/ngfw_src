/**
 * $Id$
 */

package com.untangle.app.tunnel_vpn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileReader;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;

/**
 * Monitors the OpenVPN daemon for active tunnels, capturing statistics and
 * automatically restarting any that terminate unexpectedly.
 * 
 * @author mahotz
 * 
 */
class TunnelVpnMonitor implements Runnable
{
    private static final long TRAFFIC_CHECK_INTERVAL = (60 * 1000);
    private static final long PROCESS_RESTART_DELAY = (30 * 1000);

    /* Delay a second while the thread is joining */
    private static final long THREAD_JOIN_TIME_MSEC = 1000;

    /* Interrupt if there is no traffic for 2 seconds */
    private static final int READ_TIMEOUT = 2000;

    private static final String STATE_CMD = "state";
    private static final String STATUS_CMD = "status";
    private static final String XMIT_MARKER = "TCP/UDP write bytes";
    private static final String RECV_MARKER = "TCP/UDP read bytes";
    private static final String END_MARKER = "end";

    protected final Logger logger = Logger.getLogger(getClass());

    private final ConcurrentHashMap<Integer, TunnelVpnTunnelStatus> tunnelStatusList = new ConcurrentHashMap<Integer, TunnelVpnTunnelStatus>();
    private final TunnelVpnManager manager;
    private final TunnelVpnApp app;

    private Thread thread = null;
    private volatile boolean isAlive = false;
    private volatile long lastTrafficCheck = 0;
    private volatile long cycleCount = 0;

    /**
     * Constructor
     * 
     * @param app
     *        The Tunnel VPN application
     * @param manager
     *        The Tunnel VPN manager
     */
    protected TunnelVpnMonitor(TunnelVpnApp app, TunnelVpnManager manager)
    {
        this.app = app;
        this.manager = manager;
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

        while (isAlive) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                logger.info("tunnelvpn monitor was interrupted");
            }

            if (!isAlive) break;

            /* check that all the enabled tunnels are running and active */
            checkTunnelProcesses();

            /* generate the status and traffic statistics for each tunnel */
            generateTunnelStatistics();
        }

        logger.debug("Finished");
    }

    /**
     * Called to start monitoring
     */
    public synchronized void start()
    {
        isAlive = true;

        logger.debug("Starting TunnelVpn monitor");

        /*
         * If thread is not-null, there is a running thread that thinks it is
         * alive
         */
        if (thread != null) {
            logger.debug("TunnelVpn monitor is already running");
            return;
        }

        thread = UvmContextFactory.context().newThread(this);
        thread.start();
    }

    /**
     * Called to stop monitoring
     */
    public synchronized void stop()
    {
        isAlive = false;

        if (thread != null) {
            logger.debug("Stopping TunnelVpn monitor");
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
     * Checks that all enabled tunnels have a running OpenVPN process. If any
     * are missing the process is restarted.
     */
    private void checkTunnelProcesses()
    {
        cycleCount += 1;

        for (TunnelVpnTunnelSettings tunnel : app.getSettings().getTunnels()) {

            TunnelVpnTunnelStatus status = tunnelStatusList.get(tunnel.getTunnelId());

            if (status == null) {
                status = new TunnelVpnTunnelStatus(tunnel.getTunnelId());
                tunnelStatusList.put(tunnel.getTunnelId(), status);
            }

            status.setTunnelName(tunnel.getName());
            status.setCycleCount(cycleCount);

            // ignore tunnels that are not enabled
            if (!tunnel.getEnabled()) continue;

            try {
                File pidFile = new File("/var/run/tunnelvpn/tunnel-" + tunnel.getTunnelId() + ".pid");
                if (!pidFile.exists()) {
                    // sleep 10 seconds, sometimes the PID file takes a while to appear
                    try { Thread.sleep(10000); } catch (InterruptedException e) {}
                }
                if (!pidFile.exists()) {
                    restartDeadTunnel("missing", tunnel, status);
                    continue;
                }

                BufferedReader reader = new BufferedReader(new FileReader(pidFile));
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
                    restartDeadTunnel("dead", tunnel, status);
                    continue;
                }

            } catch (Exception exn) {
                logger.warn("Failed to check openvpn pid file.", exn);
            }
        }

        /*
         * Tunnel status objects that didn't get updated are left over from a
         * tunnel that was deleted so we clean them up here.
         */
        for (Map.Entry<Integer, TunnelVpnTunnelStatus> entry : tunnelStatusList.entrySet()) {
            Integer key = entry.getKey();
            TunnelVpnTunnelStatus value = entry.getValue();
            if (value.getCycleCount() != cycleCount) tunnelStatusList.remove(value.getTunnelId());
        }

    }

    /**
     * Connects to the openvpn management port for every active tunnel to get
     * the connection and traffic stats. We also watch for transitions between
     * CONNECTED and DISCONNECTED and generate log events as required.
     */
    protected void generateTunnelStatistics()
    {
        TunnelVpnTunnelStatus status = null;
        TunnelVpnEvent event = null;

        for (TunnelVpnTunnelSettings tunnel : app.getSettings().getTunnels()) {

            status = tunnelStatusList.get(tunnel.getTunnelId());

            if (status == null) {
                status = new TunnelVpnTunnelStatus(tunnel.getTunnelId());
                tunnelStatusList.put(tunnel.getTunnelId(), status);
            }

            status.setTunnelName(tunnel.getName());

            // if the tunnel is enabled grab the stats from the daemon
            if (tunnel.getEnabled()) {
                try {
                    status = updateTunnelStatus(tunnel, status);

                } catch (Exception exn) {
                    logger.warn("Failed to get status for " + tunnel.getName() + " [" + exn.getMessage() + "]");
                }

            }

            // the tunnel is not enabled so clear the stats
            else {
                status.clearTunnelStatus();
            }

            // if the state has not changed we do nothing
            if (status.getStateInfo().equals(status.getStateLast())) continue;

            /*
             * The state has changed so we log connect or disconnect events and
             * update the last state so we can detect the next change
             */

            if (status.getStateInfo().equals(TunnelVpnTunnelStatus.STATE_CONNECTED)) {
                event = new TunnelVpnEvent(status.getServerAddress(), status.getLocalAddress(), tunnel.getName(), TunnelVpnEvent.EventType.CONNECT);
                status.restartCount = 0;
                status.restartStamp = 0;
            }
            if (status.getStateInfo().equals(TunnelVpnTunnelStatus.STATE_DISCONNECTED)) {
                event = new TunnelVpnEvent(status.getServerAddress(), status.getLocalAddress(), tunnel.getName(), TunnelVpnEvent.EventType.DISCONNECT);
            }

            status.setStateLast(status.getStateInfo());

            if (event != null) {
                app.logEvent(event);
                logger.debug("logEvent(connect_event) " + event.toSummaryString());
            }
        }
    }

    /**
     * Updates the tunnel status for a specific tunnel
     * 
     * @param tunnel
     *        The tunnel to update
     * @param status
     *        The tunnel status object
     * @return The updated tunnel status object
     * @throws Exception
     */
    private TunnelVpnTunnelStatus updateTunnelStatus(TunnelVpnTunnelSettings tunnel, TunnelVpnTunnelStatus status) throws Exception
    {
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        long xmitBytes = 0;
        long recvBytes = 0;

        try {
            /* Connect to the management port */
            socket = new Socket((String) null, TunnelVpnApp.BASE_MGMT_PORT + tunnel.getTunnelId());

            socket.setSoTimeout(READ_TIMEOUT);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            /* Read out the hello message */
            in.readLine();

            /* First get the state */
            out.write(STATE_CMD + "\n");
            out.flush();

            while (true) {
                String line = in.readLine().trim();
                if (line.equalsIgnoreCase(END_MARKER)) break;

                /*
                 * We expect something that looks like this:
                 * 1522187757,CONNECTED,SUCCESS,10.8.8.113,185.93.1.93,1194,,
                 */
                String array[] = line.split(",");
                if (array.length < 5) continue;

                status.setConnectStamp(Long.valueOf(array[0]));
                status.setStateInfo(array[1]);
                status.setLocalAddress(InetAddress.getByName(array[3]));
                status.setServerAddress(InetAddress.getByName(array[4]));
            }

            /* Now get the status */
            out.write(STATUS_CMD + "\n");
            out.flush();

            while (true) {
                String line = in.readLine().trim();
                if (line.equalsIgnoreCase(END_MARKER)) break;

                String array[] = line.split(",");
                if (array.length != 2) continue;

                if (array[0].equalsIgnoreCase(XMIT_MARKER)) {
                    status.setXmitTotal(Long.valueOf(array[1]));
                }

                if (array[0].equalsIgnoreCase(RECV_MARKER)) {
                    status.setRecvTotal(Long.valueOf(array[1]));
                }
            }

        } finally {
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        }

        /*
         * If neither value changed there is nothing to log so just return
         */
        if ((status.getXmitLast() == status.getXmitTotal()) && (status.getRecvLast() == status.getRecvTotal())) return (status);

        /*
         * If not time to write a traffic stats record just return
         */
        long currentTime = System.currentTimeMillis();
        if (currentTime < (lastTrafficCheck + TRAFFIC_CHECK_INTERVAL)) return (status);
        lastTrafficCheck = currentTime;

        /*
         * The stats for each the tunnel will be cleared if the connection dies
         * and we restart openvpn, so we look for values less than we saw on the
         * previous check and handle accordingly. Once we calculate the number
         * of XMIT and RECV bytes since the last check, we update the last
         * values and write the stat record to the database. If the tunnel is
         * not connected we skip the calculation because openvpn may give
         * garbage values.
         */

        if (status.getStateInfo().equals(TunnelVpnTunnelStatus.STATE_CONNECTED)) {
            if (status.getXmitTotal() < status.getXmitLast()) xmitBytes = status.getXmitTotal();
            else xmitBytes = (status.getXmitTotal() - status.getXmitLast());

            if (status.getRecvTotal() < status.getRecvLast()) recvBytes = status.getRecvTotal();
            else recvBytes = (status.getRecvTotal() - status.getRecvLast());

            status.setXmitLast(status.getXmitTotal());
            status.setRecvLast(status.getRecvTotal());
        }

        /*
         * don't bother logging an event if there was no traffic
         */
        if ((xmitBytes == 0) && (recvBytes == 0)) return (status);

        TunnelVpnStatusEvent event = new TunnelVpnStatusEvent(tunnel.getName(), recvBytes, xmitBytes);
        app.logEvent(event);
        logger.debug("logEvent(traffic_event) " + event.toString());

        return (status);
    }

    /**
     * Restarts a dead tunnel
     * 
     * @param reason
     *        The reson for the restart
     * @param tunnel
     *        The tunnel to be restarted
     * @param status
     *        The tunnel status object
     */
    private void restartDeadTunnel(String reason, TunnelVpnTunnelSettings tunnel, TunnelVpnTunnelStatus status)
    {
        long currentTime = System.currentTimeMillis();
        long futureTime = 0;

        // first time here we initialize the throttling logic and log the event
        if (status.restartCount == 0) {
            logger.debug("Initializing retry throttling for " + tunnel.getName() + " (" + tunnel.getTunnelId() + ")");
            status.restartStamp = currentTime;

            // change the state to disconnected so stats will log an event
            status.setStateInfo(TunnelVpnTunnelStatus.STATE_DISCONNECTED);
            generateTunnelStatistics();
        }

        futureTime = (status.restartStamp + (status.restartCount * PROCESS_RESTART_DELAY));

        // if not yet time to restart again just return
        if (currentTime < futureTime) {
            logger.debug("Throttling retry for " + tunnel.getName() + " (" + tunnel.getTunnelId() + ") COUNT:" + status.restartCount + " CUR:" + currentTime + " FUT:" + futureTime);
            return;
        }

        // double the throttling delay after each attempt
        if (status.restartCount == 0) status.restartCount = 1;
        status.restartCount = (status.restartCount * 2);

        logger.warn("Restarting OpenVPN process for " + tunnel.getName() + " (" + tunnel.getTunnelId() + ") Reason: " + reason);
        manager.launchProcess(tunnel);
    }

    /**
     * Gets a list of the status for each active tunnel
     * 
     * @return The tunnel status list
     */
    public LinkedList<TunnelVpnTunnelStatus> getTunnelStatusList()
    {
        LinkedList<TunnelVpnTunnelStatus> statusList = new LinkedList<TunnelVpnTunnelStatus>();

        // if the app is not running just return an empty list
        if (app.getRunState() != AppSettings.AppState.RUNNING) {
            return (statusList);
        }

        checkTunnelProcesses();
        generateTunnelStatistics();

        for (Map.Entry<Integer, TunnelVpnTunnelStatus> entry : tunnelStatusList.entrySet()) {
            Integer key = entry.getKey();
            TunnelVpnTunnelStatus value = entry.getValue();
            statusList.add(value);
        }

        return (statusList);
    }

    /**
     * Restart a tunnel
     * 
     * @param tunnelId
     *        The tunnel ID to be restarted
     */
    public void recycleTunnel(int tunnelId)
    {
        TunnelVpnTunnelStatus status = tunnelStatusList.get(tunnelId);
        status.setStateInfo(TunnelVpnTunnelStatus.STATE_DISCONNECTED);
        generateTunnelStatistics();
    }
}
