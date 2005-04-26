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
package com.metavize.tran.email;

import java.nio.*;
import java.util.*;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.util.*;

/**
 * Log e-mail message size relay (message exceeds relay size) event.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_SZRELAY_EVT"
 * mutable="false"
 */
public class SizeRelayEvent extends LogEvent
{
    /* constants */

    /* class variables */

    /* instance variables */
    MLMessageInfo zMsgInfo;

    /* constructors */
    public SizeRelayEvent() {}

    public SizeRelayEvent(MLMessageInfo zMsgInfo)
    {
        this.zMsgInfo = zMsgInfo;
    }

    /* public methods */
    /**
     * Associate e-mail message info with event.
     *
     * @return e-mail message info.
     * @hibernate.many-to-one
     * column="MSG_ID"
     * cascade="all"
     */
    public MLMessageInfo getMessageInfo()
    {
        return zMsgInfo;
    }

    public void setMessageInfo(MLMessageInfo zMsgInfo)
    {
        this.zMsgInfo = zMsgInfo;
        return;
    }

    /* private methods */
}
