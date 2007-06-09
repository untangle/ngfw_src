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

package com.untangle.node.http;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a request.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_http_evt_req", schema="events")
    public class HttpRequestEvent extends LogEvent
    {
        private RequestLine requestLine;
        private String host;
        private int contentLength;

        // constructors -----------------------------------------------------------

        public HttpRequestEvent() { }

        public HttpRequestEvent(RequestLine requestLine,
                                String host)
        {
            this.requestLine = requestLine;
            this.host = host;

            requestLine.setHttpRequestEvent(this);
        }

        public HttpRequestEvent(RequestLine requestLine,
                                String host, int contentLength)
        {
            this.requestLine = requestLine;
            this.host = host;
            this.contentLength = contentLength;

            requestLine.setHttpRequestEvent(this);
        }

        // accessors --------------------------------------------------------------

        /**
         * Request Line.
         *
         * @return the request line.
         */
        @OneToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="request_id")
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
         */
        @Column(name="content_length", nullable=false)
        public int getContentLength()
        {
            return contentLength;
        }

        public void setContentLength(int contentLength)
        {
            this.contentLength = contentLength;
        }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            requestLine.getPipelineEndpoints().appendSyslog(sb);

            sb.startSection("info");
            sb.addField("host", host);
            sb.addField("uri", requestLine.getRequestUri().toString());;
            sb.addField("content-length", contentLength);
        }

        @Transient
        public String getSyslogId()
        {
            return "Request";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            return SyslogPriority.INFORMATIONAL; // statistics or normal operation
        }
    }
