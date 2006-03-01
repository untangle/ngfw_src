/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.engine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.LoggingSettings;
import com.metavize.mvvm.logging.SyslogManager;
import com.metavize.mvvm.logging.SyslogPriority;
import com.metavize.mvvm.networking.NetworkManagerImpl;
import com.metavize.mvvm.networking.NetworkSettingsListener;
import com.metavize.mvvm.networking.internal.NetworkSpacesInternalSettings;
import org.apache.log4j.Logger;

class SyslogManagerImpl implements SyslogManager
{
    private static final SyslogManagerImpl MANAGER = new SyslogManagerImpl();

    private final ThreadLocal<SyslogSender> syslogSenders;
    private final Logger logger = Logger.getLogger(getClass());

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

        SyslogSender sb = syslogSenders.get();
        if (null == sb) {
            sb = new SyslogSender();
            syslogSenders.set(sb);
        }

        sb.sendSyslog(e, tag);
    }

    // package protected methods ----------------------------------------------

    void postInit()
    {
        final NetworkManagerImpl nmi = MvvmContextFactory.context().networkManager();

        nmi.registerListener(new NetworkSettingsListener() {
                public void event(NetworkSpacesInternalSettings s)
                {
                    hostname = nmi.getHostname();
                }
            });
        hostname = nmi.getHostname();
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
                                                     hostname,
                                                     tag);
                    try {
                        syslogSocket.send(p);
                    } catch (IOException exn) {
                        logger.warn("could not send syslog", exn);
                    }
                }
            }
        }
    }
}
