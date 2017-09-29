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
    private static final String STATE_CONNECTED = "CONNECTED";

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

        long now = System.currentTimeMillis();

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

            /* grab the status and traffic statistics for each tunnel */
            grabTunnelStatistics();
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

            logger.debug("Checking tunnel " + tunnel.getName() + " (" + tunnel.getTunnelId() + ")");

            try {
                File pidFile = new File("/var/run/tunnelvpn/tunnel-" + tunnel.getTunnelId() + ".pid");
                if (!pidFile.exists()) {
                    /* process does not exist so we start it here */
                    logger.warn("OpenVPN process for " + tunnel.getName() + " (" + tunnel.getTunnelId() + ") missing. Restarting...");
                    manager.launchProcess(tunnel);
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
                    /* process isn't running so we restart it here */
                    logger.warn("OpenVPN process for " + tunnel.getName() + " (PID:" + pid + ") died. Restarting...");
                    manager.launchProcess(tunnel);
                    continue;

                }

            } catch (Exception exn) {
                logger.warn("Failed to check openvpn pid file.", exn);
            }
        }
    }

    protected void grabTunnelStatistics()
    {
        TunnelVpnTunnelStatus status;
        TunnelVpnEvent event;

        try {
            for (TunnelVpnTunnelSettings tunnel : app.getSettings().getTunnels()) {
                if (!tunnel.getEnabled()) continue;

                status = updateTunnelStatus(tunnel);

                if (status.stateInfo.equals(status.stateLast)) continue;
                
                if (status.stateInfo.equals(STATE_CONNECTED)) {
                    event = new TunnelVpnEvent(status.serverAddress,status.localAddress,tunnel.getName(),TunnelVpnEvent.EventType.CONNECT);
                } else {
                    event = new TunnelVpnEvent(status.serverAddress,status.localAddress,tunnel.getName(),TunnelVpnEvent.EventType.DISCONNECT);
                }
                app.logEvent(event);
                logger.debug("TunnelVpnEvent(logEvent) " + event.toString());
                status.stateLast = status.stateInfo;
            }

        } catch (Exception exn) {
            logger.warn("Failed to get tunnel statistics.", exn);
        }
    }

    public TunnelVpnTunnelStatus getTunnelStatus(int tunnelId)
    {
        grabTunnelStatistics();
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
            status = new TunnelVpnTunnelStatus();
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
            writeCommand(out, STATE_CMD);

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

                status.connectStamp = (Long.valueOf(array[0]) * 1000);
                status.stateInfo = array[1];
                status.localAddress = InetAddress.getByName(array[3]);
                status.serverAddress = InetAddress.getByName(array[4]);
            }

            /* Now get the status */
            writeCommand(out, STATUS_CMD);

            while (true) {
                String line = in.readLine().trim();
                logger.debug("STATUS: " + line);
                if (line.equalsIgnoreCase(END_MARKER)) break;

                String array[] = line.split(",");
                if (array.length != 2) continue;

                if (array[0].equalsIgnoreCase(XMIT_MARKER)) {
                    status.xmitTotal = Long.valueOf(array[1]);
                }

                if (array[0].equalsIgnoreCase(RECV_MARKER)) {
                    status.recvTotal = Long.valueOf(array[1]);
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
        if ((status.xmitLast == status.xmitTotal) && (status.recvLast == status.recvTotal)) return (status);

        /*
         * The stats for each the tunnel will be cleared if the connection dies
         * and we restart openvpn, so we look for values less than we saw on the
         * previous check and handle accordingly. Once we calculate the number
         * of XMIT and RECV bytes since the last check, we update the last
         * values and write the stat record to the database.
         */
        if (status.xmitTotal < status.xmitLast) xmitBytes = status.xmitTotal;
        else xmitBytes = (status.xmitTotal - status.xmitLast);

        if (status.recvTotal < status.recvLast) recvBytes = status.recvTotal;
        else recvBytes = (status.recvTotal - status.recvLast);

        status.xmitLast = status.xmitTotal;
        status.recvLast = status.recvTotal;

        /*
         * don't bother logging an event if there was no traffic
         */
        if ((xmitBytes == 0) && (recvBytes == 0)) return (status);

        TunnelVpnStatusEvent event = new TunnelVpnStatusEvent(tunnel.getName(), recvBytes, xmitBytes);
        app.logEvent(event);
        logger.debug("updateTunnelStatus(logEvent) " + event.toString());

        return (status);
    }

    private void writeCommand(BufferedWriter out, String command) throws IOException
    {
        out.write(command + "\n");
        out.flush();
    }

}
