/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.ids;

import com.metavize.mvvm.logging.EventHandler;
import com.metavize.mvvm.logging.FilterDesc;

public class IDSBlockedLogEventHandler implements EventHandler<IDSLogEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("Blocked Events");

    private static final String WARM_QUERY = "FROM IDSLogEvent evt "
        + "WHERE evt.blocked = true evt.pipelineEndpoints.policy = :policy "
        + "ORDER BY evt.timeStamp";

    // constructors -----------------------------------------------------------

    IDSBlockedLogEventHandler() { }

    // EventCache methods -----------------------------------------------------

    public FilterDesc getFilterDesc()
    {
        return FILTER_DESC;
    }

    public String[] getQueries()
    {
        return new String[] { WARM_QUERY };
    }

    public boolean accept(IDSLogEvent e)
    {
        return e.isBlocked();
    }
}
