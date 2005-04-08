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

import com.metavize.mvvm.tran.Transform;

public interface Spyware extends Transform
{
    public static final int BLOCK = Transform.GENERIC_0_COUNTER;
    public static final int ADDRESS = Transform.GENERIC_1_COUNTER;
    public static final int ACTIVE_X = Transform.GENERIC_2_COUNTER;
    public static final int COOKIE = Transform.GENERIC_3_COUNTER;

    public SpywareSettings getSpywareSettings();

    public void setSpywareSettings(SpywareSettings settings);
}
