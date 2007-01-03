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

package com.untangle.mvvm.tran.firewall.protocol;

import com.untangle.mvvm.tapi.Protocol;

public interface ProtocolMatcher
{
    public boolean isMatch( Protocol protocol );

    // This version useful for applying to IPSessionDesc
    public boolean isMatch( short protocol );

    public String toDatabaseString();
}
