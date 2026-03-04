/**
 * $Id: VirusBlockerScannerLauncher.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.virus_blocker;

import java.io.File;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.untangle.uvm.vnet.AppSession;
import com.untangle.app.clam.ClamScannerClientLauncher;

/**
 * Launches a virus scanner
 *
 * @author mahotz
 *
 */
public class VirusBlockerScannerLauncher
{
    private final Logger logger = LogManager.getLogger(getClass());

    private final File scanfile;
    private final AppSession session;
    private final boolean cloudScan;
    private final boolean localScan;

    private static final long CLOUD_SCAN_MAX_MILLISECONDS = 2000;
    private VirusScannerResult localResult = null;
    private VirusCloudResult cloudResult = null;

    /**
     * Create a Launcher for the give file
     * 
     * @param scanfile
     *        The file to scan
     * @param session
     *        The application session
     * @param cloudScan
     *        Cloud scan flag
     * @param localScan
     *        Local scan flag
     */
    public VirusBlockerScannerLauncher(File scanfile, AppSession session, boolean cloudScan, boolean localScan)
    {
        this.scanfile = scanfile;
        this.session = session;
        this.cloudScan = cloudScan;
        this.localScan = localScan;
    }

    /**
     * Do the scan
     * 
     * @param timeout
     *        The timeout
     * @return The scan result
     */
    public VirusScannerResult doScan(int timeout)
    {
        VirusBlockerState virusState = (VirusBlockerState) session.attachment();
        VirusCloudScanner cloudScanner = null;
        VirusCloudFeedback feedback = null;

        if (cloudScan && virusState.fileHash != null) {
            cloudScanner = new VirusCloudScanner(virusState);
            cloudScanner.start();
        }
        VirusScannerResult result = VirusScannerResult.CLEAN;
        if (this.localScan) {
            ClamScannerClientLauncher scan = new ClamScannerClientLauncher(scanfile);
            result = scan.doScan(timeout);

            if (result != null) {
                logger.debug("Local Scan result" + result.getVirusName() + result.toString());
            }
        }

        if (cloudScanner != null && virusState.fileHash != null) {
            try {
                synchronized (cloudScanner) {
                    cloudResult = cloudScanner.getCloudResult();
                    // if the result is not known, wait up to CLOUD_SCAN_MAX_MILLISECONDS for a result
                    if (cloudResult == null) {
                        cloudScanner.wait(CLOUD_SCAN_MAX_MILLISECONDS);
                        cloudResult = cloudScanner.getCloudResult();
                    }
                }
            } catch (InterruptedException e) {
                logger.warn("Cloud scanner was interrupted.");
            }
           
            if (this.cloudScan && cloudResult != null && cloudResult.getItemCategory() != null) {
                

                // if clam scan returned positive result we send the feedback
                if (localScan && !result.isClean()) {
                    feedback = new VirusCloudFeedback(virusState, "CLAM", result.getVirusName(), "U", scanfile.length(), session, cloudResult);
                }

                // if no clam scan feedback and cloud returned positive result we also send feedback
                if ((feedback == null) && (cloudResult != null) && (cloudResult.getItemCategory() != null) && (cloudResult.getItemClass() != null) && (cloudResult.getItemConfidence() == 100)) {
                    feedback = new VirusCloudFeedback(virusState, "UT", cloudResult.getItemCategory(), cloudResult.getItemClass(),  scanfile.length(), session, cloudResult);
                    result = new VirusScannerResult(false, cloudResult.getItemCategory());
                }
                
                if (feedback != null) feedback.start();
            }
        }
        return result;
    }
}