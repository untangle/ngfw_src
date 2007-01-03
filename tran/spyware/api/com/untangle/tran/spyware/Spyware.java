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

package com.untangle.tran.spyware;

import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.tran.Transform;

public interface Spyware extends Transform
{
    static final int SCAN = Transform.GENERIC_0_COUNTER;
    static final int BLOCK = Transform.GENERIC_1_COUNTER;
    static final int PASS = Transform.GENERIC_2_COUNTER;

    SpywareSettings getSpywareSettings();
    void setSpywareSettings(SpywareSettings settings);

    BlockDetails getBlockDetails(String nonce);
    boolean unblockSite(String nonce, boolean global);

    UserWhitelistMode getUserWhitelistMode();

    EventManager<SpywareEvent> getEventManager();
}
