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

import java.util.List;

public abstract class EventLogger<E extends LogEvent>
    implements EventManager<E>
{
    // EventManager methods ---------------------------------------------------

    public abstract List<RepositoryDesc> getRepositoryDescs();
    public abstract EventRepository<E> getRepository(String repositoryName);
    public abstract List<EventRepository<E>> getRepositories();
    public abstract void setLimit(int limit);
    public abstract int getLimit();

    // public methods --------------------------------------------------------

    public abstract EventRepository<E> addSimpleEventFilter(SimpleEventFilter<E> simpleFilter);
    public abstract EventRepository<E> addListEventFilter(ListEventFilter<E> listFilter);
    public abstract EventRepository<E> addEventRepository(EventRepository<E> ec);

    public abstract void addEventLoggerShutdownListener(EventLoggerListener l);
    public abstract void removeEventLoggerShutdownListener(EventLoggerListener l);

    public abstract void log(E e);
    public abstract void start();
    public abstract void stop();
}
