/*
 * Copyright (c) 2004, 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.api;

import com.metavize.mvvm.IntfEnum;

public interface RemoteIntfManager
{
    /* Retrieve the current interface enumeration */
    public IntfEnum getIntfEnum();
}