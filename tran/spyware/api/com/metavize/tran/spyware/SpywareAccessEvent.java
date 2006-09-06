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

package com.metavize.tran.spyware;

import com.metavize.mvvm.tran.IPMaddr;
import com.metavize.mvvm.tran.PipelineEndpoints;

/**
 * Log event for a spyware hit.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="TR_SPYWARE_EVT_ACCESS"
 * mutable="false"
 */
public class SpywareAccessEvent extends SpywareEvent
{
    private PipelineEndpoints pipelineEndpoints;
    private String identification;
    private IPMaddr ipMaddr; // location
    private boolean blocked;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public SpywareAccessEvent() { }

    public SpywareAccessEvent(PipelineEndpoints pe,
                              String identification,
                              IPMaddr ipMaddr,
                              boolean blocked)
    {
        this.pipelineEndpoints = pe;
        this.identification = identification;
        this.ipMaddr = ipMaddr;
        this.blocked = blocked;
    }

    // SpywareEvent methods ---------------------------------------------------

    public String getType()
    {
        return "Access";
    }

    public String getReason()
    {
        return "in Subnet List";
    }

    public String getLocation()
    {
        return ipMaddr.toString();
    }

    // accessors --------------------------------------------------------------

    /**
     * Get the PipelineEndpoints.
     *
     * @return the PipelineEndpoints.
     * @hibernate.many-to-one
     * column="PL_ENDP_ID"
     * not-null="true"
     * cascade="all"
     */
    public PipelineEndpoints getPipelineEndpoints()
    {
        return pipelineEndpoints;
    }

    public void setPipelineEndpoints(PipelineEndpoints pipelineEndpoints)
    {
        this.pipelineEndpoints = pipelineEndpoints;
    }

    /**
     * An address or subnet.
     *
     * @return the IPMaddr.
     * @hibernate.property
     * type="com.metavize.mvvm.type.IPMaddrUserType"
     * @hibernate.column
     * name="IPMADDR"
     * sql-type="inet"
     */
    public IPMaddr getIpMaddr()
    {
        return ipMaddr;
    }

    public void setIpMaddr(IPMaddr ipMaddr)
    {
        this.ipMaddr = ipMaddr;
    }

    /**
     * The identification (domain matched)
     *
     * @return the protocl name.
     * @hibernate.property
     * column="IDENT"
     */
    public String getIdentification()
    {
        return identification;
    }

    public void setIdentification(String identification)
    {
        this.identification = identification;
    }

    /**
     * Whether or not we blocked it.
     *
     * @return whether or not the session was blocked (closed)
     * @hibernate.property
     * column="BLOCKED"
     */
    public boolean isBlocked()
    {
        return blocked;
    }

    public void setBlocked(boolean blocked)
    {
        this.blocked = blocked;
    }

    // Syslog methods ---------------------------------------------------------

    // use SpywareEvent appendSyslog, getSyslogId and getSyslogPriority
}
