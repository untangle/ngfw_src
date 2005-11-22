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
import java.util.Map;

import com.metavize.mvvm.MvvmContextFactory;
import com.metavize.mvvm.policy.Policy;
import com.metavize.mvvm.tran.TransformContext;
import com.metavize.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Session;

class EventCache<E extends LogEvent> implements EventRepository<E>
{
    private final EventLogger<E> eventLogger;
    private final ListEventFilter<E> eventFilter;

    private final LinkedList<E> cache = new LinkedList<E>();

    private final Logger logger = Logger.getLogger(getClass());

    private boolean cold = true;

    // constructors -----------------------------------------------------------

    EventCache(EventLogger<E> eventLogger, ListEventFilter<E> eventFilter)
    {
        this.eventLogger = eventLogger;
        this.eventFilter = eventFilter;
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
        return eventFilter.getRepositoryDesc();
    }

    // package protected methods ----------------------------------------------

    void log(E e)
    {
        if (eventFilter.accept(e)) {
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
            final int limit = eventLogger.getLimit();

            if (cache.size() < limit) {
                final TransformContext tctx = eventLogger.getTransformContext();

                TransactionWork tw = new TransactionWork()
                    {
                        public boolean doWork(Session s) throws SQLException
                        {
                            Map params;
                            if (null != tctx) {
                                Policy policy = tctx.getTid().getPolicy();
                                params = Collections.singletonMap("policy", policy);
                            } else {
                                params = Collections.emptyMap();
                            }

                            eventFilter.warm(s, cache, limit, params);

                            return true;
                        }
                    };

                if (null == tctx) {
                    MvvmContextFactory.context().runTransaction(tw);
                } else {
                    tctx.runTransaction(tw);
                }

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
}
