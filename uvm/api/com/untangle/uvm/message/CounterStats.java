/**
 * $Id: CounterStats.java,v 1.00 2012/04/26 15:21:15 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.util.Date;

/**
 * Counters.
 */
public interface CounterStats
{
    long getCount();
    long getCountSinceMidnight();
    Date getLastActivityDate();
}
