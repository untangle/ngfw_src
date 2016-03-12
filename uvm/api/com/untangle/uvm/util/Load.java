/**
 * $Id: Load.java,v 1.00 2016/03/12 11:06:55 dmorris Exp $
 */
package com.untangle.uvm.util;

/**
 *
 * Load represents a "load" like a CPU load.
 *
 */
public class Load
{
    /**
     * The last time (in System.currentTimeMillis()) that the load value was updated
     */
    private long lastUpdate;

    /**
     * The load (last time it was calculated)
     */
    private double load;

    /**
     * The timeframe (in seconds) to compute the load
     * 60 is a 1-minute load
     */
    private int timeframeSec;


    public Load( int timeframeSec )
    {
        this.timeframeSec = timeframeSec;
        this.load = 0.0;
        this.lastUpdate = System.currentTimeMillis();
    }
    
    public double incrementLoad(int times)
    {
        long now = System.currentTimeMillis();
        long duration = now - lastUpdate;
        this.lastUpdate = now;

        /**
         * If the clock went backwards or has not moved
         * just assume 1 millisec
         */
        if ( duration <= 0 )
            duration = 1;
        
        double num  = Math.exp( -duration / ( 1000.0 * this.timeframeSec ) );
        this.load  = (num * load) + ((1-num) * ( (times*(1000.0*this.timeframeSec)) / duration));
        return this.load;
    }
    
    public double incrementLoad()
    {
        return incrementLoad(1);
    }

    public double getLoad()
    {
        return incrementLoad(0);
    }

    
}
