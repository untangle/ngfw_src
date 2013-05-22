/**
 * $Id: HostStats.java,v 1.00 2013/05/20 13:38:10 dmorris Exp $
 */
package com.untangle.node.shield;

public class HostStats
{
    /**
     * Last time host stats was updated
     */
    private long lastUpdate = System.currentTimeMillis();

    /**
     * The 1-second load
     * Represents the average number of requests per second
     */
    //protected double load1 = 0;

    /**
     * The 5-second load
     * Represents the average number of requests per 5-second interval
     */
    protected double load5 = 0;

    /**
     * pulse is called to update all the appropriate loads
     * It should be called when the relevent hosts makes a request
     */
    public void pulse( int numRequests )
    {
        long now = System.currentTimeMillis();
        long duration = now - lastUpdate;
        this.lastUpdate = now;
        
        /**
         * If the clock went backwards or has not moved
         * just assume 1 millisec
         */
        if ( duration < 0 ) {
            duration = 1;
        } else if ( duration == 0 ) {
            duration = 1;
        } 

        //double num1  = Math.exp( -duration / ( 1000.0 ) );
        double num5  = Math.exp( -duration / ( 5000.0 ) );

        //double x1  = ( numRequests * ( 1000.0 ) ) / duration ;
        double x5  = ( numRequests * ( 5000.0 ) ) / duration ;

        //load1  = (num1*load1 + (1-num1)*x1);
        load5  = (num5*load5 + (1-num5)*x5);
    }

}