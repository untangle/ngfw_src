package com.untangle.uvm.benchmark;

import java.io.Serializable;

import com.untangle.uvm.security.Tid;

public class Benchmark implements Serializable {
    
    private final Tid tid;
    private final String nodeName;
    private final String name;
    private final boolean[] hasMin = new boolean[Event.MAX_EVENT.getKey()];
    private final long[] min = new long[Event.MAX_EVENT.getKey()]; 
    private final long[] max = new long[Event.MAX_EVENT.getKey()]; 
    private final long[] total = new long[Event.MAX_EVENT.getKey()]; 
    private final long[] count = new long[Event.MAX_EVENT.getKey()];
    
    public Benchmark( Tid tid, String nodeName, String name )
    {
        this.tid = tid;
        this.nodeName = nodeName;
        this.name = name;
        resetEvents();
    }
    
    public synchronized void addEvent(Event event, long value)
    {
        if ( event == Event.MAX_EVENT ) {
            throw new IllegalArgumentException( "Do not use MAX_EVENT to retrieve values.");
        }
        int key = event.getKey();
        if ( !hasMin[key]) {
            min[key] = value;
            hasMin[key] = true;
        } else if ( value < min[key]) {
            min[key] = value;
        }
        
        if ( value > max[key]) {
            max[key] = value;
        }
        
        count[key]++;
        total[key] += value;
    }
    
    public Tid getTid()
    {
        return this.tid;
    }
    
    public String getNodeName()
    {
        return this.nodeName;
    }
    
    public String getName()
    {
        return this.name;
    }
    
    public long[] getMin()
    {
        return min;
    }
    
    public synchronized void getMin(long[] dest)
    {
        System.arraycopy(min, 0, dest, 0, min.length);
    }

    public long[] getMax()
    {
        return max;
    }

    public synchronized void getMax(long[] dest)
    {
        System.arraycopy(max, 0, dest, 0, max.length);
    }

    public long[] getCount()
    {
        return count;
    }

    public synchronized void getCount(long[] dest)
    {
        System.arraycopy(count, 0, dest, 0, count.length);
    }

    public long[] getTotal()
    {
        return total;
    }

    public synchronized void getTotals(long[] dest)
    {
        System.arraycopy(total, 0, dest, 0, total.length);
    }
    
    public synchronized void getAverages(long[] dest)
    {
        for ( int c = 0 ; c < Event.MAX_EVENT.getKey() ; c++ ) {
            dest[c] = total[c] / count[c];
        }
    }
    
    public synchronized void getAll(long[] min, long[] max, long[] totals, long[] count, long[] avg, boolean clear)
    {
        System.arraycopy(min, 0, min, 0, min.length);
        System.arraycopy(max, 0, max, 0, min.length);
        System.arraycopy(totals, 0, totals, 0, min.length);
        System.arraycopy(count, 0, count, 0, min.length);
        
        for ( int c = 0 ; c < Event.MAX_EVENT.getKey() ; c++ ) {
            avg[c] = total[c] / count[c];
        }
        
        if ( clear ) {
            resetEvents();
        }
    }
    
    public synchronized void resetEvents()
    {
        for ( int c = 0 ; c < Event.MAX_EVENT.getKey() ; c++ ) {
            min[c] = -1;
            max[c] = 0;
            count[c] = 0;
            total[c] = 0;
            hasMin[c]= false;
        }
    }

}
