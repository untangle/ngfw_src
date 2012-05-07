/**
 * $Id: SystemStatEvent.java,v 1.00 2012/03/15 15:12:48 dmorris Exp $
 */
package com.untangle.uvm.logging;

/**
 * Log event for system stats.
 *
 * @author <a href="mailto:seb@untangle.com">Sebastien Delafond</a>
 * @version 1.0
 */
@SuppressWarnings("serial")
public class SystemStatEvent extends LogEvent
{
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

    @Override
    public String getDirectEventSql()
    {
        String sql =
            "INSERT INTO reports.n_server_events " +
            "(time_stamp, mem_free, mem_cache, mem_buffers, load_1, load_5, load_15, cpu_user, cpu_system, disk_total, disk_free, swap_total, swap_free) " +
            " values " + "(" + 
            "timestamp '" + new java.sql.Timestamp(getTimeStamp().getTime()) + "'" + "," +
            memFree + "," + 
            memCache + "," +
            memBuffers + "," +
            load1 + "," +
            load5  + "," +
            load15  + "," +
            cpuUser  + "," +
            cpuSystem  + "," +
            diskTotal + "," +
            diskFree + "," +
            swapTotal + "," +
            swapFree + ")";
        return sql;
    }

    public void appendSyslog(SyslogBuilder sb)
    {
        sb.startSection("info");
        sb.addField("cpuUser", String.format("%.2f", cpuUser));
        sb.addField("load", String.format("%.2f", load1));
        sb.addField("memFree", String.format("%dMB", memFree / 1000000));
        sb.addField("swapFree", String.format("%dGB", swapFree / 1000000000));
        sb.addField("diskFree", String.format("%dGB", diskFree / 1000000000));
    }

    public String getSyslogId()
    {
        return "System stat event";
    }

    public SyslogPriority getSyslogPriority()
    {
	    return SyslogPriority.INFORMATIONAL;
    }

    // Object methods ------------------------------------------------------

    public String toString()
    {
        return "SystemStatEvent";
    }
}