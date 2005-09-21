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

package com.metavize.tran.firewall;

import java.io.Serializable;

import com.metavize.mvvm.logging.LogEvent;

/**
 * Log event for the firewall.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_FIREWALL_EVT"
 * mutable="false"
 */

public class FirewallEvent extends LogEvent implements Serializable
{
    private int     sessionId;
    private boolean wasBlocked;
    private int     ruleIndex;
    
    // Constructors 
    /**
     * Hibernate constructor 
     */
    public FirewallEvent()
    {
    }

    public FirewallEvent( int sessionId, boolean wasBlocked, int ruleIndex )
    {
        this.sessionId  = sessionId;
        this.wasBlocked = wasBlocked;
        this.ruleIndex  = ruleIndex;
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
     * Firewall rule that triggered this event
     *
     * @return If the session was passed or blocked.
     * @hibernate.property
     * column="WAS_BLOCKED"
     */
    public boolean getWasBlocked()
    {
        return wasBlocked;
    }

    public void setWasBlocked( boolean wasBlocked )
    {
        this.wasBlocked = wasBlocked;
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
}
