/**
 * $Id$
 */
package com.untangle.node.reports;

import java.net.InetSocketAddress;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;

/**
 * Sends events to the syslog server (if enabled)
 */
public class SyslogManagerImpl
{
    private static final SyslogManagerImpl MANAGER = new SyslogManagerImpl();

    private static final String RSYSLOG = "/etc/init.d/rsyslog";
    private static final File CONF_FILE = new File("/etc/rsyslog.d/untangle-remote.conf");
    private static final String CONF_LINE = ":msg, regex, \"uvm\\[[0-9]*\\]:\" @";

    private static final Logger logger = Logger.getLogger(SyslogManagerImpl.class);

    private static boolean enabled;

    private SyslogManagerImpl() { }

    static SyslogManagerImpl manager()
    {
        return MANAGER;
    }

    public static void sendSyslog( LogEvent e, String tag )
    {
        if (!enabled)
            return;

        try {
            logger.log(org.apache.log4j.Level.INFO, tag + " " + e.toJSONString());
        } catch (Exception exn) {
            logger.warn("Failed to syslog Event: " + e, exn);
        }

    }

    public static void reconfigure(ReportsSettings reportsSettings)
    {
        if (reportsSettings != null && reportsSettings.getSyslogEnabled()) {
            enabled = true;
            String hostname = reportsSettings.getSyslogHost();
            int port = reportsSettings.getSyslogPort();
            String protocol = reportsSettings.getSyslogProtocol();

            /* int facility = reportsSettings.getSyslogFacility(); unused */
            /* int threshold = reportsSettings.getSyslogThreshold(); unused */
            // SyslogAppender sa = (SyslogAppender)logger.getAppender("EVENTS");
            // sa.setFacility("LOCAL" + facility);
            // sa.setThreshold(threshold);

            // set rsylsog conf
            String conf = CONF_LINE;
            if (protocol.equalsIgnoreCase("TCP"))
                conf += "@";
            conf += hostname + ":" + port;

            // write conf file
            BufferedWriter out = null;
            try {
                out = new BufferedWriter(new FileWriter(CONF_FILE));
                out.write(conf, 0, conf.length());
            } catch (IOException ex) {
                logger.error("Unable to write file", ex);
                return;
            }
            try {
                out.close();
            } catch (IOException ex) {
                logger.error("Unable to close file", ex);
                return;
            }
        } else {
            enabled = false;
            CONF_FILE.delete();
        }

        // restart syslog
        File pidFile = new File("/var/run/rsyslogd.pid");
        if (pidFile.exists())
            UvmContextFactory.context().execManager().exec( RSYSLOG + " " + "restart" );
    }
    
    public static void setEnabled(ReportsSettings reportsSettings)
    {
        enabled = 
            ( reportsSettings != null && reportsSettings.getSyslogEnabled()) 
            ? true : false;
    }
}
