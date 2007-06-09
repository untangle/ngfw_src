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

package com.untangle.node.mail.papi;

import java.nio.ByteBuffer;

import com.untangle.node.token.Token;

public class MIMEMessageTrickleT implements Token
{
    private final MIMEMessageT zMMessageT;

    // constructors -----------------------------------------------------------

    public MIMEMessageTrickleT(MIMEMessageT zMMessageT)
    {
        this.zMMessageT = zMMessageT;
    }

    // static factories -------------------------------------------------------

    // accessors --------------------------------------------------------------

    public MIMEMessageT getMMessageT()
    {
        return zMMessageT;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        return null;
    }

    public int getEstimatedSize()
    {
        return 0;
    }
}
