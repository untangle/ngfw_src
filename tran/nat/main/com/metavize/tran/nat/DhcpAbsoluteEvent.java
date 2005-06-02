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

package com.metavize.tran.nat;

import java.util.List;
import java.util.LinkedList;

import java.io.Serializable;

import com.metavize.mvvm.logging.LogEvent;

/**
 * Log event for a DHCP absolute event .
 *
 * @author <a href="mailto:rbscott@metavize.com">Robert Scott</a>
 * @version 1.0
 * @hibernate.class
 * table="TR_NAT_EVT_DHCP_ABS"
 * mutable="false"
 */
public class DhcpAbsoluteEvent extends LogEvent implements Serializable
{
    private Long id;
    private List absoluteLeaseList = null;

    /**
     * Hibernate constructor 
     */
    public DhcpAbsoluteEvent()
    {
    }

    public DhcpAbsoluteEvent( List s )
    {
        absoluteLeaseList = s;
    }

    /**
     * @hibernate.id
     * column="EVENT_ID"
     * generator-class="native"
     */
    protected Long getId()
    {
        return id;
    }

    protected void setId(Long id)
    {
        this.id = id;
    }

    /**
     * List of the leases assocaited with the event.
     *
     * @return the list of the redirect rules.
     * @hibernate.list
     * cascade="all-delete-orphan"
     * table="TR_NAT_EVT_DHCP_ABS_LEASES"
     * @hibernate.collection-key
     * column="EVENT_ID"
     * @hibernate.collection-index
     * column="POSITION"
     * @hibernate.collection-many-to-many
     * class="com.metavize.tran.nat.DhcpAbsoluteLease"
     * column="LEASE_ID"
     */
    public List getAbsoluteLeaseList()
    {
        return absoluteLeaseList;
    }
    
    public void setAbsoluteLeaseList( List s ) 
    { 
        absoluteLeaseList = s;
    }

    void addAbsoluteLease( DhcpAbsoluteLease lease )
    {
        if ( absoluteLeaseList == null ) {
            absoluteLeaseList = new LinkedList();
        }
        
        absoluteLeaseList.add( lease );
    }
}
