/*
 * Copyright (c) 2005, 2006 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn;

import com.metavize.mvvm.logging.RepositoryDesc;
import com.metavize.mvvm.logging.SimpleEventFilter;

public class ClientConnectEventAllFilter
  implements SimpleEventFilter<ClientConnectEvent> {
  
    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Closed Sessions");

    private static final String WARM_QUERY
        = "FROM ClientConnectEvent evt ORDER BY evt.timeStamp DESC";

    // SimpleEventFilter methods ----------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(ClientConnectEvent e)
    {
        return true;
    }
}
