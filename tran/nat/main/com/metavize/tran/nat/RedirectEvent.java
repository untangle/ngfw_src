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
import java.net.InetAddress;

import com.metavize.mvvm.logging.LogEvent;

/**
 * Log event for the firewall.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_NAT_REDIRECT_EVT"
 * mutable="false"
 */
public class RedirectEvent extends LogEvent implements Serializable
{
    private int          sessionId;
    private RedirectRule rule;
    private int          ruleIndex;
    private boolean      isDmz;
    
    // Constructors 
    /**
     * Hibernate constructor 
     */
    public RedirectEvent()
    {
    }

    public RedirectEvent( int sessionId, RedirectRule rule, int ruleIndex )
    {
        this.sessionId = sessionId;
        this.rule      = rule;
        this.ruleIndex = ruleIndex;
        this.isDmz     = false;
    }

    /* This is for DMZ events */
    public RedirectEvent( int sessionId )
    {
        this.sessionId = sessionId;
        this.rule      = null;
        this.ruleIndex = 0;
        this.isDmz     = true;
    }

    /**
     * Session id.
     *
     * @return the id of the session
     * @hibernate.property
     * column="SESSION_ID"
     */
    public int getSessionId()
    {
        return sessionId;
    }
    
    public void setSessionId( int sessionId )
    {
        this.sessionId = sessionId;
    }


    /**
     * Redirect rule that triggered this event
     *
     * @return firewall rule that triggered this event
     * @hibernate.many-to-one
     * class="com.metavize.tran.nat.RedirectRule"
     * column="RULE_ID"
     */
    public RedirectRule getRule()
    {
        return rule;
    }

    public void setRule( RedirectRule rule )
    {
        this.rule = rule;
    }

    /**
     * Rule index, when this event was triggered.
     *
     * @return current rule index for the rule that triggered this event.
     * @hibernate.property
     * column="RULE_INDEX"
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
     * column="IS_DMZ"
     */
    public boolean getIsDmz()
    {
        return this.isDmz;
    }
    
    public void setIsDmz( boolean isDmz )
    {
        this.isDmz = isDmz;
    }
}
