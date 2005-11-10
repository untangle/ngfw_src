/*
 * Copyright (c) 2005 Metavize Inc.
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
import java.util.Formatter;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.logging.LoggingSettings;
import com.metavize.mvvm.logging.SyslogManager;
import com.metavize.tran.util.AsciiCharBuffer;
import org.apache.log4j.Logger;

class SyslogManagerImpl implements SyslogManager
{
    private static final SyslogManagerImpl MANAGER = new SyslogManagerImpl();

    private final ThreadLocal<SyslogSender> syslogSenders;
    private final Logger logger = Logger.getLogger(getClass());

    private DatagramSocket syslogSocket;

    private volatile int facility;

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
        }
    }

    // private classes --------------------------------------------------------

    private class SyslogSender
    {
        private static final String DATE_FORMAT = "%1$tb %1$2te %1$tH:%1$tM:%1$tS";

        private final byte[] buf = new byte[1024];
        private final AsciiCharBuffer sb = AsciiCharBuffer.wrap(buf);
        private final Formatter dateFormatter = new Formatter(sb);

        // public methods -----------------------------------------------------

        public void sendSyslog(LogEvent e, String tag)
        {
            try {
                // 'PRI'
                int v = 8 * facility * e.getSyslogPrioritiy().getPriorityValue();
                sb.append("<");
                sb.append(Integer.toString(v));
                sb.append(">");

                // 'TIMESTAMP'
                dateFormatter.format(DATE_FORMAT, e.getTimeStamp());

                sb.append(' ');

                // 'HOSTNAME'
                sb.append("mv-edgeguard"); // XXX use legit hostname

                sb.append(' ');

                // 'TAG[pid]: '
                sb.append(tag);

                // CONTENT
                sb.append(e.getSyslogId());

                sb.append(" # ");

                e.appendSyslog(sb);
            } catch (IOException exn) {
                logger.warn("could not fully append", exn);
            }

            DatagramPacket p = new DatagramPacket(buf, 0, sb.position());

            synchronized (SyslogManagerImpl.this) {
                if (null != syslogSocket) {
                    try {
                        syslogSocket.send(p);
                    } catch (IOException exn) {
                        logger.warn("could not send syslog", exn);
                    } catch (Exception exn) {
                        exn.printStackTrace();
                    }
                }
            }

            sb.clear();
        }
    }
}
