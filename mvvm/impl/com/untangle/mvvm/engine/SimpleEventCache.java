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

package com.untangle.mvvm.engine;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.logging.ListEventFilter;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.policy.Policy;
import com.untangle.mvvm.tran.TransformContext;
import com.untangle.mvvm.util.TransactionWork;
import org.apache.log4j.Logger;
import org.hibernate.Session;

class SimpleEventCache<E extends LogEvent> extends EventCache<E>
{
    private EventLoggerImpl<E> eventLogger;
    private final ListEventFilter<E> eventFilter;

    private final LinkedList<E> cache = new LinkedList<E>();

    private final Logger logger = Logger.getLogger(getClass());

    private boolean cold = true;

    // constructors ----------------------------------------------------------

    SimpleEventCache(ListEventFilter<E> eventFilter)
    {
        this.eventFilter = eventFilter;
    }

    public void setEventLogger(EventLoggerImpl<E> eventLogger)
    {
        this.eventLogger = eventLogger;
    }

    // EventRepository methods -----------------------------------------------

    public List<E> getEvents()
    {
        LoggingManagerImpl lm = MvvmContextImpl.getInstance().loggingManager();

        synchronized (cache) {
            if (cold && lm.isConversionComplete()) {
                warm();
            }
            return new ArrayList<E>(cache);
        }
    }

    public RepositoryDesc getRepositoryDesc()
    {
        return eventFilter.getRepositoryDesc();
    }

    // EventCache methods ----------------------------------------------------

    public void log(E e)
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

    public void checkCold()
    {
        synchronized (cache) {
            cold = eventLogger.getLimit() > cache.size();
        }
    }

    // private methods -------------------------------------------------------

    private void warm()
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
                    if (null == id) {
                        id = new Long(System.identityHashCode(e));
                    }

                    if (null == last ? last == id : last.equals(id)) {
                        // XXX we usually use linked lists, otherwise
                        // this is bad, probably better to make a new list
                        i.remove();
                    } else {
                        last = id;
                    }
                }
                cold = false;
            }
        }
    }
}
