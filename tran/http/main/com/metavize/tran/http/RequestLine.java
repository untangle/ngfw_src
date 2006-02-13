/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.http;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.metavize.mvvm.tran.PipelineEndpoints;

/**
 * Holds a RFC 2616 request-line.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_HTTP_REQ_LINE"
 * mutable="false"
 */
public class RequestLine implements Serializable
{
    private static final long serialVersionUID = -2183950932382112727L;

    private Long id;
    private HttpMethod method;
    private URI requestUri;
    private PipelineEndpoints pipelineEndpoints;
    private HttpRequestEvent httpRequestEvent; // Filled in after creation time.

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public RequestLine() { }

    public RequestLine(PipelineEndpoints pe, HttpMethod method, URI requestUri)
    {
        this.pipelineEndpoints = pe;
        this.method = method;
        this.requestUri = requestUri;
    }

    // business methods -------------------------------------------------------

    public URL getUrl()
    {
        // XXX this shouldn't happen in practice
        String host = null == httpRequestEvent ? ""
            : httpRequestEvent.getHost();

        URL url;
        try {
            url = new URL("http", host, getRequestUri().toString());
        } catch (MalformedURLException exn) {
            throw new RuntimeException(exn); // should never happen
        }

        return url;
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
     * Get the PipelineEndpoints.
     *
     * @return the PipelineEndpoints.
     * @hibernate.many-to-one
     * column="PL_ENDP_ID"
     * not-null="true"
     * cascade="all"
     */
    public PipelineEndpoints getPipelineEndpoints()
    {
        return pipelineEndpoints;
    }

    public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
    {
        this.pipelineEndpoints = pipelineEndpoints;
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
     * The HttpRequestEvent that logged this item.
     *
     * @return the HttpRequestEvent.
     * @hibernate.one-to-one
     * column="event_id"
     * property-ref="requestLine"
     */
    public HttpRequestEvent getHttpRequestEvent()
    {
        return httpRequestEvent;
    }

    public void setHttpRequestEvent(HttpRequestEvent httpRequestEvent)
    {
        this.httpRequestEvent = httpRequestEvent;
    }

    // Object methods ---------------------------------------------------------

    public String toString()
    {
        return "RequestLine id: " + id + " length: "
            + requestUri.toString().length() + " (" + super.toString() + ")";
    }
}
