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
package com.untangle.node.clam;

import java.io.File;

import com.untangle.node.virus.VirusScanner;
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
