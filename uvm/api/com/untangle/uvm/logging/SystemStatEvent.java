/**
 * $Id$
 */
package com.untangle.uvm.logging;

import com.untangle.uvm.util.I18nUtil;

/**
 * Log event for system stats.
 *
 */
@SuppressWarnings("serial")
public class SystemStatEvent extends LogEvent
{
    private long memTotal;
    private long memFree;
    private long memCache;
    private long memBuffers;

    private float load1;
    private float load5;
    private float load15;

    private float cpuUser;
    private float cpuSystem;

    private long diskTotal;
    private long diskFree;

    private long swapFree;
    private long swapTotal;

    public SystemStatEvent() { }

    public long getMemTotal() { return memTotal; }
    public void setMemTotal(long newMemTotal) { this.memTotal = newMemTotal; }

    public long getMemFree() { return memFree; }
    public void setMemFree(long newMemFree) { this.memFree = newMemFree; }

    public long getMemCache() { return memCache; }
    public void setMemCache(long newMemCache) { this.memCache = newMemCache; }

    public long getMemBuffers() { return memBuffers; }
    public void setMemBuffers(long newMemBuffers) { this.memBuffers = newMemBuffers; }

    public float getLoad1() { return load1; }
    public void setLoad1(float newLoad1) { this.load1 = newLoad1; }

    public float getLoad5() { return load5; }
    public void setLoad5(float newLoad5) { this.load5 = newLoad5; }

    public float getLoad15() { return load15; }
    public void setLoad15(float newLoad15) { this.load15 = newLoad15; }

    public float getCpuUser() { return cpuUser; }
    public void setCpuUser(float newCpuUser) { this.cpuUser = newCpuUser; }

    public float getCpuSystem() { return cpuSystem; }
    public void setCpuSystem(float newCpuSystem) { this.cpuSystem = newCpuSystem; }

    public long getDiskTotal() { return diskTotal; }
    public void setDiskTotal(long newDiskTotal) { this.diskTotal = newDiskTotal; }

    public long getDiskFree() { return diskFree; }
    public void setDiskFree(long newDiskFree) { this.diskFree = newDiskFree; }

    public long getSwapFree() { return swapFree; }
    public void setSwapFree(final long newSwapFree) { this.swapFree = newSwapFree; }

    public long getSwapTotal() { return swapTotal; }
    public void setSwapTotal(final long newSwapTotal) { this.swapTotal = newSwapTotal; }

    public float getDiskFreePercent()
    {
        return ( ((float)getDiskFree()) / ((float)getDiskTotal()) );
    }

    public float getMemFreePercent()
    {
        return ( ((float)getMemFree()) / ((float)getMemTotal()) );
    }
    
    @Override
    public java.sql.PreparedStatement getDirectEventSql( java.sql.Connection conn ) throws Exception
    {
        String sql =
            "INSERT INTO reports.server_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, mem_free, mem_cache, mem_buffers, load_1, load_5, load_15, cpu_user, cpu_system, disk_total, disk_free, swap_total, swap_free) " +
            " values " +
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = conn.prepareStatement( sql );

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, memFree);
        pstmt.setLong(++i, memCache);
        pstmt.setLong(++i, memBuffers);
        pstmt.setFloat(++i, load1);
        pstmt.setFloat(++i, load5);
        pstmt.setFloat(++i, load15);
        pstmt.setFloat(++i, cpuUser);
        pstmt.setFloat(++i, cpuSystem);
        pstmt.setLong(++i, diskTotal);
        pstmt.setLong(++i, diskFree);
        pstmt.setLong(++i, swapTotal);
        pstmt.setLong(++i, swapFree);
        return pstmt;
    }

    @Override
    public String toSummaryString()
    {
        String summary = I18nUtil.marktr("The current system state is") + ": " + "[ " +
            I18nUtil.marktr("Load (1-minute)") + ": " + getLoad1() + ", " + 
            I18nUtil.marktr("Load (5-minute)") + ": " + getLoad5() + ", " +
            I18nUtil.marktr("Load (15-minute)") + ": " + getLoad15() + ", " +
            I18nUtil.marktr("Memory Free") + ": " + (getMemFree()/(1024*1024)) + "MB" + ", " +
            I18nUtil.marktr("Disk Free") + ": " + (getDiskFree()/(1024*1024)) + "MB" + ", " +
            I18nUtil.marktr("Swap Free") + ": " + (getSwapFree()/(1024*1024)) + "MB" + " ]";

        return summary;
    }
}