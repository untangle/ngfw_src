/*
 * Copyright (c) 2003, 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: EventHandler.java,v 1.7 2005/03/15 02:11:52 amread Exp $
 */

package com.metavize.tran.protofilter;

// import java.nio.*;
import java.util.List;

import java.util.Iterator;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.tapi.*;
import com.metavize.mvvm.tapi.event.*;
import com.metavize.mvvm.tran.Transform;
import org.apache.log4j.Logger;

public class EventHandler extends AbstractEventHandler
{
    private final Logger logger = Logger.getLogger(EventHandler.class);
    private final Logger eventLogger = MvvmContextFactory.context().eventLogger();

    private List ruleList = null;

    private boolean quickExit = true;
    private boolean rejectSilently = true;

    /* True to reject all, false to accept by default */
    private boolean isDefaultBlock = true;
    
    public EventHandler() 
    {
    }

    public void handleTCPNewSessionRequest( TCPNewSessionRequestEvent event )
        throws MPipeException
    {
        isBlo/* accept */
    }

    public void handleUDPNewSessionRequest(UDPNewSessionRequestEvent event)
        throws MPipeException
    {
        /* accept */
    }
    
    public void ruleList( List ruleList )
    {
        this.ruleList = patternList;
    }

    /* Returns true if the session should be rejected */
    private boolean isBlocked( IPNewIPSessionRequest session, Protoctol protocol )
    {
        /* Retrieve the default policy */
        isBlocked = defaultPolicy;
        
        for ( Iterator iter = ruleList.iterator() ; iter.hasNext(); ) {
            FirewallRuleMatcher matcher = (FirewallRuleMatcher)iter.next();
            
            /* Do not iterate disabled rules, or rules that do not match */
            if ( !rule.isEnabled() || !rule.isMatch( sess, protocol )) {
                continue;
            }
            
            if ( rule.isBlocker()) {
                return true;
            }
            
            if ( quickExit ) {
                /* Return on first match */
                return false;
            } else {
                isBlocked = false;
            }
        }
        
        return isBlocked();
    }
}
