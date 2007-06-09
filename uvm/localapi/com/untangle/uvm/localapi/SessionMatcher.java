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

package com.untangle.uvm.localapi;

import com.untangle.uvm.node.IPSessionDesc;

import com.untangle.uvm.policy.Policy;

public interface SessionMatcher
{
    /**
     * Tells if the session matches */
    boolean isMatch( Policy policy, IPSessionDesc clientSide, IPSessionDesc serverSide );
}
