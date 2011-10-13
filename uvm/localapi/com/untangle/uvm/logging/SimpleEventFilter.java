/**
 * $Id$
 */
package com.untangle.uvm.logging;

/**
 * Filters results from an EventLogger.
 */
public interface SimpleEventFilter<E extends LogEvent>
{
    RepositoryDesc getRepositoryDesc();

    String[] getQueries();
}
