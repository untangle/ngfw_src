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

package com.metavize.mvvm.localapi;

import com.metavize.mvvm.api.IPSessionDesc;

import com.metavize.mvvm.policy.Policy;

public interface SessionMatcher
{
    /**
     * Tells if the session matches */
    boolean isMatch( Policy policy, IPSessionDesc clientSide, IPSessionDesc serverSide );
}
