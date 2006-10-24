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

package com.metavize.mvvm.networking;

import com.metavize.mvvm.IntfEnum;

/* Interface for monitoring changes to the Interface Enumeration */
public interface IntfEnumListener
{
    public void event( IntfEnum newEnum );
}
