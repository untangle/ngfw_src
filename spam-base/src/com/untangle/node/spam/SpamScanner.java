/*
 * $HeadURL: svn://chef/work/src/spam-base/src/com/untangle/node/spam/SpamScanner.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.untangle.node.spam;

import java.io.File;
import java.util.Date;

public interface SpamScanner
{

    /**
     * Gets the name of the vendor of this node's spam scanner, used
     * for logging.
     *
     * @return a <code>String</code> giving the name of the vendor of
     * this scanner.
     */
    String getVendorName();

    /**
     * Scans the file for Spam, producing a spam report.  Note that
     * the contract for this requires that a report always be
     * generated, for any problems or exceptions an empty report is
     * generated (and the error/warning should be logged).
     *
     * @param file the <code>File</code> containing the complete
     * message to scan for spam
     * @param threshold a <code>float</code> giving the spam cutoff
     * value.
     * @return a <code>SpamReport</code> value
     */
    SpamReport scanFile(File file, float threshold);

    /**
     * Returns the number of spam scans that are currently ongoing.
     *
     * @return an <code>int</code> value
     */
    int getActiveScanCount();

    /**
     * Returns the date of the last update
     *
     * @return an <code>Date</code> value
     */
    Date getLastSignatureUpdate();

    /**
     * Returns the date of the last update check
     *
     * @return an <code>Date</code> value
     */
    Date getLastSignatureUpdateCheck();

    /**
     * Returns the version string of the latest signature
     *
     * @return an <code>Date</code> value
     */
    String getSignatureVersion();

}
