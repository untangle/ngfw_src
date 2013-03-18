/**
 * $Id$
 */
package com.untangle.node.clam;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.StringTokenizer;

import com.untangle.uvm.UvmContextFactory;
import com.untangle.node.virus.VirusScanner;
import com.untangle.node.virus.VirusScannerResult;
import org.apache.log4j.Logger;

public class ClamScanner implements VirusScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final int timeout = 29500; /* XXX should be user configurable */

    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty( "uvm.bin.dir" ) + "/clam-get-last-update";

    private static final String VERSION_ARG = "-V";

    public ClamScanner() {}

    public String getVendorName()
    {
        return "clam";
    }

    public VirusScannerResult scanFile(File scanfile)
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
