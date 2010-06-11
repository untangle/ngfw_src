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

package com.untangle.node.ips;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_ips_evt", schema="events")
    public class IpsLogEvent extends PipelineEvent {
        private String classification;
        private String message;
        private boolean blocked;
        private int ruleSid;

        // constructors -----------------------------------------------------------

        public IpsLogEvent() { }

        public IpsLogEvent(PipelineEndpoints pe, int ruleSid, String classification,
                           String message, boolean blocked) {
            super(pe);

            this.ruleSid = ruleSid;
            this.classification = classification;
            this.message = message;
            this.blocked = blocked;
        }

        // accessors --------------------------------------------------------------

        /**
         * SID of the rule that fired.
         */
        @Column(name="rule_sid", nullable=false)
        public int getRuleSid() {
            return this.ruleSid;
        }

        public void setRuleSid(int ruleSid) {
            this.ruleSid = ruleSid;
        }

        /**
         * Classification of signature that generated this event.
         *
         * @return the classification
         */
        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }

        /**
         * Message of signature that generated this event.
         *
         * @return the message
         */
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        /**
         * Was it blocked.
         *
         * @return whether or not the session was blocked (closed)
         */
        @Column(nullable=false)
        public boolean isBlocked() {
            return blocked;
        }

        public void setBlocked(boolean blocked) {
            this.blocked = blocked;
        }

        // Syslog methods ---------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            getPipelineEndpoints().appendSyslog(sb);

            sb.startSection("info");
            sb.addField("snort-id", ruleSid);
            sb.addField("blocked", blocked);
            sb.addField("message", message);
        }

        @Transient
        public String getSyslogId()
        {
            return "Log";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            // NOTICE = ips event logged
            // WARNING = traffic altered
            return false == blocked ? SyslogPriority.NOTICE : SyslogPriority.WARNING;
        }

        // Object methods ---------------------------------------------------------

        public String toString() {
            return "IpsLogEvent id: " + getId() + " Message: " + message;
        }
    }
