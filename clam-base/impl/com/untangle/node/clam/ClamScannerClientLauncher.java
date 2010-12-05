/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.clam;

import java.io.File;
import java.lang.Thread;
import com.untangle.uvm.LocalUvmContextFactory;
import com.untangle.node.virus.VirusClientContext;
import com.untangle.node.virus.VirusClientSocket;
import com.untangle.node.virus.VirusScannerClientLauncher;
import com.untangle.node.virus.VirusScannerResult;
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
        if (0 < timeout) {
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
        Thread thread = LocalUvmContextFactory.context().newThread(client);
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
