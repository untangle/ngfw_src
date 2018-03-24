/**
 * $Id: ApplicationControlStatistics.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.application_control;

import java.util.concurrent.atomic.AtomicLong;

import java.io.Serializable;
import org.json.JSONString;
import org.json.JSONObject;

/**
 * Class to store cumulative traffic statistics
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class ApplicationControlStatistics implements Serializable, JSONString
{
    // counters for sessions, allowed, flagged, and blocked
    private AtomicLong sessionCount = new AtomicLong();
    private AtomicLong allowedCount = new AtomicLong();
    private AtomicLong flaggedCount = new AtomicLong();
    private AtomicLong blockedCount = new AtomicLong();

    private long protoTotalCount;
    private long protoFlagCount;
    private long protoBlockCount;
    private long protoTarpitCount;
    private long logicTotalCount;
    private long logicLiveCount;

    ApplicationControlStatistics()
    {
        protoTotalCount = 0;
        protoFlagCount = 0;
        protoBlockCount = 0;
        protoTarpitCount = 0;
        logicTotalCount = 0;
        logicLiveCount = 0;
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public long getSessionCount() { return (sessionCount.get()); }
    public void IncrementSessionCount() { sessionCount.incrementAndGet(); }

    public long getAllowedCount() { return (allowedCount.get()); }
    public void IncrementAllowedCount() { allowedCount.incrementAndGet(); }

    public long getFlaggedCount() { return (flaggedCount.get()); }
    public void IncrementFlaggedCount() { flaggedCount.incrementAndGet(); }
    
    public long getBlockedCount() { return (blockedCount.get()); }
    public void IncrementBlockedCount() { blockedCount.incrementAndGet(); }

    public long getProtoTotalCount() { return (protoTotalCount); }
    public void setProtoTotalCount(long value) { protoTotalCount = value; }

    public long getProtoFlagCount() { return (protoFlagCount); }
    public void setProtoFlagCount(long value) { protoFlagCount = value; }

    public long getProtoBlockCount() { return (protoBlockCount); }
    public void setProtoBlockCount(long value) { protoBlockCount = value; }

    public long getProtoTarpitCount() { return (protoTarpitCount); }
    public void setProtoTarpitCount(long value) { protoTarpitCount = value; }

    public long getLogicTotalCount() { return (logicTotalCount); }
    public void setLogicTotalCount(long value) { logicTotalCount = value; }

    public long getLogicLiveCount() { return (logicLiveCount); }
    public void setLogicLiveCount(long value) { logicLiveCount = value; }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
