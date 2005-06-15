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

import java.net.URI;
import java.nio.ByteBuffer;

import com.metavize.tran.token.Token;

/**
 * Holds a RFC 2616 request-line.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTP_REQ_LINE"
 * mutable="false"
 */
public class RequestLine implements Token
{
    private Long id;
    private HttpMethod method;
    private URI requestUri;
    private String httpVersion;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public RequestLine() { }

    public RequestLine(HttpMethod method, URI requestUri, String httpVersion)
    {
        this.method = method;
        this.requestUri = requestUri;
        this.httpVersion = httpVersion;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="REQUEST_ID"
     * generator-class="native"
     */
    private Long getId()
    {
        return id;
    }

    private void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Request method.
     *
     * @return the request method.
     * @hibernate.property
     * column="METHOD"
     * type="com.metavize.tran.http.HttpMethodUserType"
     */
    public HttpMethod getMethod()
    {
        return method;
    }

    public void setMethod(HttpMethod method)
    {
        this.method = method;
    }

    /**
     * Request URI.
     *
     * @return the request URI.
     * @hibernate.property
     * column="URI"
     * type="com.metavize.mvvm.type.UriUserType"
     */
    public URI getRequestUri()
    {
        return requestUri;
    }

    public void setRequestUri(URI requestUri)
    {
        this.requestUri = requestUri;
    }

    /**
     * The HTTP version.
     *
     * XXX try to save space here? dont save at all?
     *
     * @return the HTTP version.
     * @hibernate.property
     * column="HTTP_VERSION"
     * length="10"
     */
    public String getHttpVersion()
    {
        return httpVersion;
    }

    public void setHttpVersion(String httpVersion)
    {
        this.httpVersion = httpVersion;
    }

    // Token methods ----------------------------------------------------------

    public ByteBuffer getBytes()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(method).append(" ").append(requestUri.toString())
            .append(" ").append(httpVersion).append("\r\n");
        byte[] buf = sb.toString().getBytes();

        return ByteBuffer.wrap(buf);
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "RequestLine id: " + id + " length: "
            + requestUri.toString().length();
    }
}
