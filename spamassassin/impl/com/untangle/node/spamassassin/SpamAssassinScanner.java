/*
 * $HeadURL$
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

package com.untangle.node.spamassassin;

import com.untangle.uvm.node.NodeException;
import com.untangle.uvm.node.script.ScriptRunner;

import com.untangle.node.spam.SpamScanner;
import com.untangle.node.spam.SpamReport;

import java.io.File;
import java.util.Date;

import org.apache.log4j.Logger;

public class SpamAssassinScanner implements SpamScanner
{
    private final Logger logger = Logger.getLogger(getClass());

    private static final int timeout = 40000; /* XXX should be user configurable */

    private static int activeScanCount = 0;
    private static Object activeScanMonitor = new Object();

    private static final String GET_LAST_SIGNATURE_UPDATE = System.getProperty( "bunnicula.bin.dir" ) + "/spamassassin-get-last-update";

    public SpamAssassinScanner() { }

    public String getVendorName()
    {
        return "SpamAssassin";
    }

    public int getActiveScanCount()
    {
        synchronized(activeScanMonitor) {
            return activeScanCount;
        }
    }

    public SpamReport scanFile(File msgFile, float threshold)
    {
        SpamAssassinScannerClientLauncher scan = new SpamAssassinScannerClientLauncher(msgFile, threshold);
        try {
            synchronized(activeScanMonitor) {
                activeScanCount++;
            }
            return scan.doScan(this.timeout);
        } finally {
            synchronized(activeScanMonitor) {
                activeScanCount--;
            }
        }
    }

    public Date getLastSignatureUpdate()
    {
        try {
            String result = ScriptRunner.getInstance().exec( GET_LAST_SIGNATURE_UPDATE );
            long timeSeconds = Long.parseLong( result.trim());

            return new Date( timeSeconds * 1000l );
        } catch ( NodeException e ) {
            logger.warn( "Unable to get last update.", e );
            return null;
        } catch ( NumberFormatException e ) {
            logger.warn( "Unable to get last update.", e );
            return null;
        }
    }

    public String getSignatureVersion()
    {
        /* This is currently not displayed in the UI or reports */
        return "";
    }

}
