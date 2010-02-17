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

package com.untangle.node.webfilter;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.node.http.RequestLine;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
    @org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_webfilter_evt_blk", schema="events")
    public class WebFilterEvent extends LogEvent
    {
        // action types
        private static final int PASSED = 0;
        private static final int BLOCKED = 1;

        private RequestLine requestLine;
        private Action action;
        private Reason reason;
        private String category;
        private String vendorName;

        // non-persistent fields -----------------------------------------------

        private boolean nonEvent = false;

        // constructors --------------------------------------------------------

        public WebFilterEvent() { }

        public WebFilterEvent(RequestLine requestLine, Action action,
                              Reason reason, String category,
                              String vendorName, boolean nonEvent)
        {
            this.requestLine = requestLine;
            this.action = action;
            this.reason = reason;
            this.category = category;

            this.vendorName = vendorName;

            this.nonEvent = nonEvent;

            if (nonEvent && null != requestLine
                && null != requestLine.getHttpRequestEvent()) {
                setTimeStamp(requestLine.getHttpRequestEvent().getTimeStamp());
            }
        }

        public WebFilterEvent(RequestLine requestLine, Action action,
                              Reason reason, String category,
                              String vendorName)
        {
            this.requestLine = requestLine;
            this.action = action;
            this.reason = reason;
            this.category = category;
            this.vendorName = vendorName;
        }

        // public methods ------------------------------------------------------

        @Transient
            public boolean isNonEvent()
        {
            return nonEvent;
        }

        // accessors -----------------------------------------------------------

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
         * The action taken.
         *
         * @return the action.
         */
        @Type(type="com.untangle.node.webfilter.ActionUserType")
            public Action getAction()
        {
            return action;
        }

        public void setAction(Action action)
        {
            this.action = action;
        }

        /**
         * Reason for blocking.
         *
         * @return the reason.
         */
        @Type(type="com.untangle.node.webfilter.ReasonUserType")
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
         */
        public String getCategory()
        {
            return category;
        }

        public void setCategory(String category)
        {
            this.category = category;
        }

        /**
         * Spam scanner vendor.
         *
         * @return the vendor
         */
        @Column(name="vendor_name")
        public String getVendorName()
        {
            return vendorName;
        }

        public void setVendorName(String vendorName)
        {
            this.vendorName = vendorName;
        }

        // WebFilterEvent methods ----------------------------------------------

        @Transient
            public int getActionType()
        {
            if (null == action ||
                Action.PASS_KEY == action.getKey()) {
                return PASSED;
            } else {
                return BLOCKED;
            }
        }

        // LogEvent methods ----------------------------------------------------

        @Transient
            public boolean isPersistent()
        {
            return !nonEvent;
        }

        // Syslog methods ------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            requestLine.getPipelineEndpoints().appendSyslog(sb);

            sb.startSection("info");
            sb.addField("url", requestLine.getUrl().toString());
            sb.addField("action", null == action ? "none" : action.getName());
            sb.addField("reason", null == reason ? "none" : reason.toString());
            sb.addField("category", null == category ? "none" : category);
        }

        @Transient
            public String getSyslogId()
        {
            return "Block";
        }

        @Transient
            public SyslogPriority getSyslogPriority()
        {
            switch(getActionType())
                {
                case PASSED:
                    // statistics or normal operation
                    return SyslogPriority.INFORMATIONAL;

                default:
                case BLOCKED:
                    return SyslogPriority.WARNING; // traffic altered
                }
        }

        // Object methods ------------------------------------------------------

        public String toString()
        {
            return "WebFilterEvent id: " + getId() + " RequestLine: "
                + requestLine;
        }
    }
