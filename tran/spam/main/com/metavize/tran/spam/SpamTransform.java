/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.spam;

import java.util.List;

import com.metavize.mvvm.tran.Transform;

public interface SpamTransform extends Transform
{
    void setSpamSettings(SpamSettings spamSettings);
    SpamSettings getSpamSettings();

    List<SpamLog> getEventLogs(int limit);
}
