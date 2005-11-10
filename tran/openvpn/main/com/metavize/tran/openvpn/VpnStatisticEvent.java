/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.tran.openvpn;

import com.metavize.mvvm.logging.StatisticEvent;

/**
 * Log event for a Nat statistics.
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="tr_openvpn_statistic_evt"
 * mutable="false"
 */
public class VpnStatisticEvent extends StatisticEvent
{
    // Constructors 
    private boolean hasStatistics = false;
    
    /**
     * Hibernate constructor 
     */
    public VpnStatisticEvent()
    {
    }

    void setHasStatistics()
    {
        this.hasStatistics = true;
    }
    
    /**
     * Returns true if any of the stats are non-zero, whenever all the stats are zero,
     * a new log event is not created.
     */
    public boolean hasStatistics()
    {
        /* This is just used to trigger the event logger for the client distribution events */
        if ( hasStatistics ) return true;

        return false;
    }
    
}
