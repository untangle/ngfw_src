/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.clamphish;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.untangle.mvvm.logging.ListEventFilter;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.tran.http.RequestLine;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

public class PhishHttpPassedFilter implements ListEventFilter<PhishHttpEvent>
{
    private static final String RL_QUERY = "FROM RequestLine rl ORDER BY rl.httpRequestEvent.timeStamp ASC";
    private static final String EVT_QUERY = "FROM PhishHttpEvent evt WHERE evt.requestLine = :requestLine";

    private static final RepositoryDesc REPO_DESC = new RepositoryDesc("Passed Phish HTTP Traffic");

    public RepositoryDesc getRepositoryDesc()
    {
        return REPO_DESC;
    }

    public boolean accept(PhishHttpEvent e)
    {
        return null == e.getAction() || Action.PASS == e.getAction();
    }

    public void warm(Session s, List<PhishHttpEvent> l, int limit,
                     Map<String, Object> params)
    {
        Query q = s.createQuery(RL_QUERY);
        for (String param : q.getNamedParameters()) {
            Object o = params.get(param);
            if (null != o) {
                q.setParameter(param, o);
            }
        }

        q.setMaxResults(limit);

        int c = 0;
        for (Iterator i = q.iterate(); i.hasNext() && c++ < limit; ) {
            RequestLine rl = (RequestLine)i.next();
            Query evtQ = s.createQuery(EVT_QUERY);
            evtQ.setEntity("requestLine", rl);
            PhishHttpEvent evt = (PhishHttpEvent)evtQ.uniqueResult();
            if (null == evt) {
                evt = new PhishHttpEvent(rl, null, null, true);
                Hibernate.initialize(evt);
                Hibernate.initialize(evt.getRequestLine());
                l.add(evt);
            } else if (Action.PASS == evt.getAction()) {
                Hibernate.initialize(evt);
                Hibernate.initialize(evt.getRequestLine());
                l.add(evt);
            }
        }
    }
}
