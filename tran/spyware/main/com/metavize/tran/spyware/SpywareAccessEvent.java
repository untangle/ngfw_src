/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareAccessEvent.java,v 1.3 2005/03/19 23:04:20 amread Exp $
 */

package com.metavize.tran.spyware;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.tran.http.RequestLine;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="TR_SPYWARE_EVT_ACCESS"
 * mutable="false"
 */
public class SpywareAccessEvent extends LogEvent
{
    private int sessionId;
    private String identification;
    private IPMaddr ipMaddr;
    private RequestLine requestLine; // Optional
    private boolean blocked;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareAccessEvent() { }

    public SpywareAccessEvent(int sessionId,
                              String identification,
                              IPMaddr ipMaddr,
                              boolean blocked)
    {
        this.sessionId = sessionId;
        this.identification = identification;
        this.ipMaddr = ipMaddr;
        this.blocked = blocked;
        this.requestLine = null;
    }

    public SpywareAccessEvent(int sessionId,
                              RequestLine requestLine,
                              String identification,
                              IPMaddr ipMaddr,
                              boolean blocked)
    {
        this.sessionId = sessionId;
        this.requestLine = requestLine;
        this.identification = identification;
        this.ipMaddr = ipMaddr;
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
     * Request line for this HTTP response pair.
     *
     * @return the request line.
     * @hibernate.many-to-one
     * column="REQUEST_ID"
     * cascade="none"
     */
    public RequestLine getRequestLine()
    {
        return requestLine;
    }

    public void setRequestLine(RequestLine requestLine)
    {
        this.requestLine = requestLine;
    }

    /**
     * An address or subnet.
     *
     * @return the IPMaddr.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPMaddrUserType"
     * @hibernate.column
     * name="IPMADDR"
     * sql-type="inet"
     */
    public IPMaddr getIpMaddr()
    {
        return ipMaddr;
    }

    public void setIpMaddr(IPMaddr ipMaddr)
    {
        this.ipMaddr = ipMaddr;
    }

    /**
     * The identification (domain matched)
     *
     * @return the protocl name.
     * @hibernate.property
     * column="IDENT"
     */
    public String getIdentification()
    {
        return identification;
    }

    public void setIdentification(String identification)
    {
        this.identification = identification;
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
