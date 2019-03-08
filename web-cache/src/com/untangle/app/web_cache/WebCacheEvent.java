/**
 * $Id$
 */

package com.untangle.app.web_cache;

import java.io.Serializable;

import com.untangle.uvm.logging.LogEvent;
import com.untangle.uvm.util.I18nUtil;

/**
 * Class to manage the details of web cache events which are logged to the
 * database periodically to capture cache statistics.
 * 
 * @author mahotz
 * 
 */
@SuppressWarnings("serial")
public class WebCacheEvent extends LogEvent implements Serializable
{
    private long hitCount;
    private long missCount;
    private long bypassCount;
    private long systemCount;
    private long hitBytes;
    private long missBytes;
    private long policyId;

    public WebCacheEvent()
    {
        hitCount = missCount = bypassCount = systemCount = hitBytes = missBytes = policyId = 0;
    }

    public WebCacheEvent(Integer policyId, long hitCount, long missCount, long bypassCount, long systemCount, long hitBytes, long missBytes)
    {
        this.hitCount = hitCount;
        this.missCount = missCount;
        this.bypassCount = bypassCount;
        this.systemCount = systemCount;
        this.hitBytes = hitBytes;
        this.missBytes = missBytes;
        this.policyId = policyId;
    }

    // THIS IS FOR ECLIPSE - @formatter:off

    public long getHitCount() { return(hitCount); }
    public void setHitCount( long hitCount ) { this.hitCount = hitCount; }

    public long getMissCount() { return(missCount); }
    public void setMissCount( long missCount ) { this.missCount = missCount; }

    public long getBypassCount() { return(bypassCount); }
    public void setBypassCount( long bypassCount  ) { this.bypassCount = bypassCount; }

    public long getSystemCount() { return(systemCount); }
    public void setSystemCount( long systemCount ) { this.systemCount = hitCount; }

    public long getHitBytes() { return(hitBytes); }
    public void setHitBytes( long hitBytes ) { this.hitBytes = hitBytes; }

    public long getMissBytes() { return(missBytes); }
    public void setMissBytes( long missBytes ) { this.missBytes = missBytes; }

    public Long getPolicyId() { return(policyId); }
    public void setPolicyId( Integer policyId ) { this.policyId = policyId; }

    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql = "INSERT INTO " + schemaPrefix() + "web_cache_stats" + getPartitionTablePostfix() + " " +
            "(time_stamp, hits, misses, bypasses, systems, hit_bytes, miss_bytes) " + 
            "values " +
            "( ?, ?, ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, getHitCount());
        pstmt.setLong(++i, getMissCount());
        pstmt.setLong(++i, getBypassCount());
        pstmt.setLong(++i, getSystemCount());
        pstmt.setLong(++i, getHitBytes());
        pstmt.setLong(++i, getMissBytes());

        pstmt.addBatch();
        return;
    }

    // THIS IS FOR ECLIPSE - @formatter:on

    public String toString()
    {
        String detail = new String();
        detail += ("WebCacheEvent(");
        detail += (" hitCount:" + hitCount);
        detail += (" missCount:" + missCount);
        detail += (" bypassCount:" + bypassCount);
        detail += (" systemCount:" + systemCount);
        detail += (" hitBytes:" + hitBytes);
        detail += (" missBytes:" + missBytes);
        detail += (")");
        return detail;
    }

    @Override
    public String toSummaryString()
    {
        String summary = "Web Cache " + I18nUtil.marktr("cached") + " " + getHitCount() + " " + I18nUtil.marktr("pages");
        return summary;
    }
}
