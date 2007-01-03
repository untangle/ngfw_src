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

package com.untangle.tran.httpblocker;

import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.tran.Transform;

public interface HttpBlocker extends Transform
{
    HttpBlockerSettings getHttpBlockerSettings();
    void setHttpBlockerSettings(HttpBlockerSettings settings);
    BlockDetails getDetails(String nonce);

    EventManager<HttpBlockerEvent> getEventManager();
}
