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
 * Log event for response.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTP_EVT_RESP"
 * mutable="false"
 */
public class HttpResponseEvent extends LogEvent
{
    private RequestLine requestLine;
    private String contentType;
    private int contentLength;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public HttpResponseEvent() { }

    public HttpResponseEvent(RequestLine requestLine, String contentType,
                             int contentLength)
    {
        this.requestLine = requestLine;
        if (contentType.length() > DEFAULT_STRING_SIZE) contentType = contentType.substring(0, DEFAULT_STRING_SIZE);
        this.contentType = contentType;
        this.contentLength = contentLength;
    }

    // accessors --------------------------------------------------------------

    /**
     * Request line for this HTTP response pair.
     *
     * @return the request line.
     * @hibernate.many-to-one
     * column="REQUEST_ID"
     * cascade="all"
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
     * The base Content-Type, without any encodings or other useless
     * nonsense.
     *
     * @return the content type.
     * @hibernate.property
     * column="CONTENT_TYPE"
     */
    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        if (contentType.length() > DEFAULT_STRING_SIZE) contentType = contentType.substring(0, DEFAULT_STRING_SIZE);
        this.contentType = contentType;
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
