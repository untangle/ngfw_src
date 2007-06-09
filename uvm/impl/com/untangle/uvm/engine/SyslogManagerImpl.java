/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.engine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.LoggingSettings;
import com.untangle.mvvm.logging.SyslogManager;
import com.untangle.mvvm.logging.SyslogPriority;
import com.untangle.mvvm.networking.LocalNetworkManager;
import com.untangle.mvvm.networking.NetworkSettingsListener;
import com.untangle.mvvm.networking.internal.NetworkSpacesInternalSettings;
import org.apache.log4j.Logger;

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
        final LocalNetworkManager nmi = MvvmContextFactory.context().networkManager();

        nmi.registerListener(new NetworkSettingsListener() {
                public void event(NetworkSpacesInternalSettings s)
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
