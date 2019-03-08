/**
 * $Id$
 */

package com.untangle.app.spam_blocker_lite;

import java.io.File;
import java.util.Date;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.app.spam_blocker.SpamReport;
import com.untangle.app.spam_blocker.SpamScanner;
import org.apache.log4j.Logger;

/**
 * The spam assassin scanner
 */
public class SpamAssassinScanner implements SpamScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final String SPAM_SCANNER_USERNAME = "spamd";

    private static final int timeout = 45000;

    private static int activeScanCount = 0;
    private static Object activeScanMonitor = new Object();

    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty("uvm.bin.dir") + "/spam-blocker-lite-get-last-update";
    private static final String GET_LAST_SIGNATURE_UPDATE_CHECK = System.getProperty("uvm.bin.dir") + "/spam-blocker-lite-get-last-update-check";

    /**
     * Constructor
     */
    public SpamAssassinScanner()
    {
    }

    /**
     * Get the vendor name
     * 
     * @return The vendor name
     */
    public String getVendorName()
    {
        return "SpamBlockerLite";
    }

    /**
     * Get the active scan count
     * 
     * @return The active scan count
     */
    public int getActiveScanCount()
    {
        synchronized (activeScanMonitor) {
            return activeScanCount;
        }
    }

    /**
     * Scan a message file
     * 
     * @param msgFile
     *        The file
     * @param threshold
     *        The spam threshold
     * @return The scan result
     */
    public SpamReport scanFile(File msgFile, float threshold)
    {
        SpamAssassinClient client = new SpamAssassinClient(msgFile, SpamAssassinClient.SPAMD_DEFHOST, SpamAssassinClient.SPAMD_DEFPORT, threshold, SPAM_SCANNER_USERNAME);

        try {
            synchronized (activeScanMonitor) {
                activeScanCount++;
            }
            Thread thread = UvmContextFactory.context().newThread(client);
            client.setThread(thread);
            client.startScan();
            client.checkProgress(SpamAssassinScanner.timeout);
            client.stopScan();

            return client.getResult();
        } finally {
            synchronized (activeScanMonitor) {
                activeScanCount--;
            }
        }
    }

    /**
     * Get the date of the last spam signature update
     * 
     * @return The date of the last spam signature update
     */
    public Date getLastSignatureUpdate()
    {
        try {
            String result = UvmContextFactory.context().execManager().execOutput(GET_LAST_SIGNATURE_UPDATE);
            long timeSeconds = Long.parseLong(result.trim());

            return new Date(timeSeconds * 1000l);
        } catch (Exception e) {
            logger.warn("Unable to get last update.", e);
            return null;
        }
    }

    /**
     * Get the date of the last signature update check
     * 
     * @return The date of the last signature update check
     */
    public Date getLastSignatureUpdateCheck()
    {
        try {
            String result = UvmContextFactory.context().execManager().execOutput(GET_LAST_SIGNATURE_UPDATE_CHECK);
            long timeSeconds = Long.parseLong(result.trim());

            return new Date(timeSeconds * 1000l);
        } catch (Exception e) {
            logger.warn("Unable to get last update check.", e);
            return null;
        }
    }

    /**
     * Get the spam signature version
     * 
     * @return
     */
    public String getSignatureVersion()
    {
        /* This is currently not displayed in the UI or reports */
        return "";
    }
}
