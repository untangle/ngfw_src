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

package com.untangle.node.token;

import com.untangle.uvm.tapi.TCPSession;

public interface CasingFactory
{
    Casing casing(TCPSession session, boolean clientSide);
}
