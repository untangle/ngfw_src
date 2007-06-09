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
package com.untangle.tran.kav;

import java.io.File;

import com.untangle.tran.virus.VirusScanner;
import com.untangle.tran.virus.VirusScannerResult;

public class KavTest
{
    public static void main(String args[])
    {
        if (args.length < 1) {
            System.err.println("Usage: java KavTest <filename>");
            System.exit(1);
        }

        KavScanner scanner = new KavScanner();
        VirusScannerResult result = null;
        
        result = scanner.scanFile(new File(args[0]));

        System.out.println(result);
        System.exit(0);
    }

}
