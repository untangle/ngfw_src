/**
 * $Id: VirusBlockerApp.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.virus_blocker;

import java.io.File;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.untangle.uvm.DaemonManager;
import com.untangle.uvm.ExecManagerResultReader;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.util.Constants;
import com.untangle.uvm.app.AppProperties;
import com.untangle.app.virus_blocker.VirusBlockerBaseApp;

/**
 * Virus Blocker Application
 */
public class VirusBlockerApp extends VirusBlockerBaseApp
{
    private final Logger logger = LogManager.getLogger(VirusBlockerApp.class);
    final String STOP_SOCKET = "systemctl stop clamav-daemon.socket";
    DaemonManager daemonManager = UvmContextFactory.context().daemonManager();

    /**
     * Constructor
     * 
     * @param appSettings
     *        Application settings
     * @param appProperties
     *        Application properties
     */
    public VirusBlockerApp(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);
        //clamav should be always running. covers the scenario where localscan is disabled
        this.fileScannerAvailable = true;
        this.setScanner(new VirusBlockerScanner(this));
    }

    /**
     * Get the HTTP strength
     * 
     * @return The strength
     */
    @Override
    protected int getHttpStrength()
    {
        // virus blocker is 18
        // virus blocker lite is 15
        // virus blocker should be higher (closer to server)
        return 18;
    }

    /**
     * Get the FTP strength
     * 
     * @return The strength
     */
    @Override
    protected int getFtpStrength()
    {
        // virus blocker is 18
        // virus blocker lite is 15
        // virus blocker should be higher (closer to server)
        return 18;
    }

    /**
     * Get the SMTP strength
     * 
     * @return The strength
     */
    @Override
    protected int getSmtpStrength()
    {
        // virus blocker is 15
        // virus blocker lite is 18
        // virus blocker should be lower (closer to client)
        return 15;
    }

    /**
     * Get the name
     * 
     * @return The name
     */
    @Override
    public String getName()
    {
        return "virus_blocker";
    }

    /**
     * Get the application name
     * 
     * @return The application name
     */
    @Override
    public String getAppName()
    {
        return "virus-blocker";
    }

    /**
     * Check the premium flag
     * 
     * @return True if premium, otherwise false
     */
    @Override
    public boolean isPremium()
    {
        return false;
    }

    /**
     * Checks to see if the license is valid
     * 
     * @return True if valid, otherwise false
     */
    @Override
    public boolean isLicenseValid()
    {
        // accept either license "virus-blocker" or "virus-blocker-cloud"
        if (UvmContextFactory.context().licenseManager().isLicenseValid("virus-blocker")) return true;
        if (UvmContextFactory.context().licenseManager().isLicenseValid("virus-blocker-cloud")) return true;
        return false;
    }

    /**
     * Called before the application is started
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void preStart(boolean isPermanentTransition)
    {            
        daemonManager.incrementUsageCount(Constants.CLAMAV_DAEMON);
        daemonManager.incrementUsageCount(Constants.FRESHCLAM);
        daemonManager.enableDaemonMonitoring(Constants.CLAMAV_DAEMON, 300, "clamd");
        daemonManager.enableDaemonMonitoring(Constants.FRESHCLAM, 3600, "freshclam");
        super.preStart(isPermanentTransition);
    }

    /**
     * Called after the application is stopped
     * 
     * @param isPermanentTransition
     *        Permanent transition flag
     */
    @Override
    protected void postStop(boolean isPermanentTransition)
    {
        daemonManager.decrementUsageCount(Constants.CLAMAV_DAEMON);
        int count = daemonManager.getUsageCount(Constants.CLAMAV_DAEMON);
        if(count == 0){
            try {
                ExecManagerResultReader reader = UvmContextFactory.context().execManager().execEvil(STOP_SOCKET);
                reader.waitFor();
                logger.info("Stopped clamav-daemon.socket because usage count reached zero");
            } catch (Exception exn) {
                logger.warn("Failed to run command: " + STOP_SOCKET, exn);
            }
        }
        daemonManager.decrementUsageCount(Constants.FRESHCLAM);
        super.postStop(isPermanentTransition);
    }
}
