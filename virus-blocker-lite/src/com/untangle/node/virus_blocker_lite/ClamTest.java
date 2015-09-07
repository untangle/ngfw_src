/**
 * $Id$
 */
package com.untangle.node.virus_blocker_lite;

import java.io.File;

import com.untangle.node.virus.VirusScannerResult;

public class ClamTest
{
    public static void main(String args[])
    {
        if (args.length < 1) {
            System.err.println("Usage: java ClamTest <filename>");
            System.exit(1);
        }

        ClamScanner scanner = new ClamScanner();
        VirusScannerResult result = null;
        
        result = scanner.scanFile(new File(args[0]));
        System.out.println(result);
        System.exit(0);
    }

}
