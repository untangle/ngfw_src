package com.untangle.app.web_cache; // IMPL

import java.util.TimerTask;
import org.apache.log4j.Logger;
import com.untangle.uvm.util.LoadAvg;

public class WebCacheTimer extends TimerTask
{
    private final Logger logger = Logger.getLogger(getClass());
    private final WebCacheApp node;

    private long lastHitCount = 0;
    private long lastMissCount = 0;
    private long lastBypassCount = 0;
    private long lastSystemCount = 0;
    private long lastHitBytes = 0;
    private long lastMissBytes = 0;

    public WebCacheTimer(WebCacheApp node)
    {
        this.node = node;
    }

    public void run()
    {
        CaptureStatistics();
        CheckSystemLoad();
    }

    private void CaptureStatistics()
    {
        // get the current values from the statistics object
        long currHitCount = node.statistics.getHitCount();
        long currMissCount = node.statistics.getMissCount();
        long currBypassCount = node.statistics.getBypassCount();
        long currSystemCount = node.statistics.getSystemCount();
        long currHitBytes = node.statistics.getHitBytes();
        long currMissBytes = node.statistics.getMissBytes();

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
        WebCacheEvent event = new WebCacheEvent(node.getAppSettings().getPolicyId(),diffHitCount,diffMissCount,diffBypassCount,diffSystemCount,diffHitBytes,diffMissBytes);
        node.logEvent(event);
        logger.debug("WebCacheTimer.CaptureStatistics() =" + event.toString());

        // update our last value with the current values
        lastHitCount = currHitCount;
        lastMissCount = currMissCount;
        lastBypassCount = currBypassCount;
        lastSystemCount = currSystemCount;
        lastHitBytes = currHitBytes;
        lastMissBytes = currMissBytes;
    }

    private void CheckSystemLoad()
    {
        LoadAvg la = LoadAvg.get();
        float nowval = la.getOneMin();
        float maxval = node.getSettings().getLoadLimit().floatValue();

            // see if we should activate bypass on high load
            if ((nowval > maxval) && (node.highLoadBypass == false))
            {
            logger.info("High load bypass flag activated on load(" + nowval + ") limit(" + maxval + ")");
            node.highLoadBypass = true;
            return;
            }

            // see if we should clear bypass on reduced load
            if ((nowval < maxval) && (node.highLoadBypass == true))
            {
            logger.info("High load bypass flag cleared on load(" + nowval + ") limit(" + maxval + ")");
            node.highLoadBypass = false;
            return;
            }

        logger.debug("WebCacheTimer.CheckSystemLoad() NOW(" + nowval + ") LIMIT(" + maxval + ") BYPASS(" + node.highLoadBypass + ")");
    }
}
