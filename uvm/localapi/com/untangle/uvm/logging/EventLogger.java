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

package com.untangle.uvm.logging;

import java.util.List;

public abstract class EventLogger<E extends LogEvent>
    implements EventManager<E>
{
    // EventManager methods ---------------------------------------------------

    public abstract List<RepositoryDesc> getRepositoryDescs();
    public abstract EventRepository<E> getRepository(String repositoryName);
    public abstract List<EventRepository<E>> getRepositories();

    // public methods --------------------------------------------------------

    public abstract EventRepository<E> addSimpleEventFilter(SimpleEventFilter<E> simpleFilter);
    public abstract EventRepository<E> addListEventFilter(ListEventFilter<E> listFilter);
    public abstract EventRepository<E> addEventRepository(EventRepository<E> ec);

    public abstract void log(E e);
}
