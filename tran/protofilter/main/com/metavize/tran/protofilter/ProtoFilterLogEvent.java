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

package com.metavize.tran.protofilter;

import com.metavize.mvvm.logging.LogEvent;

/**
 * Log event for a proto filter match.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="TR_PROTOFILTER_EVT"
 * mutable="false"
 */
public class ProtoFilterLogEvent extends LogEvent
{
    private int sessionId;
    private String protocol;
    private boolean blocked;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public ProtoFilterLogEvent() { }

    public ProtoFilterLogEvent(int sessionId, String protocol, boolean blocked)
    {
        this.sessionId = sessionId;
        if (protocol != null && protocol.length() > DEFAULT_STRING_SIZE) protocol = protocol.substring(0, DEFAULT_STRING_SIZE);
        this.protocol = protocol;
        this.blocked = blocked;
    }

    // accessors --------------------------------------------------------------

    /**
     * Session id.
     *
     * @return the session id.
     * @hibernate.property
     * column="SESSION_ID"
     */
    public int getSessionId()
    {
        return sessionId;
    }

    public void setSessionId(int sessionId)
    {
        this.sessionId = sessionId;
    }

    /**
     * The protocol, as determined by the protocol filter.
     *
     * @return the protocol name.
     * @hibernate.property
     * column="PROTOCOL"
     */
    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(String protocol)
    {
        if (protocol != null && protocol.length() > DEFAULT_STRING_SIZE) protocol = protocol.substring(0, DEFAULT_STRING_SIZE);
        this.protocol = protocol;
    }

    /**
     * Whether or not we blocked it.
     *
     * @return whether or not the session was blocked (closed)
     * @hibernate.property
     * column="BLOCKED"
     */
    public boolean isBlocked()
    {
        return blocked;
    }

    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }
}
