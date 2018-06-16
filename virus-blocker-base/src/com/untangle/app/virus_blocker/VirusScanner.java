/**
 * $Id$
 */

package com.untangle.app.virus_blocker;

import java.io.File;
import java.util.Date;
import com.untangle.uvm.vnet.AppSession;

import com.untangle.uvm.app.Scanner;

/**
 * Virus scanner
 */
public interface VirusScanner extends Scanner
{
    /**
     * 
     * Scans the file for viruses, producing a virus report. Note that the
     * contract for this requires that a report always be generated, for any
     * problems or exceptions an "clean" report is generated (and the
     * error/warning should be logged).
     * 
     * @param msgFile
     *        The file to scan
     * @param session
     *        The application session
     * @return The scan result
     */
    VirusScannerResult scanFile(File msgFile, AppSession session);

    /**
     * Get the date of the last virus signature update
     * 
     * @return The date of the last virus signature update
     */
    Date getLastSignatureUpdate();
}
