/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.mail.papi;

import java.nio.ByteBuffer;

import com.metavize.tran.token.Token;

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
}
