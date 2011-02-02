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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

import org.apache.log4j.Logger;

import com.untangle.uvm.logging.EventLogger;
import com.untangle.uvm.logging.EventRepository;
import com.untangle.uvm.logging.ListEventFilter;
import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.logging.RepositoryDesc;
import com.untangle.uvm.logging.SimpleEventFilter;
import com.untangle.uvm.node.NodeContext;

/**
 * Implementation of EventLogger.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class EventLoggerImpl<E extends LogEvent> extends EventLogger<E>
{
    private static final boolean LOGGING_DISABLED;

    private final List<EventCache<E>> caches = new LinkedList<EventCache<E>>();
    private final NodeContext nodeContext;
    private final BlockingQueue<LogEventDesc> inputQueue;
    private final String tag;

    private final Logger logger = Logger.getLogger(getClass());

    // constructors -----------------------------------------------------------

    EventLoggerImpl()
    {
        this.nodeContext = null;
        inputQueue = UvmContextImpl.getInstance().loggingManager().getInputQueue();
        this.tag = "uvm[0]: ";
    }

    EventLoggerImpl(NodeContext nodeContext)
    {
        this.nodeContext = nodeContext;
        inputQueue = UvmContextImpl.getInstance().loggingManager().getInputQueue();
        String name = nodeContext.getNodeDesc().getSyslogName();
        this.tag = name + "[" + nodeContext.getNodeId().getId() + "]: ";
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

    // public methods --------------------------------------------------------

    public EventRepository<E> addSimpleEventFilter(SimpleEventFilter<E> simpleFilter)
    {
        ListEventFilter<E> lef = new SimpleEventFilterAdaptor<E>(simpleFilter);
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
        EventCache<E> ec = new EventRepositoryCache<E>(er);
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

    void doLog(E e)
    {
        for (EventCache<E> ec : caches) {
            ec.log(e);
        }
    }

    NodeContext getNodeContext()
    {
        return nodeContext;
    }

    // private classes --------------------------------------------------------

    private static class EventRepositoryCache<E extends LogEvent> extends EventCache<E>
    {
        private final EventRepository<E> eventRepository;

        EventRepositoryCache(EventRepository<E> eventRepository)
        {
            this.eventRepository = eventRepository;
        }


        // EventRepository methods --------------------------------------------
        public RepositoryDesc getRepositoryDesc() {
            return eventRepository.getRepositoryDesc();
        }

        public List<E> getEvents(int limit)
        {
            return eventRepository.getEvents(limit);
        }
        
        public List<E> getEvents()
        {
            return eventRepository.getEvents();
        }

        // EventCache methods ------------------------------------------------
        public void log(E e) { }
        public void setEventLogger(EventLoggerImpl<E> el) { }
    }

    // static initialization --------------------------------------------------

    static {
        LOGGING_DISABLED = RemoteLoggingManagerImpl.isLoggingDisabled();
    }
}
