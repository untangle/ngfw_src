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
package com.metavize.tran.spam;

import java.io.IOException;
import java.io.File;

public interface SpamScanner
{

    /**
     * Gets the name of the vendor of this transform's spam scanner, used for logging.
     *
     * @return a <code>String</code> giving the name of the vendor of this scanner
     */
    String getVendorName();

    SpamReport scanFile(File file, float threshold)
        throws IOException, InterruptedException;
}
