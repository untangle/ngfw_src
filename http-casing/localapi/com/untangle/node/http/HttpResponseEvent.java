/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
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
    @Table(name="n_http_evt_resp", schema="events")
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
