/**
 * $Id$
 */

package com.untangle.app.tunnel_vpn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContext;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.HostTable;
import com.untangle.uvm.HostTableEntry;
import com.untangle.uvm.util.I18nUtil;

class TunnelVpnMonitor implements Runnable
{
    /* Poll every 60 seconds */
    private static final long SLEEP_TIME_MSEC = 60 * 1000;

    /* Delay a second while the thread is joining */
    private static final long THREAD_JOIN_TIME_MSEC = 1000;

    /* Interrupt if there is no traffic for 2 seconds */
    private static final int READ_TIMEOUT = 2000;

    private static final String STATE_CMD = "state";
    private static final String STATUS_CMD = "status";
    private static final String XMIT_MARKER = "TCP/UDP write bytes";
    private static final String RECV_MARKER = "TCP/UDP read bytes";
    private static final String END_MARKER = "end";

    protected static final Logger logger = Logger.getLogger(TunnelVpnMonitor.class);

    private final ConcurrentHashMap<Integer, TunnelVpnTunnelStatus> tunnelStatus = new ConcurrentHashMap<Integer, TunnelVpnTunnelStatus>();
    private final TunnelVpnManager manager;
    private final TunnelVpnApp app;

    /* The thread the monitor is running on */
    private Thread thread = null;

    /* Status of the monitor */
    private volatile boolean isAlive = true;

    /* Whether or not the monitor is enabled */
    private volatile boolean isEnabled = false;

    protected TunnelVpnMonitor(TunnelVpnApp app, TunnelVpnManager manager)
    {
        this.app = app;
        this.manager = manager;
    }

    public void run()
    {
        if (!isAlive) {
            logger.error("died before starting");
            return;
        }

        logger.debug("Starting");

        while (true) {
            if (!isAlive) break;
            try {
                Thread.sleep(SLEEP_TIME_MSEC);
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

    public synchronized void start()
    {
        isAlive = true;
        isEnabled = false;

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

    public synchronized void enable()
    {
        isEnabled = true;
    }

    public synchronized void disable()
    {
        isEnabled = false;
    }

    public synchronized void stop()
    {
        if (thread != null) {
            logger.debug("Stopping TunnelVpn monitor");

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
     * Checks that all enabled tunnels have a running OpenVPN process. If any
     * are missing the process is restarted.
     */
    private void checkTunnelProcesses()
    {
        for (TunnelVpnTunnelSettings tunnel : app.getSettings().getTunnels()) {

            // ignore tunnels that are not enabled
            if (!tunnel.getEnabled()) continue;

            TunnelVpnTunnelStatus status = tunnelStatus.get(tunnel.getTunnelId());

            if (status == null) {
                status = new TunnelVpnTunnelStatus(tunnel.getTunnelId());
                status.setTunnelName(tunnel.getName());
                tunnelStatus.put(tunnel.getTunnelId(), status);
            }

            logger.debug("Checking tunnel " + tunnel.getName() + " (" + tunnel.getTunnelId() + ")");

            try {
                File pidFile = new File("/var/run/tunnelvpn/tunnel-" + tunnel.getTunnelId() + ".pid");
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
    }

    protected void generateTunnelStatistics()
    {
        TunnelVpnTunnelStatus status = null;
        TunnelVpnEvent event;

        for (TunnelVpnTunnelSettings tunnel : app.getSettings().getTunnels()) {

            if (!tunnel.getEnabled()) continue;

            try {
                status = updateTunnelStatus(tunnel);

            } catch (Exception exn) {
                logger.warn("Failed to get status for " + tunnel.getName() + " [" + exn.getMessage() + "]");
            }

            if (status == null) status = getTunnelStatus(tunnel.getTunnelId());
            if (status == null) continue;

            if (status.getStateInfo().equals(status.getStateLast())) continue;

            if (status.getStateInfo().equals(TunnelVpnTunnelStatus.STATE_CONNECTED)) {
                event = new TunnelVpnEvent(status.getServerAddress(), status.getLocalAddress(), tunnel.getName(), TunnelVpnEvent.EventType.CONNECT);
                status.restartCount = 0;
                status.restartStamp = 0;
            } else {
                event = new TunnelVpnEvent(status.getServerAddress(), status.getLocalAddress(), tunnel.getName(), TunnelVpnEvent.EventType.DISCONNECT);
            }

            app.logEvent(event);
            logger.debug("TunnelVpnEvent(logEvent) " + event.toSummaryString());
            status.setStateLast(status.getStateInfo());
        }
    }

    public LinkedList<TunnelVpnTunnelStatus> getTunnelStatusList()
    {
        generateTunnelStatistics();

        LinkedList<TunnelVpnTunnelStatus> statusList = new LinkedList<TunnelVpnTunnelStatus>();

        for (Map.Entry<Integer, TunnelVpnTunnelStatus> entry : tunnelStatus.entrySet()) {
            Integer key = entry.getKey();
            TunnelVpnTunnelStatus value = entry.getValue();
            statusList.add(value);
        }

        return (statusList);
    }

    public TunnelVpnTunnelStatus getTunnelStatus(int tunnelId)
    {
        return tunnelStatus.get(tunnelId);
    }

    private TunnelVpnTunnelStatus updateTunnelStatus(TunnelVpnTunnelSettings tunnel) throws Exception
    {
        Socket socket = null;
        BufferedReader in = null;
        BufferedWriter out = null;
        long xmitBytes = 0;
        long recvBytes = 0;

        /* get the tunnel status object for the argumented tunnel */
        TunnelVpnTunnelStatus status = tunnelStatus.get(tunnel.getTunnelId());

        /* if status object not found create and add to hashmap */
        if (status == null) {
            status = new TunnelVpnTunnelStatus(tunnel.getTunnelId());
            status.setTunnelName(tunnel.getName());
            tunnelStatus.put(tunnel.getTunnelId(), status);
        }

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
                logger.debug("STATE: " + line);
                if (line.equalsIgnoreCase(END_MARKER)) break;

                /*
                 * We expect something that looks like this:
                 * 1506443582,CONNECTED,SUCCESS,10.8.8.27,181.215.110.250
                 */
                String array[] = line.split(",");
                if (array.length != 5) continue;

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
                logger.debug("STATUS: " + line);
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

        logger.debug("STATUS: " + status.toString());

        /*
         * if neither value changed there is nothing to log so just return
         */
        if ((status.getXmitLast() == status.getXmitTotal()) && (status.getRecvLast() == status.getRecvTotal())) return (status);

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
        logger.debug("updateTunnelStatus(logEvent) " + event.toString());

        return (status);
    }

    private void restartDeadTunnel(String reason, TunnelVpnTunnelSettings tunnel, TunnelVpnTunnelStatus status)
    {
        long currentTime = (System.currentTimeMillis() / 1000);
        long futureTime = 0;

        // first time here we initialize the throttling logic and log the event
        if (status.restartCount == 0) {
            logger.debug("Initializing retry throttling for " + tunnel.getName() + " (" + tunnel.getTunnelId() + ")");
            status.restartStamp = currentTime;

            // change the state to disconnected so stats will log an event
            status.setStateInfo(TunnelVpnTunnelStatus.STATE_DISCONNECTED);
            generateTunnelStatistics();
        }

        futureTime = (status.restartStamp + (status.restartCount * SLEEP_TIME_MSEC) / 1000);

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

    public void clearTunnelStatus(int tunnelId)
    {
        tunnelStatus.remove(tunnelId);
    }
}
