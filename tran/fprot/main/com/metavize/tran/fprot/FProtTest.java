/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FProtTest.java,v 1.1 2005/01/25 04:41:57 dmorris Exp $
 */
package com.metavize.tran.fprot;

import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;

public class FProtTest
{
    public static void main(String args[])
    {
        if (args.length < 1) {
            System.err.println("Usage: java FProtTest <filename>");
            System.exit(1);
        }

        FProtScanner scanner = new FProtScanner();
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
