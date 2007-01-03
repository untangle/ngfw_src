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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import com.untangle.mvvm.MvvmContextFactory;
import com.untangle.mvvm.logging.EventLogger;
import com.untangle.mvvm.logging.EventRepository;
import com.untangle.mvvm.logging.ListEventFilter;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.LogEvent;
import com.untangle.mvvm.logging.RepositoryDesc;
import com.untangle.mvvm.logging.SimpleEventFilter;
import com.untangle.mvvm.logging.SyslogManager;
import com.untangle.mvvm.security.Tid;
import com.untangle.mvvm.tran.TransformContext;
import org.apache.log4j.Logger;

class EventLoggerImpl<E extends LogEvent> extends EventLogger<E>
{
    private static final boolean LOGGING_DISABLED;

    private final List<EventCache<E>> caches = new LinkedList<EventCache<E>>();
    private final TransformContext transformContext;
    private final BlockingQueue<LogEventDesc> inputQueue;
    private final String tag;

    private final Logger logger = Logger.getLogger(getClass());

    private volatile int limit = 100;

    // constructors -----------------------------------------------------------

    EventLoggerImpl()
    {
        this.transformContext = null;
        inputQueue = LoggingManagerImpl.loggingManager().getInputQueue();
        this.tag = "mvvm[0]: ";
    }

    EventLoggerImpl(TransformContext transformContext)
    {
        this.transformContext = transformContext;
        inputQueue = LoggingManagerImpl.loggingManager().getInputQueue();
        String name = transformContext.getTransformDesc().getSyslogName();
        this.tag = name + "[" + transformContext.getTid().getId() + "]: ";
    }

    // EventManager methods ---------------------------------------------------

    public List<RepositoryDesc> getRepositoryDescs()
    {
        List<RepositoryDesc> l = new ArrayList<RepositoryDesc>(caches.size());
        for (EventCache<E> ec : caches) {
            l.add(ec.getRepositoryDesc());
        }

        return l;
    }

    public EventRepository<E> getRepository(String repositoryName)
    {
        for (EventCache<E> ec : caches) {
            if (ec.getRepositoryDesc().getName().equals(repositoryName)) {
                return ec;
            }
        }

        return null;
    }

    public List<EventRepository<E>> getRepositories()
    {
        return new LinkedList<EventRepository<E>>(caches);
    }

    public void setLimit(int limit)
    {
        boolean checkCold = limit > this.limit;

        this.limit = limit;

        if (checkCold) {
            for (EventCache<E> c : caches) {
                c.checkCold();
            }
        }
    }

    public int getLimit()
    {
        return limit;
    }

    // public methods --------------------------------------------------------

    public EventRepository<E> addSimpleEventFilter(SimpleEventFilter<E> simpleFilter)
    {
        ListEventFilter lef = new SimpleEventFilterAdaptor(simpleFilter);
        EventCache<E> ec = new SimpleEventCache<E>(lef);
        ec.setEventLogger(this);
        caches.add(ec);
        return ec;
    }

    public EventRepository<E> addListEventFilter(ListEventFilter<E> listFilter)
    {
        EventCache<E> ec = new SimpleEventCache<E>(listFilter);
        ec.setEventLogger(this);
        caches.add(ec);
        return ec;
    }

    public EventRepository<E> addEventRepository(EventRepository<E> er) {
        EventCache<E> ec = new EventRepositoryCache(er);
        caches.add(ec);
        return ec;
    }

    public void log(E e)
    {
        if (LOGGING_DISABLED) {
            return;
        }

        if (!inputQueue.offer(new LogEventDesc(this, e, tag))) {
            logger.warn("dropping logevent: " + e);
        }
    }

    // package private methods ------------------------------------------------

    void doLog(LogEvent e)
    {
        for (EventCache<E> ec : caches) {
            ec.log((E)e);
        }
    }

    TransformContext getTransformContext()
    {
        return transformContext;
    }

    // private classes --------------------------------------------------------

    private static class EventRepositoryCache<E extends LogEvent>
        extends EventCache<E>
    {
        private final EventRepository eventRepository;

        EventRepositoryCache(EventRepository eventRepository)
            {
                this.eventRepository = eventRepository;
            }


        // EventRepository methods --------------------------------------------
        public RepositoryDesc getRepositoryDesc() {
            return eventRepository.getRepositoryDesc();
        }

        public List<E> getEvents()
        {
            return eventRepository.getEvents();
        }

        // EventCache methods ------------------------------------------------
        public void log(E e) { }
        public void checkCold() { }
        public void setEventLogger(EventLoggerImpl<E> el) { }
    }

    // static initialization --------------------------------------------------

    static {
        LOGGING_DISABLED = LoggingManagerImpl.isLoggingDisabled();
    }
}
