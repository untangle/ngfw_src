/**
 * $Id$
 */
package com.untangle.node.virus_blocker_lite;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.StringTokenizer;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.node.virus_blocker.VirusScanner;
import com.untangle.node.virus_blocker.VirusScannerResult;
import com.untangle.node.clam.ClamScannerClientLauncher;
import com.untangle.uvm.vnet.NodeSession;
import org.apache.log4j.Logger;

public class ClamScanner implements VirusScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final int timeout = 29500; /* XXX should be user configurable */

    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty( "uvm.bin.dir" ) + "/virus-blocker-lite-get-last-update";

    private static final String VERSION_ARG = "-V";

    public ClamScanner() {}

    public String getVendorName()
    {
        return "virus_blocker_lite";
    }

    public VirusScannerResult scanFile(File scanfile, NodeSession session)
    {
        ClamScannerClientLauncher scan = new ClamScannerClientLauncher(scanfile);
        return scan.doScan(timeout);
    }
    
    public Date getLastSignatureUpdate()
    {
        try {
            String result = UvmContextFactory.context().execManager().execOutput( GET_LAST_SIGNATURE_UPDATE );
            long timeSeconds = Long.parseLong( result.trim());

            return new Date( timeSeconds * 1000l );
        } catch ( Exception e ) {
            logger.warn( "Unable to get last update.", e );
            return null;
        } 
    }
}
