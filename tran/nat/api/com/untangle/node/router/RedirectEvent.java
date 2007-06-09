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

package com.untangle.node.router;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.uvm.logging.PipelineEvent;
import com.untangle.uvm.logging.SyslogBuilder;
import com.untangle.uvm.logging.SyslogPriority;
import com.untangle.uvm.node.PipelineEndpoints;

/**
 * Log event for the firewall.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="tr_nat_redirect_evt", schema="events")
    public class RedirectEvent extends PipelineEvent implements Serializable
    {
        private int          ruleIndex;
        private boolean      isDmz;

        // Constructors
        public RedirectEvent() { }

        public RedirectEvent( PipelineEndpoints pe, int ruleIndex )
        {
            super(pe);
            this.ruleIndex = ruleIndex;
            this.isDmz     = false;
        }

        /* This is for DMZ events */
        public RedirectEvent( PipelineEndpoints pe )
        {
            super (pe);
            this.ruleIndex = 0;
            this.isDmz     = true;
        }

        /**
         * Rule index, when this event was triggered.
         *
         * @return current rule index for the rule that triggered this event.
         */
        @Column(name="rule_index", nullable=false)
        public int getRuleIndex()
        {
            return ruleIndex;
        }

        public void setRuleIndex( int ruleIndex )
        {
            this.ruleIndex = ruleIndex;
        }

        /**
         * True if this was caused by the DMZ rule.
         *
         * @return Whether or not this was a DMZ event.
         */
        @Column(name="is_dmz", nullable=false)
        public boolean getIsDmz()
        {
            return this.isDmz;
        }

        public void setIsDmz( boolean isDmz )
        {
            this.isDmz = isDmz;
        }

        // PipelineEvent methods --------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            getPipelineEndpoints().appendSyslog(sb);

            sb.startSection("info");
            sb.addField("rule", ruleIndex);
            sb.addField("is-dmz", isDmz);
        }

        @Transient
        public String getSyslogId()
        {
            return "Redirect";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
            return SyslogPriority.WARNING; // traffic altered
        }
    }
