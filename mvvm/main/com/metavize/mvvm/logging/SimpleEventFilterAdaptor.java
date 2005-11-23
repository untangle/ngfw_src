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

package com.metavize.mvvm.logging;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Query;
import org.hibernate.Session;

class SimpleEventFilterAdaptor<E extends LogEvent>
    implements ListEventFilter<E>
{
    private SimpleEventFilter<E> simpleEventFilter;

    SimpleEventFilterAdaptor(SimpleEventFilter<E> simpleEventFilter)
    {
        this.simpleEventFilter = simpleEventFilter;
    }

    // ListEventFilter methods ------------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return simpleEventFilter.getRepositoryDesc();
    }

    public boolean accept(E e)
    {
        return simpleEventFilter.accept(e);
    }


    public void warm(Session s, List<E> l, int limit,
                        Map<String, Object> params)
    {
        for (String q : simpleEventFilter.getQueries()) {
            runQuery(q, s, l, limit, params);
        }
    }

    // private methods --------------------------------------------------------

    private void runQuery(String query, Session s, List<E> l, int limit,
                          Map<String, Object> params)
    {
        Query q = s.createQuery(query);
        for (String param : q.getNamedParameters()) {
            Object o = params.get(param);
            if (null != o) {
                q.setParameter(param, o);
            }
        }

        int c = 0;
        for (Iterator i = q.iterate(); i.hasNext() && ++c < limit; ) {
            E sb = (E)i.next();
            l.add(sb);
        }
    }
}
