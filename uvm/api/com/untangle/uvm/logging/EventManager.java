/*
 * $Id$
 */
package com.untangle.uvm.logging;

import java.util.List;

/**
 * Interface that allows the user interface to get a set of
 * <code>EventRepository</code>s.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
public interface EventManager<E extends LogEvent>
{
    List<RepositoryDesc> getRepositoryDescs();

    EventRepository<E> getRepository(String filterName);

    List<EventRepository<E>> getRepositories();

    void log(E e);
}
