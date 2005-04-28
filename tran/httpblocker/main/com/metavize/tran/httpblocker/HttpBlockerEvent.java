/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.httpblocker;

import com.metavize.mvvm.logging.LogEvent;
import com.metavize.tran.http.RequestLine;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTPBLK_EVT_BLK"
 * mutable="false"
 */
public class HttpBlockerEvent extends LogEvent
{
    private RequestLine requestLine;
    private Reason reason;
    private String category;

    // constructors -----------------------------------------------------------

    public HttpBlockerEvent() { }

    public HttpBlockerEvent(RequestLine requestLine, Reason reason, String category)
    {
        this.requestLine = requestLine;
        this.reason = reason;
        this.category = category;
    }

    // accessors --------------------------------------------------------------

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
     * Reason for blocking.
     *
     * @return the reason.
     * @hibernate.property
     * column="REASON"
     * type="com.metavize.tran.httpblocker.ReasonUserType"
     */
    public Reason getReason()
    {
        return reason;
    }

    public void setReason(Reason reason)
    {
        this.reason = reason;
    }

    /**
     * A string associated with the block reason.
     *
     * @return a <code>String</code> value
     * @hibernate.property
     * column="CATEGORY"
     */
    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "HttpBlockerEvent id: " + getId() + " RequestLine: " + requestLine;
    }
}
