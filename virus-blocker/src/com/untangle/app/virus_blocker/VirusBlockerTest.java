/**
 * $Id: VirusBlockerTest.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.virus_blocker;

import java.io.File;

import com.untangle.app.virus_blocker.VirusScannerResult;

/**
 * Manual test for Virus Blocker
 */
public class VirusBlockerTest
{
    /**
     * The main function
     * 
     * @param args
     *        Program arguments
     */
    public static void main(String args[])
    {
        if (args.length < 1) {
            System.err.println("Usage: java VirusBlockerTest <filename>");
            System.exit(1);
        }

        VirusBlockerScanner scanner = new VirusBlockerScanner(null);
        VirusScannerResult result = null;

        // TODO - calc the MD5 sum of the file and pass instead of null
        result = scanner.scanFile(new File(args[0]), null);

        System.out.println(result);
        System.exit(0);
    }
}
