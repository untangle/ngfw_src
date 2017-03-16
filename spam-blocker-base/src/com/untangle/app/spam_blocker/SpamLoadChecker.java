/**
 * $Id$
 */
package com.untangle.app.spam_blocker;

import java.util.Random;

import org.apache.log4j.Logger;

import com.untangle.uvm.util.LoadAvg;

public final class SpamLoadChecker
{
    public static final float ALLOW_ANYWAY_CHANCE = 0.05f;

    private static Random rng = new Random();
    
    private SpamLoadChecker() { }
    
    /**
     * Call this to determine if session request should be rejected for load too high.
     *
     * @param logger a <code>Logger</code> used to log the warning if the load is too high
     * @return a <code>boolean</code> value
     */
    public static boolean reject(int activeCount, Logger logger, int scanLimit, float loadLimit)
    {
        LoadAvg la = LoadAvg.get();
        float oneMinLA = la.getOneMin();
        if (activeCount >= scanLimit) {
            logger.warn("Too many concurrent scans: " + activeCount);
            return true;
        }
        if (oneMinLA >= loadLimit) {
            if ( rng.nextFloat() < ALLOW_ANYWAY_CHANCE ) {
                logger.warn("Load too high, but allowing anyway: " + la);
                return false;
            }
            logger.warn("Load too high: " + la);
            return true;
        }
        return false;
    }
}
