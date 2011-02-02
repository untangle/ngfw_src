/*
 * $HeadURL: svn://chef/work/src/uvm/impl/com/untangle/uvm/user/SystemStatEvent.java $
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.logging;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Log event for system stats.
 *
 * @author <a href="mailto:seb@untangle.com">Sebastien Delafond</a>
 * @version 1.0
 */
@Entity
@org.hibernate.annotations.Entity(mutable=false)
    @Table(name="n_server_evt", schema="events")
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

        // constructors --------------------------------------------------------
        public SystemStatEvent() { }

        // accessors -----------------------------------------------------------
	/**
	 * Get the <code>MemFree</code> value.
	 *
	 * @return an <code>long</code> value
	 */
        @Column(name="mem_free")
	public long getMemFree() {
	    return memFree;
	}

	/**
	 * Set the <code>MemFree</code> value.
	 *
	 * @param newMemFree The new MemFree value.
	 */
	public void setMemFree(long newMemFree) {
	    this.memFree = newMemFree;
	}

	/**
	 * Get the <code>MemCache</code> value.
	 *
	 * @return an <code>long</code> value
	 */
        @Column(name="mem_cache")
	public long getMemCache() {
	    return memCache;
	}

	/**
	 * Set the <code>MemCache</code> value.
	 *
	 * @param newMemCache The new MemCache value.
	 */
	public void setMemCache(long newMemCache) {
	    this.memCache = newMemCache;
	}

	/**
	 * Get the <code>MemBuffers</code> value.
	 *
	 * @return an <code>long</code> value
	 */
        @Column(name="mem_buffers")
	public long getMemBuffers() {
	    return memBuffers;
	}

	/**
	 * Set the <code>MemBuffers</code> value.
	 *
	 * @param newMemBuffers The new MemBuffers value.
	 */
	public void setMemBuffers(long newMemBuffers) {
	    this.memBuffers = newMemBuffers;
	}

	/**
	 * Get the <code>Load1</code> value.
	 *
	 * @return an <code>float</code> value
	 */
        @Column(name="load_1")
	public float getLoad1() {
	    return load1;
	}

	/**
	 * Set the <code>Load1</code> value.
	 *
	 * @param newLoad1 The new Load1 value.
	 */
	public void setLoad1(float newLoad1) {
	    this.load1 = newLoad1;
	}

	/**
	 * Get the <code>Load5</code> value.
	 *
	 * @return an <code>float</code> value
	 */
        @Column(name="load_5")
	public float getLoad5() {
	    return load5;
	}

	/**
	 * Set the <code>Load5</code> value.
	 *
	 * @param newLoad5 The new Load5 value.
	 */
	public void setLoad5(float newLoad5) {
	    this.load5 = newLoad5;
	}

	/**
	 * Get the <code>Load15</code> value.
	 *
	 * @return an <code>float</code> value
	 */
        @Column(name="load_15")
	public float getLoad15() {
	    return load15;
	}

	/**
	 * Set the <code>Load15</code> value.
	 *
	 * @param newLoad15 The new Load15 value.
	 */
	public void setLoad15(float newLoad15) {
	    this.load15 = newLoad15;
	}

	/**
	 * Get the <code>CpuUser</code> value.
	 *
	 * @return a <code>float</code> value
	 */
        @Column(name="cpu_user")
	public float getCpuUser() {
	    return cpuUser;
	}

	/**
	 * Set the <code>CpuUser</code> value.
	 *
	 * @param newCpuUser The new CpuUser value.
	 */
	public void setCpuUser(float newCpuUser) {
	    this.cpuUser = newCpuUser;
	}

	/**
	 * Get the <code>CpuSystem</code> value.
	 *
	 * @return a <code>float</code> value
	 */
        @Column(name="cpu_system")
	public float getCpuSystem() {
	    return cpuSystem;
	}

	/**
	 * Set the <code>CpuSystem</code> value.
	 *
	 * @param newCpuSystem The new CpuSystem value.
	 */
	public void setCpuSystem(float newCpuSystem) {
	    this.cpuSystem = newCpuSystem;
	}

	/**
	 * Get the <code>DiskTotal</code> value.
	 *
	 * @return a <code>long</code> value
	 */
        @Column(name="disk_total")
	public long getDiskTotal() {
	    return diskTotal;
	}

	/**
	 * Set the <code>DiskTotal</code> value.
	 *
	 * @param newDiskTotal The new DiskTotal value.
	 */
	public void setDiskTotal(long newDiskTotal) {
	    this.diskTotal = newDiskTotal;
	}

	/**
	 * Get the <code>DiskFree</code> value.
	 *
	 * @return a <code>long</code> value
	 */
        @Column(name="disk_free")
	public long getDiskFree() {
	    return diskFree;
	}

	/**
	 * Set the <code>DiskFree</code> value.
	 *
	 * @param newDiskFree The new DiskFree value.
	 */
	public void setDiskFree(long newDiskFree) {
	    this.diskFree = newDiskFree;
	}

        /**
         * Get the <code>SwapFree</code> value.
         *
         * @return a <code>long</code> value
         */
        @Column(name="swap_free")
        public long getSwapFree() {
            return swapFree;
        }

        /**
         * Set the <code>SwapFree</code> value.
         *
         * @param newSwapFree The new SwapFree value.
         */
        public void setSwapFree(final long newSwapFree) {
            this.swapFree = newSwapFree;
        }

        /**
         * Get the <code>SwapTotal</code> value.
         *
         * @return a <code>long</code> value
         */
        @Column(name="swap_total")
        public long getSwapTotal() {
            return swapTotal;
        }

        /**
         * Set the <code>SwapTotal</code> value.
         *
         * @param newSwapTotal The new SwapTotal value.
         */
        public void setSwapTotal(final long newSwapTotal) {
            this.swapTotal = newSwapTotal;
        }

        // Syslog methods ------------------------------------------------------

        public void appendSyslog(SyslogBuilder sb)
        {
            sb.startSection("info");
        }

        @Transient
        public String getSyslogId()
        {
            return "System stat event";
        }

        @Transient
        public SyslogPriority getSyslogPriority()
        {
	    return SyslogPriority.INFORMATIONAL;
        }

        // Object methods ------------------------------------------------------

        public String toString()
        {
            return "SystemStatEvent id: " + getId();
        }
    }
