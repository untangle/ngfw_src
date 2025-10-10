/**
 * $Id: VirusBlockerApp.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.virus_blocker;

import java.io.File;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.app.virus_blocker.VirusBlockerBaseApp;

/**
 * Virus Blocker Application
 */
public class VirusBlockerApp extends VirusBlockerBaseApp
{
    private final Logger logger = LogManager.getLogger(VirusBlockerApp.class);
    private static final String CLAM_DAEMON_START = System.getProperty("uvm.bin.dir") + "/clamav-daemon-start start";
    private static final String CLAM_DAEMON_STOP = System.getProperty("uvm.bin.dir") + "/clamav-daemon-start stop";

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
        
        try {
            // ClamAV is dependant on daily.cvd and main.cvd files, those need to be downloaded first before starting the clamav daemon
            // updates and downloads is taken care by separate daemon fresh-clam. 
            UvmContextFactory.context().execManager().execEvilProcess(CLAM_DAEMON_START);

        } catch (Exception e) {
            logger.warn("Error while starting ClamAV Daemons", e);
        }
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
        try {
            // use the same script of preStart method for stopping ClamAV related daemon services
            UvmContextFactory.context().execManager().execEvilProcess(CLAM_DAEMON_STOP);

        } catch (Exception e) {
            logger.warn("Error while stopping ClamAV Daemons", e);
        }
        super.postStop(isPermanentTransition);
    }
}
