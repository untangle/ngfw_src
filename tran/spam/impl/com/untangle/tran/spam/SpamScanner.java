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
package com.untangle.tran.spam;

import java.io.File;

public interface SpamScanner
{

    /**
     * Gets the name of the vendor of this transform's spam scanner, used for logging.
     *
     * @return a <code>String</code> giving the name of the vendor of this scanner
     */
    String getVendorName();

    /**
     * Scans the file for Spam, producing a spam report.  Note that the contract for this
     * requires that a report always be generated, for any problems or exceptions an
     * empty report is generated (and the error/warning should be logged).
     *
     * @param file the <code>File</code> containing the complete message to scan for spam
     * @param threshold a <code>float</code> giving the spam cutoff value
     * @return a <code>SpamReport</code> value
     */
    SpamReport scanFile(File file, float threshold);

    /**
     * Returns the number of spam scans that are currently ongoing.
     *
     * @return an <code>int</code> value
     */
    int getActiveScanCount();
}
