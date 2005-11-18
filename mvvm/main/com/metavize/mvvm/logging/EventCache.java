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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;

class EventCache<E extends LogEvent> implements EventRepository<E>
{
    private final EventLogger<E> eventLogger;
    private final EventFilter<E> eventHandler;

    private final LinkedList<E> cache = new LinkedList<E>();

    private final Logger logger = Logger.getLogger(getClass());

    private boolean cold = true;

    // constructors -----------------------------------------------------------

    EventCache(EventLogger<E> eventLogger, EventFilter<E> eventHandler)
    {
        this.eventLogger = eventLogger;
        this.eventHandler = eventHandler;
    }

    // EventRepository methods ------------------------------------------------

    public List<E> getEvents()
    {
        synchronized (cache) {
            if (cold) {
                warm();
            }
            return new ArrayList<E>(cache);
        }
    }

    public RepositoryDesc getRepositoryDesc()
    {
        return eventHandler.getRepositoryDesc();
    }

    // package protected methods ----------------------------------------------

    void log(E e)
    {
        if (eventHandler.accept(e)) {
            synchronized (cache) {
                while (cache.size() >= eventLogger.getLimit()) {
                    cache.removeLast();
                }
                cache.add(0, e);
            }
        }
    }

    void warm()
    {
        synchronized (cache) {
            int limit = eventLogger.getLimit();

            if (cache.size() < limit) {
                doWarm(limit, cache);
                Collections.sort(cache);
                Long last = null;
                for (Iterator<E> i = cache.iterator(); i.hasNext(); ) {
                    E e = i.next();
                    Long id = e.getId();
                    if (null == last ? last == id : last.equals(id)) {
                        i.remove();
                    } else {
                        last = id;
                    }
                }
                cold = false;
            }
        }
    }

    void checkCold()
    {
        synchronized (cache) {
            cold = eventLogger.getLimit() > cache.size();
        }
    }

    // private methods --------------------------------------------------------

    private void doWarm(final int limit, final List<E> l)
    {
        final TransformContext tctx = eventLogger.getTransformContext();

        TransactionWork tw = new TransactionWork()
            {
                private final Policy policy = tctx.getTid().getPolicy();

                public boolean doWork(Session s) throws SQLException
                {
                    for (String query : eventHandler.getQueries()) {
                        runQuery(s, query);
                    }

                    return true;
                }

                private void runQuery(Session s, String query)
                    throws SQLException
                {
                    Query q = s.createQuery(query);
                    String[] params = q.getNamedParameters();
                    for (String param : params) {
                        if (param.equals("policy")) {
                            q.setParameter("policy", policy);
                        } else {
                            logger.debug("unknown parameter: " + param);
                        }
                    }

                    int c = 0;
                    for (Iterator i = q.iterate(); i.hasNext() && ++c < limit; ) {
                        E sb = (E)i.next();
                        l.add(sb);
                    }
                }
            };
        tctx.runTransaction(tw);
    }
}
