/**
 * $Id$
 */

package com.untangle.app.virus_blocker_lite;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.uvm.app.AppSettings;
import com.untangle.uvm.app.AppProperties;
import com.untangle.app.virus_blocker.VirusBlockerBaseApp;

/**
 * The Virus Blocker Lite application
 */
public class VirusBlockerLiteApp extends VirusBlockerBaseApp
{
    /**
     * Constructor
     * 
     * @param appSettings
     *        The application settings
     * @param appProperties
     *        The application properties
     */
    public VirusBlockerLiteApp(AppSettings appSettings, AppProperties appProperties)
    {
        super(appSettings, appProperties);
        this.setScanner(new ClamScanner(this));
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
        return 15;
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
        return 15;
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
        return 18;
    }

    /**
     * Get the name
     * 
     * @return The name
     */
    @Override
    public String getName()
    {
        return "virus_blocker_lite";
    }

    /**
     * Get the application name
     * 
     * @return The application name
     */
    @Override
    public String getAppName()
    {
        return "virus-blocker-lite";
    }

    /**
     * Get the premium flag
     * 
     * @return True if premium, otherwise false
     */
    @Override
    public boolean isPremium()
    {
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
        UvmContextFactory.context().daemonManager().incrementUsageCount("clamav-daemon");
        UvmContextFactory.context().daemonManager().incrementUsageCount("clamav-freshclam");
        UvmContextFactory.context().daemonManager().enableDaemonMonitoring("clamav-daemon", 300, "clamd");
        UvmContextFactory.context().daemonManager().enableDaemonMonitoring("clamav-freshclam", 3600, "freshclam");
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
        UvmContextFactory.context().daemonManager().decrementUsageCount("clamav-daemon");
        UvmContextFactory.context().daemonManager().decrementUsageCount("clamav-freshclam");
        super.postStop(isPermanentTransition);
    }
}
