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

package com.untangle.uvm.networking;

import com.untangle.uvm.IntfEnum;

/* Interface for monitoring changes to the Interface Enumeration */
public interface IntfEnumListener
{
    public void event( IntfEnum newEnum );
}
