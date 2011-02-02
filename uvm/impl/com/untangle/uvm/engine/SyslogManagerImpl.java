/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import org.apache.log4j.Logger;

import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.LoggingSettings;
import com.untangle.uvm.logging.SyslogManager;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.networking.NetworkConfigurationListener;
import com.untangle.uvm.networking.NetworkConfiguration;

/**
 * Implements SyslogManager.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class SyslogManagerImpl implements SyslogManager
{
    private static final SyslogManagerImpl MANAGER = new SyslogManagerImpl();
    private final static long NEXT_PERIOD = (1000l * 60l * 60l * 6l); //6 hrs

    private final ThreadLocal<SyslogSender> syslogSenders;
    private final Logger logger = Logger.getLogger(getClass());

    private long nextTimeMS = 0;

    private DatagramSocket syslogSocket;

    private volatile int facility;
    private volatile SyslogPriority threshold;
    private volatile String hostname;

    private SyslogManagerImpl()
    {
        syslogSenders = new ThreadLocal<SyslogSender>();
    }

    // static factories -------------------------------------------------------

    static SyslogManagerImpl manager()
    {
        return MANAGER;
    }

    // SyslogManager methods --------------------------------------------------

    public void sendSyslog(LogEvent e, String tag)
    {
        synchronized (this) {
            if (null == syslogSocket) {
                return;
            }
        }

        SyslogSender syslogSender = syslogSenders.get();
        if (null == syslogSender) {
            syslogSender = new SyslogSender();
            syslogSenders.set(syslogSender);
        }

        syslogSender.sendSyslog(e, tag);
    }

    // package protected methods ----------------------------------------------

    void postInit()
    {
        final NetworkManager nmi = LocalUvmContextFactory.context().networkManager();

        nmi.registerListener(new NetworkConfigurationListener() {
                public void event(NetworkConfiguration s)
                {
                    hostname = nmi.getHostname().toString();
                }
            });
        
        hostname = nmi.getHostname().toString();
    }

    void reconfigure(LoggingSettings loggingSettings)
    {
        if (!loggingSettings.isSyslogEnabled()) {
            syslogSocket = null;
        }  else {
            String h = loggingSettings.getSyslogHost();
            int p = loggingSettings.getSyslogPort();

            try {
                synchronized (this) {
                    if (null != syslogSocket) {
                        syslogSocket.close();
                    }

                    syslogSocket = new DatagramSocket();
                    syslogSocket.connect(new InetSocketAddress(h, p));

                    syslogSocket.setSendBufferSize(1024);
                    syslogSocket.setTrafficClass(0x02); // IPTOS_LOWCOST
                }
            } catch (SocketException exn) {
                logger.error("could not bind socket", exn);
            }

            facility = loggingSettings.getSyslogFacility().getFacilityValue();
            threshold = loggingSettings.getSyslogThreshold();
        }
    }

    // private classes --------------------------------------------------------

    private class SyslogSender
    {
        private final SyslogBuilderImpl sb = new SyslogBuilderImpl();

        // public methods -----------------------------------------------------

        public void sendSyslog(LogEvent e, String tag)
        {
            synchronized (SyslogManagerImpl.this) {
                if (null != syslogSocket && threshold.inThreshold(e)) {
                    DatagramPacket p = sb.makePacket(e, facility,
                                                     hostname, tag);
                    try {
                        syslogSocket.send(p);
                    } catch (Exception exn) {
                        long curTimeMS = System.currentTimeMillis();

                        if (curTimeMS >= nextTimeMS) {
                            // wait NEXT_PERIOD ms to log exception again
                            nextTimeMS = curTimeMS + NEXT_PERIOD;
                            // log as info instead of warn b/c rbscott sez so
                            logger.info("could not send syslog, host: " + syslogSocket.getInetAddress().getHostName() + ", port: " + syslogSocket.getPort(), exn);
                        }
                    }
                }
            }
        }
    }
}
