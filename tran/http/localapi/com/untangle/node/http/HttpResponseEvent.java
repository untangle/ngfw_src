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
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for response.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_http_evt_resp", schema="events")
    public class HttpResponseEvent extends LogEvent
    {
        private RequestLine requestLine;
        private String contentType;
        private int contentLength;

        // constructors -----------------------------------------------------------

        public HttpResponseEvent() { }

        public HttpResponseEvent(RequestLine requestLine,
                                 String contentType, int contentLength)
        {
            this.requestLine = requestLine;
            this.contentType = contentType;
            this.contentLength = contentLength;
        }

        // accessors --------------------------------------------------------------

        /**
         * Request line for this HTTP response pair.
         *
         * @return the request line.
         */
        @ManyToOne(cascade=CascadeType.ALL, fetch=FetchType.EAGER)
        @JoinColumn(name="request_id")
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
         */
        @Column(name="content_type")
        public String getContentType()
        {
            return contentType;
        }

        public void setContentType(String contentType)
        {
            if (contentType != null && contentType.length() > DEFAULT_STRING_SIZE) contentType = contentType.substring(0, DEFAULT_STRING_SIZE);
            this.contentType = contentType;
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
            sb.addField("url", requestLine.getUrl().toString());
            sb.addField("content-type", contentType);
            sb.addField("content-length", contentLength);
        }

        @Transient
        public String getSyslogId()
        {
            return "Response";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            return SyslogPriority.INFORMATIONAL; // statistics or normal operation
        }
    }
