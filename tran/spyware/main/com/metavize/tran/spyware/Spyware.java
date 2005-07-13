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

package com.metavize.tran.spyware;

import java.util.List;

import com.metavize.mvvm.tran.Transform;

public interface Spyware extends Transform
{
    static final int BLOCK = Transform.GENERIC_0_COUNTER;
    static final int ADDRESS = Transform.GENERIC_1_COUNTER;
    static final int ACTIVE_X = Transform.GENERIC_2_COUNTER;
    static final int COOKIE = Transform.GENERIC_3_COUNTER;

    SpywareSettings getSpywareSettings();
    void setSpywareSettings(SpywareSettings settings);

    List<SpywareActiveXLog> getActiveXLogs(int limit);
    List<SpywareCookieLog> getCookieLogs(int limit);
}
