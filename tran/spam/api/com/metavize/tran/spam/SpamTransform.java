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

package com.metavize.tran.spam;

import com.metavize.mvvm.logging.EventManager;
import com.metavize.mvvm.tran.Transform;

public interface SpamTransform extends Transform
{
    void setSpamSettings(SpamSettings spamSettings);
    SpamSettings getSpamSettings();

    EventManager<SpamEvent> getEventManager();
}
