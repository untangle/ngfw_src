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

package com.untangle.uvm.shield;

import com.untangle.uvm.node.IPaddr;

public interface ShieldNodeSettings
{
    public boolean isLive();
    
    public IPaddr getAddress();

    public IPaddr getNetmask();

    public float getDivider();
}
