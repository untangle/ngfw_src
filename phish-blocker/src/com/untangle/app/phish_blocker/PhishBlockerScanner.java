/**
 * $Id$
 */

package com.untangle.app.phish_blocker;

import java.io.File;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.app.clam.ClamScannerClientLauncher;
import com.untangle.app.spam_blocker.ReportItem;
import com.untangle.app.spam_blocker.SpamReport;
import com.untangle.app.spam_blocker.SpamScanner;
import com.untangle.app.virus_blocker.VirusScannerResult;
import org.apache.log4j.Logger;

/**
 * Implementation of a scanner for phishing emails
 */
public class PhishBlockerScanner implements SpamScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final float HIT_SCORE = 100f;
    private static final int TIMEOUT = 10000;

    private static int activeScanCount = 0;
    private static Object activeScanMonitor = new Object();

    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty("uvm.bin.dir") + "/phish-blocker-get-last-update";
    private static final String GET_LAST_SIGNATURE_UPDATE_CHECK = System.getProperty("uvm.bin.dir") + "/phish-blocker-get-last-update-check";

    /**
     * Constructor
     */
    public PhishBlockerScanner()
    {
    }

    /**
     * Get the vendor name
     * 
     * @return The vendor name
     */
    public String getVendorName()
    {
        return "PhishBlocker"; // also referenced in SpamSpamFilter
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
     * Generate a report on a message file
     * 
     * @param msgFile
     *        The message file
     * @param threshold
     *        The detection threshold
     * @return The report
     */
    public SpamReport scanFile(File msgFile, float threshold)
    {
        ClamScannerClientLauncher scan = new ClamScannerClientLauncher(msgFile);
        try {
            synchronized (activeScanMonitor) {
                activeScanCount++;
            }
            VirusScannerResult vsr = scan.doScan(PhishBlockerScanner.TIMEOUT);
            SpamReport result;
            if (vsr.isClean() || vsr.getVirusName() == null || !vsr.getVirusName().contains("Phish")) {
                result = SpamReport.EMPTY;
            } else {
                // convert VirusScannerResult to phish SpamReport
                ReportItem ourItem = new ReportItem(HIT_SCORE + threshold, vsr.getVirusName());
                List<ReportItem> items = new LinkedList<>();
                items.add(ourItem);
                result = new SpamReport(items, threshold);
            }
            logger.debug("phishc: " + result);
            return result;
        } finally {
            synchronized (activeScanMonitor) {
                activeScanCount--;
            }
        }
    }

    /**
     * Get the date of the last signature update
     * 
     * @return The date of the last signature update
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
     * Get the date of the last signature check
     * 
     * @return The date of the last signature check
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
     * Get the signature version
     * 
     * @return The signature version
     */
    public String getSignatureVersion()
    {
        /* This is currently not displayed in the UI or reports */
        return "";
    }
}
