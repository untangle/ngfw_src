/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SizeLimitEvent.java,v 1.3 2005/03/25 03:51:16 amread Exp $
 */
package com.metavize.tran.email;

import java.nio.*;
import java.util.*;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.util.*;

/**
 * Log e-mail message size limit (message size exceeds limit) event.
 *
 * @author <a href="mailto:cng@metavize.com">C Ng</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_EMAIL_SZLIMIT_EVT"
 * mutable="false"
 */
public class SizeLimitEvent extends LogEvent
{
    /* constants */

    /* class variables */

    /* instance variables */
    MLMessageInfo zMsgInfo;

    /* constructors */
    public SizeLimitEvent() {}

    public SizeLimitEvent(MLMessageInfo zMsgInfo)
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
