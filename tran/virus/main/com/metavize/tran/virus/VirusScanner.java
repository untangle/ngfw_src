/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: VirusScanner.java,v 1.2 2005/01/18 05:44:04 amread Exp $
 */
package com.metavize.tran.virus;

import java.io.IOException;
import java.util.List;

public interface VirusScanner
{
    VirusScannerResult scanFile (String fileName)
        throws IOException, InterruptedException;
    VirusScannerResult scanBufs (List bufs)
        throws IOException, InterruptedException;
}
