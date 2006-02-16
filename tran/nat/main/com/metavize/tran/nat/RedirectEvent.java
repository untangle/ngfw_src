/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.nat;

import java.io.Serializable;

import com.metavize.mvvm.logging.PipelineEvent;
import com.metavize.mvvm.logging.SyslogBuilder;

import com.metavize.mvvm.networking.RedirectRule;

import com.metavize.mvvm.tran.PipelineEndpoints;

/**
 * Log event for the firewall.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_nat_redirect_evt"
 * mutable="false"
 */
public class RedirectEvent extends PipelineEvent implements Serializable
{
    private int          ruleIndex;
    private boolean      isDmz;

    // Constructors
    /**
     * Hibernate constructor
     */
    public RedirectEvent()
    {
    }

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
     * @hibernate.property
     * column="rule_index"
     */
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
     * @hibernate.property
     * column="is_dmz"
     */
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
}
