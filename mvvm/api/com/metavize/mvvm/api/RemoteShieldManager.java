/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.api;

import java.net.InetAddress;

public interface RemoteShieldManager
{
    /* Dump the current state of the shield every <interval> seconds */
    public void shieldStatus( InetAddress destination, int port, int interval );

    /* Reload the XML configuration file and reconfigure the shield */
    public void shieldReconfigure();
}