/**
 * $Id$
 */
package com.untangle.app.clam;

import java.io.File;
import java.lang.Thread;
import com.untangle.uvm.UvmContextFactory;
import com.untangle.app.virus_blocker.VirusClientContext;
import com.untangle.app.virus_blocker.VirusClientSocket;
import com.untangle.app.virus_blocker.VirusScannerClientLauncher;
import com.untangle.app.virus_blocker.VirusScannerResult;
import org.apache.log4j.Logger;

public class ClamScannerClientLauncher extends VirusScannerClientLauncher
{
    protected final Logger clogger = Logger.getLogger(getClass());

    /**
     * These are "FOUND" viruses by clamdscan, but are not really viruses
     * - copied from the clam source
     */
    private final static String[] invalidVirusNames = {
        "Encrypted.RAR",
        "Oversized.RAR",
        "RAR.ExceededFileSize",
        "RAR.ExceededFilesLimit",
        "Suspect.Zip",
        "Broken.Executable",
        "Exploit.Zip.ModifiedHeaders",
        "Oversized.Zip",
        "Encrypted.Zip",
        "Zip.ExceededFileSize",
        "Zip.ExceededFilesLimit",
        "GZip.ExceededFileSize",
        "BZip.ExceededFileSize",
        "CAB.ExceededFileSize",
        "MSCAB.ExceededFileSize",
        "Archive.ExceededRecursionLimit",
        "PE.UPX.ExceededFileSize"
    };

    private ClamClient client;
    private VirusClientContext cContext;

    /**
     * Create a ClientLauncher for the given file
     */
    public ClamScannerClientLauncher(File msgFile)
    {
        super(msgFile);
    }

    public VirusScannerResult doScan(long timeout)
    {
        ClamClient clientTmp = createClient(); // create scanner

        clientTmp.startScan(); // start scanning

        // wait for result or stop scanning if too much time has passed
        if ( timeout > 0 ) {
            // time remains; let client continue
            clogger.debug("clamc: " + clientTmp + ", wait: " + timeout);
            clientTmp.checkProgress(timeout);
        } else {
            // no time remains; stop client (should never occur)
            clogger.warn("clamc: " + clientTmp + ", stop (timed out)");
            clientTmp.stopScan();
        }

        VirusScannerResult result = getResult(); // get result
        clogger.debug("clamc: " + result);
        return result;
    }

    private ClamClient createClient()
    {
        cContext = new VirusClientContext(msgFile, VirusClientSocket.CLAMD_DEFHOST, VirusClientSocket.CLAMD_DEFPORT);
        client = new ClamClient(cContext);
        Thread thread = UvmContextFactory.context().newThread(client);
        client.setThread(thread);
        return client;
    }

    private VirusScannerResult getResult()
    {
        VirusScannerResult result = cContext.getResult();
        if (null == result) {
            result = VirusScannerResult.ERROR;
        } else if (false == result.isClean()) {
            String virusName = result.getVirusName();
            for (String invalid : invalidVirusNames) {
                if (true == virusName.equalsIgnoreCase(invalid)) {
                    clogger.warn("not virus: " + invalid + ", reset to clean");
                    result = VirusScannerResult.CLEAN;
                }
            }
        }

        return result;
    }
}
