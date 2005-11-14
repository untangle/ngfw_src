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
import org.hibernate.Query;
import org.hibernate.Session;
import org.apache.log4j.Logger;

class EventCache<E extends LogEvent> implements EventFilter<E>
{
    private final EventLogger<E> eventLogger;
    private final EventHandler<E> eventHandler;

    private final LinkedList<E> cache = new LinkedList<E>();

    private final Logger logger = Logger.getLogger(getClass());

    private boolean cold = true;

    // constructors -----------------------------------------------------------

    EventCache(EventLogger<E> eventLogger, EventHandler<E> eventHandler)
    {
        this.eventLogger = eventLogger;
        this.eventHandler = eventHandler;
    }

    // LogFilter methods ------------------------------------------------------

    public List<E> getEvents()
    {
        synchronized (cache) {
            if (cold) {
                warm();
            }
            return new ArrayList<E>(cache);
        }
    }

    public FilterDesc getFilterDesc()
    {
        return eventHandler.getFilterDesc();
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
                E last = null;
                for (Iterator<E> i = cache.iterator(); i.hasNext(); ) {
                    E e = i.next();
                    if (last == e) {
                        i.remove();
                    } else {
                        last = e;
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
