/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.hauri;

import com.metavize.tran.virus.VirusScanner;
import com.metavize.tran.virus.VirusScannerResult;

public class HauriTest
{
    public static void main(String args[])
    {
        if (args.length < 1) {
            System.err.println("Usage: java HauriTest <filename>");
            System.exit(1);
        }

        HauriScanner scanner = new HauriScanner();
        VirusScannerResult result = null;
        
        result = scanner.scanFile(args[0]);

        System.out.println(result);
        System.exit(0);
    }

}
