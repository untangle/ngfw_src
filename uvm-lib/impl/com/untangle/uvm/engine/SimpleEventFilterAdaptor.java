/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.engine;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;

import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;

/**
 * Adapts a SimpleEventFilter to a ListEventFilter.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
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

    @SuppressWarnings("unchecked") //Query
    private void runQuery(String query, Session s, List<E> l, int limit, Map<String, Object> params)
    {
        Query q = s.createQuery(query);
        for (String param : q.getNamedParameters()) {
            Object o = params.get(param);
            if (null != o) {
                q.setParameter(param, o);
            }
        }

        q.setMaxResults(limit);

        int c = 0;
        for (Iterator<E> i = q.iterate(); i.hasNext() && ++c < limit; ) {
            E sb = i.next();
            Hibernate.initialize(sb);
            l.add(sb);
        }
    }
}
