/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: SpywareActiveXEvent.java,v 1.4 2005/03/19 23:04:20 amread Exp $
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
 * table="TR_SPYWARE_EVT_ACTIVEX"
 * mutable="false"
 */
public class SpywareActiveXEvent extends LogEvent
{
    private int sessionId;
    private String identification;
    private RequestLine requestLine;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareActiveXEvent() { }

    public SpywareActiveXEvent(int sessionId,
                               RequestLine requestLine,
                               String identification)
    {
        this.sessionId = sessionId;
        this.identification = identification;
        this.requestLine = requestLine;
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
     * The identification (ActiveX class ID matched)
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
}
