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
 * The virus blocker scanner launcher
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
     * Constructor
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

        if (cloudScan && virusState.fileHash != null) {
            cloudScanner = new VirusCloudScanner(virusState);
            cloudScanner.start();
        }

        if (localScan) {
            ClamScannerClientLauncher scan = new ClamScannerClientLauncher(scanfile);
            localResult = scan.doScan(timeout);

            if (localResult != null) {
                logger.debug("Local Scan result" + localResult.getVirusName() + localResult.toString());
            }
            return localResult;
        }

        if (cloudScanner != null && virusState.fileHash != null) {
            try {
                synchronized (cloudScanner) {
                    cloudScanner.wait(CLOUD_SCAN_MAX_MILLISECONDS);
                }
            } catch (InterruptedException e) {
                logger.warn("Cloud scanner was interrupted.");
            }
            cloudResult = cloudScanner.getCloudResult();
            if (cloudResult != null && cloudResult.getItemCategory() != null) {
                VirusScannerResult result = new VirusScannerResult(false, cloudResult.getItemCategory());
                
                if (localScan) {
                    VirusCloudFeedback feedback = new VirusCloudFeedback(virusState, "CLAM", localResult.getVirusName(), "U", scanfile.length(), session, cloudResult);
                    feedback.start();
                }
                return result;
            }
        }
        return VirusScannerResult.CLEAN;
    }
}