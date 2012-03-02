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

package com.untangle.node.phish;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.node.http.RequestLine;
import org.hibernate.annotations.Type;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_phish_http_evt", schema="events")
@SuppressWarnings("serial")
    public class PhishHttpEvent extends LogEvent
    {
        // action types
        private static final int PASSED = 0;
        private static final int BLOCKED = 1;

        private RequestLine requestLine;
        private Action action;
        private String category;

        // non-persistent fields --------------------------------------------------
        private boolean nonEvent = false;

        // constructors -----------------------------------------------------------
        public PhishHttpEvent() { }

        public PhishHttpEvent(RequestLine requestLine, Action action,
                              String category, boolean nonEvent)
        {
            this.requestLine = requestLine;
            this.action = action;
            this.category = category;

            this.nonEvent = nonEvent;

            if (true == nonEvent && null != requestLine) {
                // to present consistent times for the same fake events
                // in different event filters,
                // amread suggested using timestamps of request line events
                setTimeStamp(requestLine.getHttpRequestEvent().getTimeStamp());
            }
        }

        public PhishHttpEvent(RequestLine requestLine, Action action,
                              String category)
        {
            this.requestLine = requestLine;
            this.action = action;
            this.category = category;
        }

        // public methods ---------------------------------------------------------

        @Transient
        public boolean isNonEvent()
        {
            return nonEvent;
        }

        // accessors --------------------------------------------------------------

        /**
         * Request line for this Phish HTTP response pair.
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
        @Type(type="com.untangle.node.phish.ActionUserType")
        public Action getAction()
        {
            return action;
        }

        public void setAction(Action action)
        {
            this.action = action;
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

        // PhishHttpEvent methods -------------------------------------------------

        @Transient
        public int getActionType()
        {
            if (null == action || Action.PASS_KEY == action.getKey()) {
                return PASSED;
            } else {
                return BLOCKED;
            }
        }

        // LogEvent methods -------------------------------------------------------

        @Transient
        public boolean isPersistent()
        {
            return !nonEvent;
        }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            requestLine.getSessionEvent().appendSyslog(sb);

            sb.startSection("info");
            sb.addField("url", requestLine.getUrl().toString());
            sb.addField("action", null == action ? "none" : action.getName());
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

        // Object methods ---------------------------------------------------------

        public String toString()
        {
            return "PhishHttpEvent id: " + getId() + " RequestLine: " + requestLine;
        }
    }
