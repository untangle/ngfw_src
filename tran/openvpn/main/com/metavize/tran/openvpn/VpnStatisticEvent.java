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

import java.util.Date;

import com.metavize.mvvm.logging.StatisticEvent;
import com.metavize.mvvm.logging.SyslogBuilder;
import com.metavize.mvvm.logging.SyslogPriority;

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
    private Date start;
    private Date end;
    
    private long bytesTx = 0;
    private long bytesRx = 0;

    /**
     * Hibernate constructor
     */
    public VpnStatisticEvent()
    {
    }

    /**
     * Time the session started.
     *
     * @return time logged.
     * @hibernate.property
     * column="start_time"
     */
    public Date getStart()
    {
        return this.start;
    }
    
    void setStart( Date start )
    {
        this.start = start;
    }

    /**
     * Time the session ended.
     *
     * @return time logged.
     * @hibernate.property
     * column="end_time"
     */
    public Date getEnd()
    {
        return this.end;
    }

    void setEnd( Date end )
    {
        this.end = end;
    }

    /**
     * Total bytes received during this session.
     *
     * @return time logged.
     * @hibernate.property
     * column="rx_bytes"
     */
    public long getBytesRx()
    {
        return this.bytesRx;
    }

    void setBytesRx( long bytesRx )
    {
        this.bytesRx = bytesRx;
    }

    void incrBytesRx( long bytesRx )
    {
        this.bytesRx += bytesRx;
    }

    
    /**
     * Total transmitted received during this session.
     *
     * @return time logged.
     * @hibernate.property
     * column="tx_bytes"
     */
    public long getBytesTx()
    {
        return this.bytesTx;
    }

    void setBytesTx( long bytesTx )
    {
        this.bytesTx = bytesTx;
    }

    void incrBytesTx( long bytesTx )
    {
        this.bytesTx += bytesTx;
    }

    public boolean hasStatistics()
    {
        return ( this.bytesTx > 0 || this.bytesRx > 0 );
    }


    // Syslog methods ---------------------------------------------------------

    public void appendSyslog(SyslogBuilder sb)
    {
    }

    public SyslogPriority getSyslogPrioritiy()
    {
        return SyslogPriority.DEBUG;
    }
}
