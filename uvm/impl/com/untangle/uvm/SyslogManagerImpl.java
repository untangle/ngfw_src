/**
 * $Id$
 */

package com.untangle.uvm;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.json.JSONObject;

import com.untangle.uvm.event.EventSettings;
import com.untangle.uvm.event.SyslogServer;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.logging.LogEvent;

/**
 * Sends events to the syslog server (if enabled)
 */
public class SyslogManagerImpl
{
    private static final Logger logger = LogManager.getLogger(SyslogManagerImpl.class);

    private static final SyslogManagerImpl MANAGER = new SyslogManagerImpl();

    public static final String LOG_TAG = "uvm";
    public static final String LOG_TAG_PREFIX = LOG_TAG + "[0]:";

    private static final File CONF_FILE = new File("/etc/rsyslog.d/untangle-remote.conf");
    private static final String CONF_LINE = "if $msg startswith ' <tag>' then @";

    private static boolean enabled;

    /**
     * Constructor
     */
    private SyslogManagerImpl()
    {
    }

    /**
     * Get the syslog manager
     * 
     * @return The manager
     */
    static SyslogManagerImpl manager()
    {
        return MANAGER;
    }

    /**
     * Send an event to the system log
     * 
     * @param e
     *        The event
     * @param jsonEvent
     *        The event in JSON format.
     */
    public static void sendSyslog(LogEvent e, JSONObject jsonEvent)
    {
        try {
            logger.log(org.apache.logging.log4j.Level.INFO, e.getTag() + " " + jsonEvent);
        } catch (Exception exn) {
            logger.warn("Failed to syslog Event: " + e, exn);
        }

    }

    /**
     * Check for changes to the configuration file
     * 
     * @param settingsFilename
     *        The settings file name
     * @param eventSettings
     *        The event settings
     */
    public static void reconfigureCheck(String settingsFilename, EventSettings eventSettings)
    {
        File settingsFile = new File(settingsFilename);

        if (settingsFile.lastModified() > CONF_FILE.lastModified()) {
            reconfigure(eventSettings);
        }
        setEnabled(eventSettings);
    }

    /**
     * Reconfigure with new settings
     * 
     * @param eventSettings
     *        The new settings
     */
    public static void reconfigure(EventSettings eventSettings)
    {
        if (eventSettings != null && eventSettings.getSyslogServers() != null ) {
            //Delete the CONF_FILE as rsyslog process is restarted at the end of reconfigure method
            CONF_FILE.delete();
            for (SyslogServer sysLogServer: eventSettings.getSyslogServers()) {
                String hostname = sysLogServer.getHost();
                int port = sysLogServer.getPort();
                String protocol = sysLogServer.getProtocol();

                // set rsylsog conf
                String conf = CONF_LINE.replace("<tag>", sysLogServer.getTag());
                if (protocol.equalsIgnoreCase("TCP")) {
                    conf += "@";
                }
                conf += hostname + ":" + port +"\n";
                conf += "& stop" + "\n";

                // write conf file
                BufferedWriter out = null;
                try {
                    out = new BufferedWriter(new FileWriter(CONF_FILE, true));
                    out.write(conf, 0, conf.length());
                } catch (IOException ex) {
                    logger.error("Unable to write file", ex);
                    return;
                } finally {
                    try {
                        if (out != null) {
                            out.close();
                        }
                    } catch (IOException ex) {
                        logger.error("Unable to close file", ex);
                    }
                }

            }

        } else {
            // Remove rsyslog conf
            CONF_FILE.delete();
        }

        // restart syslog
        UvmContextFactory.context().execManager().exec("systemctl restart rsyslog");
    }

    /**
     * Set the enabled flag based on the event settings
     * 
     * @param eventSettings
     *        The event settings
     */
    public static void setEnabled(EventSettings eventSettings)
    {
        enabled = (eventSettings != null && eventSettings.getSyslogEnabled()) ? true : false;
    }
}
