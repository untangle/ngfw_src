/**
 * $Id: Load.java,v 1.00 2016/03/12 11:06:55 dmorris Exp $
 */
package com.untangle.uvm.util;

import org.apache.log4j.Logger;

/**
 *
 * Load represents a "load" like a CPU load.
 *
 */
public class Load
{
    private final Logger logger = Logger.getLogger(Load.class);

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
    private final int timeframeSec;

    /**
     * Initialize instance of Load.
     * @param  timeframeSec integer of timeframe in seconds.
     * @return              instance of Load
     */
    public Load( int timeframeSec )
    {
        this.timeframeSec = timeframeSec;
        this.load = 0.0;
        this.lastUpdate = System.currentTimeMillis();
    }
    
    /**
     * Add to the load.
     * @param  times integer of count to increase
     * @return       double new value of load.
     */
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
        double newLoad = ( (times*(1000.0*this.timeframeSec)) / duration);
        this.load  = (num*this.load) + ((1-num)*newLoad);
        return this.load;
    }
    
    /**
     * Incrment load by 1.
     * @return [description]
     * @return       double new value of load.
     */
    public double incrementLoad()
    {
        return incrementLoad(1);
    }

    /**
     * Return current load.
     * @return       double current value of load.
     */
    public double getLoad()
    {
        return incrementLoad(0);
    }

    
}
