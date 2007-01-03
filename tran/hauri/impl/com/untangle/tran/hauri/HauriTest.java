/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.untangle.tran.hauri;

import com.untangle.tran.virus.VirusScanner;
import com.untangle.tran.virus.VirusScannerResult;

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
