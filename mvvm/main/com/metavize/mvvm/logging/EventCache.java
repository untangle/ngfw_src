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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

class EventCache<E extends LogEvent> implements EventFilter<E>
{
    private final EventLogger<E> eventLogger;
    private final EventHandler<E> eventHandler;

    private final LinkedList<E> cache = new LinkedList<E>();

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
                List<E> l = eventHandler.doWarm(limit);
                cache.addAll(l);
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
            System.out.println("COLD? " + cold);
        }
    }
}
