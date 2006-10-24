/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */
package com.metavize.tran.firewall;

import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tran.Transform;

public interface Firewall extends Transform
{
    FirewallSettings getFirewallSettings();
    void setFirewallSettings( FirewallSettings settings );

    EventManager<FirewallEvent> getEventManager();
}
