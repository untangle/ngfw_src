/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.firewall;

import java.util.List;

import com.metavize.mvvm.tran.Transform;

public interface Firewall extends Transform
{
    FirewallSettings getFirewallSettings();
    void setFirewallSettings( FirewallSettings settings );

    List<FirewallLog> getEventLogs(int limit);

    List<FirewallLog> getEventLogs(int limit, boolean blockedOnly);
}
