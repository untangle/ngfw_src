/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: AddressMap.java 194 2005-04-06 19:13:55Z rbscott $
 */

package com.metavize.mvvm.reporting;

import java.io.Serializable;
import java.sql.Timestamp;
import java.net.InetAddress;

/**
 * Describes the address map, a temporary table built during report generation
 *
 * There is guaranteed to be only one name for a given address and time.
 *
 * @author
 * @version 1.0
 * @hibernate.class
 * table="address_map"
 */
public class AddressMap implements Serializable
{
    private static final long serialVersionUID = -6231213396376584446L;

    private Long id;
    private InetAddress address;
    private String name;
    private Timestamp startTime;
    private Timestamp endTime;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate settings.
     */
    public AddressMap() { }

    public AddressMap(InetAddress address, String name, Timestamp startTime)
    {
        this.address = address;
        this.name = name;
        this.startTime = startTime;
    }

    // accessors --------------------------------------------------------------

    /**
     * @hibernate.id
     * column="UPGRADE_SETTINGS_ID"
     * generator-class="native"
     */
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * Returns the InetAddress that is being mapped from.
     *
     * @return the <code>InetAddress</code> for this entry
     * @hibernate.property
     * column="ADDR"
     * not-null="true"
     */
    public InetAddress getAddress()
    {
        return address;
    }

    public void setAddress(InetAddress address)
    {
        this.address = address;
    }

    /**
     * Returns the name that the address is mapped to
     *
     * @return the name for this entry
     * @hibernate.property
     * column="NAME"
     */
    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * Returns the first datetime at which this entry is valid.
     *
     * @return the <code>Timestamp</code> at which point this entry becomes valid
     * @hibernate.property
     * column="START_TIME"
     */
    public Timestamp getStartTime()
    {
        return startTime;
    }

    public void setStartTime(Timestamp startTime)
    {
        this.startTime = startTime;
    }

    /**
     * Returns the first datetime at which this entry is valid.
     *
     * @return the <code>Timestamp</code> at which point this entry becomes invalid
     * @hibernate.property
     * column="END_TIME"
     */
    public Timestamp getEndTime()
    {
        return endTime;
    }

    public void setEndTime(Timestamp endTime)
    {
        this.endTime = endTime;
    }
}
