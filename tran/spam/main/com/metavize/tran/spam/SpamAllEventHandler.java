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

package com.metavize.tran.spam;

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

public class SpamAllEventHandler implements EventHandler<SpamEvent>
{
    private static final FilterDesc FILTER_DESC = new FilterDesc("All Events");

    private static final String LOG_QUERY
        = "FROM SpamLogEvent evt WHERE evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";
    private static final String SMTP_QUERY
        = "FROM SpamSmtpEvent evt WHERE evt.messageInfo.pipelineEndpoints.policy = :policy ORDER BY evt.timeStamp";

    private static final String[] QUERIES = new String[]
        { LOG_QUERY, SMTP_QUERY };

    private final TransformContext transformContext;

    // constructors -----------------------------------------------------------

    SpamAllEventHandler(TransformContext transformContext)
    {
        this.transformContext = transformContext;
    }

    // EventCache methods -----------------------------------------------------

    public FilterDesc getFilterDesc()
    {
        return FILTER_DESC;
    }

    public List<SpamEvent> doWarm(final int limit)
    {
        final List<SpamEvent> l = new LinkedList<SpamEvent>();

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
                        SpamEvent ve = (SpamEvent)i.next();
                        l.add(ve);
                    }
                }
            };
        transformContext.runTransaction(tw);

        return l;
    }

    public boolean accept(SpamEvent e)
    {
        return true;
    }
}
