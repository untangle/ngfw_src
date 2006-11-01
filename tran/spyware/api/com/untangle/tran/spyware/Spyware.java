/*
 * Copyright (c) 2003-2006 Untangle Networks, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle Networks, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.spyware;

import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.tran.Transform;

public interface Spyware extends Transform
{
    static final int BLOCK = Transform.GENERIC_0_COUNTER;
    static final int ADDRESS = Transform.GENERIC_1_COUNTER;
    static final int ACTIVE_X = Transform.GENERIC_2_COUNTER;
    static final int COOKIE = Transform.GENERIC_3_COUNTER;

    SpywareSettings getSpywareSettings();
    void setSpywareSettings(SpywareSettings settings);

    BlockDetails getBlockDetails(String nonce);
    boolean unblockSite(String nonce, boolean global);

    EventManager<SpywareEvent> getEventManager();
}
