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

package com.metavize.mvvm.shield;

import com.metavize.mvvm.tran.IPaddr;

public interface ShieldNodeSettings
{
    public boolean isLive();
    
    public IPaddr getAddress();

    public IPaddr getNetmask();

    public float getDivider();
}
