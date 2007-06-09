/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 */

package com.untangle.tran.ids;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.logging.PipelineEvent;
import com.untangle.mvvm.logging.SyslogBuilder;
import com.untangle.mvvm.logging.SyslogPriority;
import com.untangle.mvvm.tran.PipelineEndpoints;

/**
 * Log event for a blocked request.
 *
 * @author <a href="mailto:nchilders@untangle.com">Nick Childers</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_ids_evt", schema="events")
    public class IDSLogEvent extends PipelineEvent {

        private String classification;
        private String message;
        private boolean blocked;
        private int ruleSid;

        // constructors -----------------------------------------------------------

        public IDSLogEvent() { }

        public IDSLogEvent(PipelineEndpoints pe, int ruleSid, String classification,
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
            return "IDSLogEvent id: " + getId() + " Message: " + message;
        }
    }
