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

package com.metavize.tran.http;

import com.metavize.mvvm.logging.LogEvent;

/**
 * Log event for a request.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTP_EVT_REQ"
 * mutable="false"
 */
public class HttpRequestEvent extends LogEvent
{
    private int sessionId;
    private RequestLine requestLine;
    private String host;
    private int contentLength;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public HttpRequestEvent() { }

    public HttpRequestEvent(int sessionId, RequestLine requestLine,
                            String host)
    {
        this.sessionId = sessionId;
        this.requestLine = requestLine;
        this.host = host;

        requestLine.setHttpRequestEvent(this);
    }

    public HttpRequestEvent(int sessionId, RequestLine requestLine,
                            String host, int contentLength)
    {
        this.sessionId = sessionId;
        this.requestLine = requestLine;
        this.host = host;
        this.contentLength = contentLength;

        requestLine.setHttpRequestEvent(this);
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
     * Request Line.
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
        requestLine.setHttpRequestEvent(this);
    }

    /**
     * The host, as specified by the request header.
     *
     * @return the host.
     * @hibernate.property
     * column="HOST"
     */
    public String getHost()
    {
        return host;
    }

    public void setHost(String host)
    {
        this.host = host;
    }

    /**
     * Content length, as counted by the parser.
     *
     * @return number of octets in the body.
     * @hibernate.property
     * column="CONTENT_LENGTH"
     */
    public int getContentLength()
    {
        return contentLength;
    }

    public void setContentLength(int contentLength)
    {
        this.contentLength = contentLength;
    }
}
