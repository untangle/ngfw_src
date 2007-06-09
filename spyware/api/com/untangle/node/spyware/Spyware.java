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

package com.untangle.node.spyware;

import com.untangle.uvm.logging.EventManager;
import com.untangle.uvm.node.Node;
import com.untangle.node.http.UserWhitelistMode;

public interface Spyware extends Node
{
    static final int SCAN = Node.GENERIC_0_COUNTER;
    static final int BLOCK = Node.GENERIC_1_COUNTER;
    static final int PASS = Node.GENERIC_2_COUNTER;

    SpywareSettings getSpywareSettings();
    void setSpywareSettings(SpywareSettings settings);

    SpywareBlockDetails getBlockDetails(String nonce);
    boolean unblockSite(String nonce, boolean global);

    UserWhitelistMode getUserWhitelistMode();

    EventManager<SpywareEvent> getEventManager();
}
