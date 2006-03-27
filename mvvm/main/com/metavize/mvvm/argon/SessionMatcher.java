/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 *  $Id$
 */

package com.metavize.mvvm.argon;

import com.metavize.mvvm.policy.Policy;

public interface SessionMatcher
{
    /**
     * Tells if the session matches */
    boolean isMatch( Policy policy, IPSessionDesc clientSide, IPSessionDesc serverSide );
}
