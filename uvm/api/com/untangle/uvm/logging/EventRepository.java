/**
 * $Id$
 */
package com.untangle.uvm.logging;

import java.util.List;

/**
 * Client interface for retrieving <code>LogEvent</code>s from
 * <code>EventLogger</code>.
 */
public interface EventRepository<E extends LogEvent>
{
    int MAX_SIZE = 1000;

    RepositoryDesc getRepositoryDesc();

    List<E> getEvents(int limit);

    List<E> getEvents();
}
