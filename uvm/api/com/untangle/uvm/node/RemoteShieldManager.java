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

package com.untangle.mvvm.tran;

import java.net.InetAddress;

public interface RemoteShieldManager
{
    /* Dump the current state of the shield every <interval> seconds */
    public void shieldStatus( InetAddress destination, int port, int interval );

    /* Reload the XML configuration file and reconfigure the shield */
    public void shieldReconfigure();
}
