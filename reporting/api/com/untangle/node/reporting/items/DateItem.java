/**
 * $Id: DateItem.java,v 1.00 2012/06/11 14:58:27 dmorris Exp $
 */
package com.untangle.node.reporting.items;

import java.io.Serializable;
import java.util.Date;

@SuppressWarnings("serial")
public class DateItem
    implements Serializable, Comparable<DateItem>
{
    private final Date date;
    private final Integer numDays;

    public DateItem(Date date, Integer numDays)
    {
        if (null == date || null == numDays) {
            throw new IllegalArgumentException("date or numDays is null");
        }

        this.date = date;
        this.numDays = numDays;
    }

    public Date getDate()
    {
        return date;
    }

    public Integer getNumDays()
    {
        return numDays;
    }

    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        } else if (o instanceof DateItem) {
            DateItem di = (DateItem)o;
            return di.date.equals(this.date) && di.numDays.equals(this.numDays);
        } else {
            return false;
        }
    }

    public int hashCode()
    {
        int result = 17;
        result = 37 * result + date.hashCode();
        result = 37 * result + numDays.hashCode();

        return result;
    }

    public int compareTo(DateItem di)
    {
        int i = date.compareTo(di.date);
        if (i == 0) {
            return numDays.compareTo(di.numDays);
        } else {
            return i;
        }
    }
}