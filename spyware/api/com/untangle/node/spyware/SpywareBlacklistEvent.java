/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.spyware;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.node.PipelineEndpoints;
import com.untangle.node.http.HttpRequestEvent;
import com.untangle.node.http.RequestLine;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_spyware_evt_blacklist", schema="events")
@SuppressWarnings("serial")
    public class SpywareBlacklistEvent extends SpywareEvent
    {
        private RequestLine requestLine; // pipeline endpoints & location

        // constructors -----------------------------------------------------------

        public SpywareBlacklistEvent() { }

        public SpywareBlacklistEvent(RequestLine requestLine)
        {
            this.requestLine = requestLine;
        }

        // SpywareEvent methods ---------------------------------------------------

        @Transient
        public String getType()
        {
            return "Blacklist";
        }

        @Transient
        public String getReason()
        {
            return "in URL List";
        }

        @Transient
        public String getIdentification()
        {
            HttpRequestEvent hre = requestLine.getHttpRequestEvent();
            String host = null == hre
                ? getPipelineEndpoints().getSServerAddr().toString()
                : hre.getHost();
            return "http://" + host + requestLine.getRequestUri().toString();
        }

        @Transient
        public boolean isBlocked()
        {
            return true;
        }

        @Transient
        public String getLocation()
        {
            return requestLine.getUrl().toString();
        }

        @Transient
        public PipelineEndpoints getPipelineEndpoints()
        {
            return requestLine.getPipelineEndpoints();
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

        // Syslog methods ---------------------------------------------------------

        // use SpywareEvent appendSyslog, getSyslogId and getSyslogPriority
    }
