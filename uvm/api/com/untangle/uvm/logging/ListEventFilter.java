/**
 * $Id$
 */
package com.untangle.uvm.logging;

import java.util.List;
import java.util.Map;

import org.hibernate.Session;

/**
 * Filters results from an EventLogger.
 */
public interface ListEventFilter<E extends LogEvent>
{
    RepositoryDesc getRepositoryDesc();

    void doGetEvents(Session s, List<E> l, int limit, Map<String, Object> params);
}
