/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: ClamTest.java,v 1.1 2005/02/26 04:11:31 dmorris Exp $
 */
package com.metavize.tran.clam;

import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;

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
        
        try {
            result = scanner.scanFile(args[0]);
        }
        catch (java.io.IOException e) {
            System.err.println("Error: " + e);
        }
        catch (java.lang.InterruptedException e) {
            System.err.println("Error: " + e);
        }

        System.out.println(result);
        System.exit(0);
    }

}
