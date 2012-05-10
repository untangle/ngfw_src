/**
 * $Id$
 */
package com.untangle.uvm.engine;

import java.net.InetSocketAddress;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.NetworkManager;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.LoggingSettings;
import com.untangle.uvm.logging.SyslogManager;
import com.untangle.uvm.networking.NetworkConfigurationListener;
import com.untangle.uvm.networking.NetworkConfiguration;

/**
 * Implements SyslogManager.
 */
class SyslogManagerImpl implements SyslogManager
{
    private static final SyslogManagerImpl MANAGER = new SyslogManagerImpl();

    private static final String RSYSLOG = "/etc/init.d/rsyslog";
    private static final File CONF_FILE = new File("/etc/rsyslog.d/untangle-remote.conf");
    private static final String CONF_LINE = ":msg, regex, \"uvm\\[[0-9]*\\]:\" @";

    private final Logger logger = Logger.getLogger(getClass());

    private boolean enabled;

    private SyslogManagerImpl() { }

    // static factories -------------------------------------------------------

    static SyslogManagerImpl manager()
    {
        return MANAGER;
    }

    // SyslogManager methods --------------------------------------------------

    public void sendSyslog( LogEvent e, String tag )
    {
        synchronized (this) {
            if (!enabled)
                return;
        }

        logger.log(org.apache.log4j.Level.INFO, tag + " " + e.toJSONString());
    }

    // package protected methods ----------------------------------------------

    void postInit()
    {
    }

    void reconfigure(LoggingSettings loggingSettings)
     {
        if (loggingSettings != null && loggingSettings.isSyslogEnabled()) {
            this.enabled = true;
            String hostname = loggingSettings.getSyslogHost();
            int port = loggingSettings.getSyslogPort();
            String protocol = loggingSettings.getSyslogProtocol();

            /* int facility = loggingSettings.getSyslogFacility(); unused */
            /* int threshold = loggingSettings.getSyslogThreshold(); unused */
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
            this.enabled = false;
            CONF_FILE.delete();            
        }

        // restart syslog
        UvmContextFactory.context().execManager().exec( RSYSLOG + " " + "restart" );
    }
}
