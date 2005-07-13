/*
 * Copyright (c) 2003, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.virus;

import java.io.IOException;
import java.util.List;

public interface VirusScanner
{

    /**
     * Gets the name of the vendor of this transform's virus scanner, used for logging.
     *
     * @return a <code>String</code> giving the name of the vendor of this scanner
     */
    String getVendorName();

    VirusScannerResult scanFile (String fileName)
        throws IOException, InterruptedException;
    VirusScannerResult scanBufs (List bufs)
        throws IOException, InterruptedException;
}
