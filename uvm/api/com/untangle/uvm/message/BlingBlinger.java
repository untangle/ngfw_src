/**
 * $Id: BlingBlinger.java,v 1.00 2012/04/26 15:22:59 dmorris Exp $
 */
package com.untangle.uvm.message;

import java.io.Serializable;
import java.util.Date;
import java.util.Calendar;

@SuppressWarnings("serial")
public class BlingBlinger implements CounterStats, Serializable
{
    private final StatDesc statDesc;

    private Date now = new Date();
    private Date lastUpdate = new Date();

    private volatile long count = 0;
    private volatile long countSinceMidnight = 0;

    public BlingBlinger(String name, String displayName, String unit, String action, boolean displayable)
    {
        this.statDesc = new StatDesc( name, displayName, action, unit, displayable );
    }

    // public methods ---------------------------------------------------------

    public StatDesc getStatDesc()
    {
        return statDesc;
    }

    public long increment()
    {
        return increment(1);
    }

    public long increment(long delta)
    {
        synchronized (this) {
            now.setTime(System.currentTimeMillis());
            Calendar lastUpdateCal = Calendar.getInstance();
            Calendar nowCal = Calendar.getInstance();
            lastUpdateCal.setTime(lastUpdate);
            nowCal.setTime(now);
            
            count += delta;
            if (lastUpdateCal.get(Calendar.DAY_OF_WEEK) == nowCal.get(Calendar.DAY_OF_WEEK) &&
                lastUpdateCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
                lastUpdateCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
                countSinceMidnight += delta;
            } else {
                countSinceMidnight = 0;
                lastUpdate.setTime(now.getTime());
            }
        }

        return count;
    }

    // CounterStats methods ---------------------------------------------------
    public long getCount()
    {
        return count;
    }

    public void setCount(long newValue)
    {
        synchronized (this) {
            count = 0;
            countSinceMidnight = 0;
            increment(newValue);
        }
    }
    
    public long getCountSinceMidnight()
    {
        return countSinceMidnight;
    }

    public Date getLastActivityDate()
    {
        return now;
    }
}

