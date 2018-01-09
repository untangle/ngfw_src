/**
 * $Id$
 */
package com.untangle.app.web_cache;

import java.util.TimerTask;
import org.apache.log4j.Logger;
import com.untangle.uvm.util.LoadAvg;

/**
 * This timer task will periodically log the cumulative cache statistics.
 * 
 * @author mahotz
 * 
 */
public class WebCacheTimer extends TimerTask
{
    private final Logger logger = Logger.getLogger(getClass());
    private final WebCacheApp app;

    private long lastHitCount = 0;
    private long lastMissCount = 0;
    private long lastBypassCount = 0;
    private long lastSystemCount = 0;
    private long lastHitBytes = 0;
    private long lastMissBytes = 0;

    /**
     * Constructor
     * 
     * @param app
     *        The application that created us
     */
    public WebCacheTimer(WebCacheApp app)
    {
        this.app = app;
    }

    /**
     * Our main run function
     */
    public void run()
    {
        CaptureStatistics();
        CheckSystemLoad();
    }

    /**
     * Function to calculate and log cache statistics
     */
    private void CaptureStatistics()
    {
        // get the current values from the statistics object
        long currHitCount = app.statistics.getHitCount();
        long currMissCount = app.statistics.getMissCount();
        long currBypassCount = app.statistics.getBypassCount();
        long currSystemCount = app.statistics.getSystemCount();
        long currHitBytes = app.statistics.getHitBytes();
        long currMissBytes = app.statistics.getMissBytes();

        // calculate the change for each value
        long diffHitCount = (currHitCount - lastHitCount);
        long diffMissCount = (currMissCount - lastMissCount);
        long diffBypassCount = (currBypassCount - lastBypassCount);
        long diffSystemCount = (currSystemCount - lastSystemCount);
        long diffHitBytes = (currHitBytes - lastHitBytes);
        long diffMissBytes = (currMissBytes - lastMissBytes);

        // if nothing has changed we don't need to log an event
        if ((diffHitCount | diffMissCount | diffBypassCount | diffSystemCount | diffHitBytes | diffMissBytes) == 0) return;

        // counters have increased so write the log event
        WebCacheEvent event = new WebCacheEvent(app.getAppSettings().getPolicyId(), diffHitCount, diffMissCount, diffBypassCount, diffSystemCount, diffHitBytes, diffMissBytes);
        app.logEvent(event);
        logger.debug("WebCacheTimer.CaptureStatistics() =" + event.toString());

        // update our last value with the current values
        lastHitCount = currHitCount;
        lastMissCount = currMissCount;
        lastBypassCount = currBypassCount;
        lastSystemCount = currSystemCount;
        lastHitBytes = currHitBytes;
        lastMissBytes = currMissBytes;
    }

    /**
     * When the system load goes above a configured threshold, we disable
     * caching to eliminate our impact on performance. When it comes back down
     * we enable caching again.
     */
    private void CheckSystemLoad()
    {
        LoadAvg la = LoadAvg.get();
        float nowval = la.getOneMin();
        float maxval = app.getSettings().getLoadLimit().floatValue();

        // see if we should activate bypass on high load
        if ((nowval > maxval) && (app.highLoadBypass == false)) {
            logger.info("High load bypass flag activated on load(" + nowval + ") limit(" + maxval + ")");
            app.highLoadBypass = true;
            return;
        }

        // see if we should clear bypass on reduced load
        if ((nowval < maxval) && (app.highLoadBypass == true)) {
            logger.info("High load bypass flag cleared on load(" + nowval + ") limit(" + maxval + ")");
            app.highLoadBypass = false;
            return;
        }

        logger.debug("WebCacheTimer.CheckSystemLoad() NOW(" + nowval + ") LIMIT(" + maxval + ") BYPASS(" + app.highLoadBypass + ")");
    }
}
