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

package com.untangle.uvm.argon;

import com.untangle.jnetcap.*;

public class TCPSessionImpl extends IPSessionImpl implements TCPSession 
{
    public TCPSessionImpl( TCPNewSessionRequest request )
    {
        super( request );
    }
}
