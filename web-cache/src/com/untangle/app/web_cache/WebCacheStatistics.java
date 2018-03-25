/**
 * $Id$
 */
package com.untangle.app.web_cache;

import java.util.concurrent.atomic.AtomicLong;

import org.json.JSONString;
import org.json.JSONObject;

/**
 * This class is used to periodically log the web cache statistics.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class WebCacheStatistics implements java.io.Serializable, JSONString
{
    private AtomicLong hitCount = new AtomicLong();
    private AtomicLong missCount = new AtomicLong();
    private AtomicLong bypassCount = new AtomicLong();
    private AtomicLong systemCount = new AtomicLong();

    private AtomicLong hitBytes = new AtomicLong();
    private AtomicLong missBytes = new AtomicLong();

    // THIS IS FOR ECLIPSE - @formatter:off

    public long getHitCount() { return (hitCount.get()); }
    public long getMissCount() { return (missCount.get()); }
    public long getBypassCount() { return (bypassCount.get()); }
    public long getSystemCount() { return (systemCount.get()); }
    public long getHitBytes() { return (hitBytes.get()); }
    public long getMissBytes() { return (missBytes.get()); }

    public void AddHitBytes(long argValue) { hitBytes.addAndGet(argValue); }
    public void AddMissBytes(long argValue) { missBytes.addAndGet(argValue); }
    public void IncHitCount() { hitCount.incrementAndGet(); }
    public void IncMissCount() { missCount.incrementAndGet(); }
    public void IncBypassCount() { bypassCount.incrementAndGet(); }
    public void IncSystemCount() { systemCount.incrementAndGet(); }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }
}
