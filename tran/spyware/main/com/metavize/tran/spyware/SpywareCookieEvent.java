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
public class SpywareCookieEvent extends SpywareEvent
{
    // persisted fields -------------------------------------------------------
    private int sessionId;
    private String identification;
    private RequestLine requestLine;
    private boolean toServer;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareCookieEvent() { }

    public SpywareCookieEvent(IPSession session,
                              RequestLine requestLine,
                              String identification,
                              boolean toServer)
    {
        super(session);

        this.session = session;
        this.sessionId = session.getSessionId();
        this.identification = identification;
        this.requestLine = requestLine;
        this.toServer = toServer;
    }

    // Hibernate accessors ----------------------------------------------------

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
     * @return the protocol name.
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
