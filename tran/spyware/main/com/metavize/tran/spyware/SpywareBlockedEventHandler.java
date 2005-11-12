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

public class SpywareBlockedEventHandler implements EventHandler<SpywareEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("Blocked Events");

    private static final String ACCESS_QUERY
        = "FROM SpywareAccessEvent evt WHERE evt.pipelineEndpoints.policy = :policy and evt.blocked = true ORDER BY evt.timeStamp";
    private static final String ACTIVEX_QUERY
        = "FROM SpywareActiveXEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String BLACKLIST_QUERY
        = "FROM SpywareBlacklistEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String COOKIE_QUERY
        = "FROM SpywareCookieEvent evt WHERE evt.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private static final String[] QUERIES = new String[]
        { ACCESS_QUERY, ACTIVEX_QUERY, BLACKLIST_QUERY, COOKIE_QUERY };

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    SpywareBlockedEventHandler(TransformContext transformContext)
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
                    for (String query : QUERIES) {
                        runQuery(s, query);
                    }

                    return true;
                }

                private void runQuery(Session s, String query)
                    throws SQLException
                {
                    Query q = s.createQuery(query);
                    q.setParameter("policy", policy);
                    int c = 0;
                    for (Iterator i = q.iterate(); i.hasNext() && ++c < limit; ) {
                        SpywareEvent sb = (SpywareEvent)i.next();
                        l.add(sb);
                    }
                }
            };
        transformContext.runTransaction(tw);

        return l;
    }

    public boolean accept(SpywareEvent e)
    {
        return e.isBlocked();
    }
}
