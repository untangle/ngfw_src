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

package com.untangle.tran.spam;

import com.untangle.mvvm.logging.EventManager;
import com.untangle.mvvm.tran.Transform;

public interface SpamTransform extends Transform
{
    void setSpamSettings(SpamSettings spamSettings);
    SpamSettings getSpamSettings();

    EventManager<SpamEvent> getEventManager();
}
