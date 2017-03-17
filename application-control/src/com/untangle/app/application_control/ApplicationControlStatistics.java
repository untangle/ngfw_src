/**
 * $Id: ApplicationControlStatistics.java 37269 2014-02-26 23:46:16Z dmorris $
 */

package com.untangle.app.application_control;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONString;
import org.json.JSONObject;

@SuppressWarnings("serial")
public class ApplicationControlStatistics implements java.io.Serializable, JSONString
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

    // functions to retrieve the session counters

    public long getSessionCount()
    {
        return (sessionCount.get());
    }

    public long getAllowedCount()
    {
        return (allowedCount.get());
    }

    public long getFlaggedCount()
    {
        return (flaggedCount.get());
    }

    public long getBlockedCount()
    {
        return (blockedCount.get());
    }

    // functions to increment the session counters

    public void IncrementSessionCount()
    {
        sessionCount.incrementAndGet();
    }

    public void IncrementAllowedCount()
    {
        allowedCount.incrementAndGet();
    }

    public void IncrementFlaggedCount()
    {
        flaggedCount.incrementAndGet();
    }

    public void IncrementBlockedCount()
    {
        blockedCount.incrementAndGet();
    }

    // functions used to get and set the rule counts displayed in the UI

    public long getProtoTotalCount()
    {
        return (protoTotalCount);
    }

    public void setProtoTotalCount(long value)
    {
        protoTotalCount = value;
    }

    public long getProtoFlagCount()
    {
        return (protoFlagCount);
    }

    public void setProtoFlagCount(long value)
    {
        protoFlagCount = value;
    }

    public long getProtoBlockCount()
    {
        return (protoBlockCount);
    }

    public void setProtoBlockCount(long value)
    {
        protoBlockCount = value;
    }

    public long getProtoTarpitCount()
    {
        return (protoTarpitCount);
    }

    public void setProtoTarpitCount(long value)
    {
        protoTarpitCount = value;
    }

    public long getLogicTotalCount()
    {
        return (logicTotalCount);
    }

    public void setLogicTotalCount(long value)
    {
        logicTotalCount = value;
    }

    public long getLogicLiveCount()
    {
        return (logicLiveCount);
    }

    public void setLogicLiveCount(long value)
    {
        logicLiveCount = value;
    }

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
