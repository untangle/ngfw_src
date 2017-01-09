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

    private int activeHosts;
    
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

    public long getSwapTotal() { return swapTotal; }
    public void setSwapTotal(final long newSwapTotal) { this.swapTotal = newSwapTotal; }

    public long getSwapFree() { return swapFree; }
    public void setSwapFree(final long newSwapFree) { this.swapFree = newSwapFree; }

    public int getActiveHosts() { return activeHosts; }
    public void setActiveHosts(final int newValue) { this.activeHosts = newValue; }
    
    public float getDiskFreePercent()
    {
        return ( ((float)getDiskFree()) / ((float)getDiskTotal()) );
    }

    public long getDiskUsed()
    {
        return getDiskTotal() - getDiskFree();
    }

    public float getDiskUsedPercent()
    {
        return ( ((float)getDiskUsed()) / ((float)getDiskTotal()) );
    }
    
    public float getMemFreePercent()
    {
        return ( ((float)getMemFree()) / ((float)getMemTotal()) );
    }

    public long getMemUsed()
    {
        return getMemTotal() - getMemFree();
    }

    public float getMemUsedPercent()
    {
        return ( ((float)getMemUsed()) / ((float)getMemTotal()) );
    }
    
    public float getSwapFreePercent()
    {
        return ( ((float)getSwapFree()) / ((float)getSwapTotal()) );
    }

    public long getSwapUsed()
    {
        return getSwapTotal() - getSwapFree();
    }

    public float getSwapUsedPercent()
    {
        return ( ((float)getSwapUsed()) / ((float)getSwapTotal()) );
    }
    
    @Override
    public void compileStatements( java.sql.Connection conn, java.util.Map<String,java.sql.PreparedStatement> statementCache ) throws Exception
    {
        String sql =
            "INSERT INTO " + schemaPrefix() + "server_events" + getPartitionTablePostfix() + " " +
            "(time_stamp, mem_total, mem_free, load_1, load_5, load_15, cpu_user, cpu_system, disk_total, disk_free, swap_total, swap_free, active_hosts) " +
            " values " +
            "( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )";

        java.sql.PreparedStatement pstmt = getStatementFromCache( sql, statementCache, conn );        

        int i=0;
        pstmt.setTimestamp(++i, getTimeStamp());
        pstmt.setLong(++i, memTotal);
        pstmt.setLong(++i, memFree);
        pstmt.setFloat(++i, load1);
        pstmt.setFloat(++i, load5);
        pstmt.setFloat(++i, load15);
        pstmt.setFloat(++i, cpuUser);
        pstmt.setFloat(++i, cpuSystem);
        pstmt.setLong(++i, diskTotal);
        pstmt.setLong(++i, diskFree);
        pstmt.setLong(++i, swapTotal);
        pstmt.setLong(++i, swapFree);
        pstmt.setInt(++i, activeHosts);

        pstmt.addBatch();
        return;
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
            I18nUtil.marktr("Swap Used") + ": " + (getSwapUsed()/(1024*1024)) + "MB" + " ]";

        return summary;
    }
}
