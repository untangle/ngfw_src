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

package com.metavize.tran.spyware;

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

public class SpywareCookieEventHandler implements EventHandler<SpywareEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("Cookie Events");

    private static final String WARM_QUERY
        = "FROM SpywareCookieEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    SpywareCookieEventHandler(TransformContext transformContext)
    {
        this.transformContext = transformContext;
    }

    // EventCache methods -----------------------------------------------------

    public FilterDesc getFilterDesc()
    {
        return FILTER_DESC;
    }

    public List<SpywareEvent> doWarm(final int limit)
    {
        final List<SpywareEvent> l = new LinkedList<SpywareEvent>();

        TransactionWork tw = new TransactionWork()
            {
                private final Policy policy = transformContext.getTid()
                    .getPolicy();

                public boolean doWork(Session s) throws SQLException
                {
                    Query q = s.createQuery(WARM_QUERY);
                    q.setParameter("policy", policy);
                    for (Iterator i = q.iterate(); i.hasNext() && l.size() < limit; ) {
                        SpywareEvent sb = (SpywareCookieEvent)i.next();
                        l.add(sb);
                    }

                    return true;
                }
            };
        transformContext.runTransaction(tw);

        return l;
    }

    public boolean accept(SpywareEvent e)
    {
        return e instanceof SpywareCookieEvent;
    }
}
