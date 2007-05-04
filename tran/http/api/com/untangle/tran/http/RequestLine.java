/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.http;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.tran.PipelineEndpoints;
import org.hibernate.annotations.Type;

/**
 * Holds a RFC 2616 request-line.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_http_req_line", schema="events")
    public class RequestLine implements Serializable
    {
        private static final long serialVersionUID = -2183950932382112727L;

        private Long id;
        private HttpMethod method;
        private URI requestUri;
        private PipelineEndpoints pipelineEndpoints;
        private HttpRequestEvent httpRequestEvent; // Filled in after creation time.

        // constructors -----------------------------------------------------------

        public RequestLine() { }

        public RequestLine(PipelineEndpoints pe, HttpMethod method, URI requestUri)
        {
            this.pipelineEndpoints = pe;
            this.method = method;
            this.requestUri = requestUri;
        }

        // business methods -------------------------------------------------------

        @Transient
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

        @Id
        @Column(name="request_id")
        @GeneratedValue
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
         */
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="pl_endp_id", nullable=false)
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
         */
        @Type(type="com.untangle.tran.http.HttpMethodUserType")
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
         */
        @Column(name="uri")
        @Type(type="com.untangle.mvvm.type.UriUserType")
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
         */
        @OneToOne(mappedBy="requestLine")
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
