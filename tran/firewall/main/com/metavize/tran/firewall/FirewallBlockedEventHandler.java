/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: FirewallAccessEventHandler.java 3373 2005-11-12 00:13:48Z amread $
 */

package com.metavize.tran.firewall;


import com.metavize.mvvm.logging.EventHandler;
import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.tran.TransformContext;

public class FirewallBlockedEventHandler implements EventHandler<FirewallEvent>
{
    private static final RepositoryDesc FILTER_DESC = new RepositoryDesc("Firewall Block Events");

    private static final String WARM_QUERY
        = "FROM FirewallEvent evt WHERE evt.wasBlocked = true AND evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    FirewallBlockedEventHandler(TransformContext transformContext)
    {
        this.transformContext = transformContext;
    }

    // EventCache methods -----------------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return FILTER_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(FirewallEvent e)
    {
        if (e instanceof FirewallEvent) {
            FirewallEvent re = (FirewallEvent)e;
            return re.getWasBlocked();
        } else {
            return false;
        }
    }
}
