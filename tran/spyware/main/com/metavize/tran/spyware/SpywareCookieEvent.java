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

package com.metavize.tran.spyware;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.http.RequestLine;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="TR_SPYWARE_EVT_COOKIE"
 * mutable="false"
 */
public class SpywareCookieEvent extends LogEvent
{
    private int sessionId;
    private String identification;
    private RequestLine requestLine;
    private boolean toServer;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareCookieEvent() { }

    public SpywareCookieEvent(int sessionId,
                              RequestLine requestLine,
                              String identification,
                              boolean toServer)
    {
        this.sessionId = sessionId;
        this.identification = identification;
        this.requestLine = requestLine;
        this.toServer = toServer;
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
     * cascade="save-update"
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
     * The identification (name of IP address range matched)
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
     * Whether or not the cookie is to the server (otherwise to the client)
     *
     * @return if true the cookie was zeroed going to the server,
     * otherwise it was removed going to the client
     * @hibernate.property
     * column="TO_SERVER"
     */
    public boolean isToServer()
    {
        return toServer;
    }

    public void setToServer(boolean toServer)
    {
        this.toServer = toServer;
    }
}
