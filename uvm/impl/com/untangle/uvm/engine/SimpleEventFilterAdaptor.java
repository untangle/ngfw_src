/*
 * $Id$
 */
package com.untangle.uvm.engine;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.Session;
import org.apache.log4j.Logger;

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

    private final Logger logger = Logger.getLogger(getClass());
    
    SimpleEventFilterAdaptor(SimpleEventFilter<E> simpleEventFilter)
    {
        this.simpleEventFilter = simpleEventFilter;
    }

    // ListEventFilter methods ------------------------------------------------

    public RepositoryDesc getRepositoryDesc()
    {
        return simpleEventFilter.getRepositoryDesc();
    }

    public void doGetEvents(Session s, List<E> l, int limit, Map<String, Object> params)
    {
        for (String q : simpleEventFilter.getQueries()) {
            logger.debug("Events required: query='" + q + "', params='" + params + "'");
            runQuery(q, s, l, limit, params);
            logger.debug("... query returned: '" + l.size() + "'");
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
            if (sb == null)
                logger.warn("Query (" + query + ") returned null item");
            Hibernate.initialize(sb);
            l.add(sb);
        }
    }
}
