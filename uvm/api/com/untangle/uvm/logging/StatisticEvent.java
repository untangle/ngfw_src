/**
 * $Id$
 */
package com.untangle.uvm.logging;

@SuppressWarnings("serial")
public abstract class StatisticEvent extends LogEvent
{
    /**
     * Return true if the current event has any interesting statistics that should be logged
     * now.  For instance, if you are tracking deltas, and all the deltas are zero, this should
     * return false;
     */
    public abstract boolean hasStatistics();
}
