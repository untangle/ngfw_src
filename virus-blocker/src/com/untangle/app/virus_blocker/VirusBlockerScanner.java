/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import java.io.File;
import java.util.Date;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.app.virus_blocker.VirusScanner;
import com.untangle.app.virus_blocker.VirusScannerResult;
import com.untangle.app.virus_blocker.VirusBlockerState;
import com.untangle.app.virus_blocker.VirusCloudScanner;
import com.untangle.app.virus_blocker.VirusCloudFeedback;
import com.untangle.uvm.vnet.AppSession;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * The Clam Virus Blocker Scanner
 */
public class VirusBlockerScanner implements VirusScanner
{
    private final Logger logger = LogManager.getLogger(getClass());

    private static final int timeout = 29500; /* XXX should be user configurable */

    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty("uvm.bin.dir") + "/virus-blocker-lite-get-last-update";

    private static final String VERSION_ARG = "-V";

    private final VirusBlockerApp app;

    /**
     * Constructor
     * 
     * @param app
     *        The virus blocker lite application
     */
    public VirusBlockerScanner(VirusBlockerApp app)
    {
        this.app = app;
    }

    /**
     * Get the vendor name
     * 
     * @return The vendor name
     */
    public String getVendorName()
    {
        return "virus_blocker_lite";
    }

    /**
     * Scan a file for viruses
     * 
     * @param scanfile
     *        The file to scan
     * @param session
     *        The application session
     * @return The scan result
     */
    public VirusScannerResult scanFile(File scanfile, AppSession session)
    {
        VirusBlockerScannerLauncher scan = new VirusBlockerScannerLauncher(scanfile, session, app.getSettings().getEnableCloudScan(), app.getSettings().getEnableLocalScan());
        return scan.doScan(timeout);
    }

       /**
     * Get the last signature update
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


}
 

