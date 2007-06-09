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
package com.untangle.node.virus;

import java.io.File;

import org.apache.log4j.Logger;

abstract public class VirusScannerClientLauncher {
    protected File msgFile;

    /**
     * Create a ClientLauncher for the given file
     */
    public VirusScannerClientLauncher(File msgFile) {
        this.msgFile = msgFile;
    }

    abstract public VirusScannerResult doScan(long timeout);
}
