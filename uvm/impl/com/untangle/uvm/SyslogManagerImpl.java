/**
 * $Id$
 */
package com.untangle.uvm;

import java.net.InetSocketAddress;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;

/**
 * Sends events to the syslog server (if enabled)
 */
public class SyslogManagerImpl
{
    private static final Logger logger = Logger.getLogger(SyslogManagerImpl.class);

    private static final SyslogManagerImpl MANAGER = new SyslogManagerImpl();

    public static final String LOG_TAG = "uvm";
    public static final String LOG_TAG_PREFIX = LOG_TAG + "[0]: ";

    private static final String RSYSLOG = "/etc/init.d/rsyslog";
    private static final File CONF_FILE = new File("/etc/rsyslog.d/untangle-remote.conf");
    private static final String CONF_LINE = ":msg, startswith, \" " + LOG_TAG + "\\[\" @";

    private static boolean enabled;

    private SyslogManagerImpl() { }

    static SyslogManagerImpl manager()
    {
        return MANAGER;
    }

    public static void sendSyslog( LogEvent e )
    {
        if (!enabled){
            return;
        }

        try {
            logger.log(org.apache.log4j.Level.INFO, e.getTag() + " " + e.toJSONString());
        } catch (Exception exn) {
            logger.warn("Failed to syslog Event: " + e, exn);
        }

    }

    public static void reconfigureCheck(String settingsFilename, EventSettings eventSettings)
    {
        File settingsFile = new File(settingsFilename);

        if (settingsFile.lastModified() > CONF_FILE.lastModified()){
            reconfigure(eventSettings);
        }
        setEnabled(eventSettings);

    }

    public static void reconfigure(EventSettings eventSettings)
    {
        if (eventSettings != null && eventSettings.getSyslogEnabled()) {
            enabled = true;
            String hostname = eventSettings.getSyslogHost();
            int port = eventSettings.getSyslogPort();
            String protocol = eventSettings.getSyslogProtocol();

            // set rsylsog conf
            String conf = CONF_LINE;
            if (protocol.equalsIgnoreCase("TCP"))
            {
                conf += "@";
            }
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
            // Remove rsyslog conf
            enabled = false;
            CONF_FILE.delete();
        }

        // restart syslog
        File pidFile = new File("/var/run/rsyslogd.pid");
        if (pidFile.exists())
            UvmContextFactory.context().execManager().exec( RSYSLOG + " " + "restart" );
    }
    
    public static void setEnabled(EventSettings eventSettings)
    {
        enabled = 
            ( eventSettings != null && eventSettings.getSyslogEnabled()) 
            ? true : false;
    }
}
