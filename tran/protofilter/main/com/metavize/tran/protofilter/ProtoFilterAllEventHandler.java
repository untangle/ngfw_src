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

package com.metavize.tran.protofilter;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.logging.EventHandler;
import com.metavize.mvvm.logging.FilterDesc;
import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.util.TransactionWork;
import org.hibernate.Query;
import org.hibernate.Session;

public class ProtoFilterAllEventHandler implements EventHandler<ProtoFilterLogEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("Protocol Events");

    private static final String WARM_QUERY
        = "FROM ProtoFilterLogEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    ProtoFilterAllEventHandler(TransformContext transformContext)
    {
        this.transformContext = transformContext;
    }

    // EventCache methods -----------------------------------------------------

    public FilterDesc getFilterDesc()
    {
        return FILTER_DESC;
    }

    public List<ProtoFilterLogEvent> doWarm(final int limit)
    {
        final List<ProtoFilterLogEvent> l = new LinkedList<ProtoFilterLogEvent>();

        TransactionWork tw = new TransactionWork()
            {
                private final Policy policy = transformContext.getTid()
                    .getPolicy();

                public boolean doWork(Session s) throws SQLException
                {
                    Query q = s.createQuery(WARM_QUERY);
                    q.setParameter("policy", policy);
                    for (Iterator i = q.iterate(); i.hasNext() && l.size() < limit; ) {
                        ProtoFilterLogEvent sb = (ProtoFilterLogEvent)i.next();
                        l.add(sb);
                    }

                    return true;
                }
            };
        transformContext.runTransaction(tw);

        return l;
    }

    public boolean accept(ProtoFilterLogEvent e)
    {
        return true;
    }
}
