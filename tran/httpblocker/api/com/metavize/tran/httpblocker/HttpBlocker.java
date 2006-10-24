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

package com.metavize.tran.httpblocker;

import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tran.Transform;

public interface HttpBlocker extends Transform
{
    HttpBlockerSettings getHttpBlockerSettings();
    void setHttpBlockerSettings(HttpBlockerSettings settings);
    BlockDetails getDetails(String nonce);

    EventManager<HttpBlockerEvent> getEventManager();
}
