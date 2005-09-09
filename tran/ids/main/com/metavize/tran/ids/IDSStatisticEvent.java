/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: NatStatisticEvent.java 1283 2005-07-10 01:10:23Z rbscott $
 */

package com.metavize.tran.ids;

import com.metavize.mvvm.logging.StatisticEvent;

/**
 * Log event for a IDS statistics.
 *
 * @author <a href="mailto:nchilders@metavize.com">nchilders</a>
 * @stolen from rbscott yo
 * @version 1.0
 * @hibernate.class
 * table="TR_IDS_STATISTIC_EVT"
 * mutable="false"
 */

public class IDSStatisticEvent extends StatisticEvent {

    private int scanned	= 0;
    private int passed	= 0;
	private int blocked = 0;

    // Constructors 
    /**
     * Hibernate constructor 
     */
    public IDSStatisticEvent()
    {
    }

    public IDSStatisticEvent( int scanned, int passed, int blocked ) 
    {
        this.scanned = scanned;
		this.passed  = passed;
		this.blocked = blocked;
    }
    
    /**
     * Number of scanned chunks
     *
     * @return Number of scanned chunks
     * @hibernate.property
     * column="IDS_SCANNED"
     */
    public int getScanned() { return scanned; }
    public void setScanned( int scanned ) { this.scanned = scanned; }
    public void incrScanned() { this.scanned++; }

    /**
     * Number of passed chunks
     *
     * @return Number of matched chunks
     * @hibernate.property
     * column="IDS_PASSED"
     */

	public int getPassed() { return passed; }
	public void setPassed(int passed) { this.passed = passed; }
	public void incrPassed() { this.passed++; }

	/**
	 * Number of blocked chunks
	 * 
	 * @return Number of blocked chunks
     * @hibernate.property
	 * column="IDS_BLOCKED"
	 */

	public int getBlocked() { return blocked; }
	public void setBlocked(int blocked) { this.blocked = blocked; }
	public void incrBlocked() { this.blocked++; }
			

    /**
     * Returns true if any of the stats are non-zero, whenever all the stats are zero,
     * a new log event is not created.
     */
    public boolean hasStatistics() { return ((scanned + passed + blocked) > 0 ); }
    
}
