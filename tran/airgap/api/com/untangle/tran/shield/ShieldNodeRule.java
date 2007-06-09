/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.tran.airgap;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import com.untangle.mvvm.shield.ShieldNodeSettings;
import com.untangle.mvvm.tran.IPaddr;
import com.untangle.mvvm.tran.ParseException;
import com.untangle.mvvm.tran.Rule;
import org.hibernate.annotations.Type;


/**
 * Rule for shield node settings.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="tr_airgap_shield_node_rule", schema="settings")
public class ShieldNodeRule extends Rule implements ShieldNodeSettings
{
    private static final long serialVersionUID = -6928365798856031269L;

    /* ip address this is configuring */
    private IPaddr address;

    /* Netmask that this rule applies to */
    private IPaddr netmask;

    /* divider for this rule (between0 and whatever, not inclusive) */
    private float divider = DIVIDER_VALUES[0];

    /* Enumeration of all of the possible dividers */
    private static final String DIVIDER_ENUMERATION[];
    private static final float  DIVIDER_VALUES[];

    private static final Map DIVIDER_MAP_FLOAT_TO_STRING = new HashMap();
    private static final Map DIVIDER_MAP_STRING_TO_FLOAT = new HashMap();

    /* Hibernate constructor */
    public ShieldNodeRule() { }

    public ShieldNodeRule(boolean isLive, IPaddr address, IPaddr netmask,
                          float divider, String category, String description)
    {
        setLive(isLive);
        setCategory(category);
        setDescription(description);
        this.address = address;
        this.netmask = netmask;
        this.divider = divider;
    }

    /**
     * Node being modified.
     *
     * @return the node to modify
     */
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getAddress()
    {
        return this.address;
    }

    public void setAddress(IPaddr address)
    {
        this.address = address;
    }

    public void setAddress(String addressString)
        throws UnknownHostException, ParseException
    {
        setAddress(IPaddr.parse(addressString));
    }

    @Transient
    public String getAddressString()
    {
        if (address == null || address.isEmpty()) return "";

        return address.toString();
    }


    /**
     * Netmask onto which to apply this configuration.
     *
     * @return the netmask
     */
    @Type(type="com.untangle.mvvm.type.IPaddrUserType")
    public IPaddr getNetmask()
    {
        return this.netmask;
    }

    public void setNetmask(IPaddr netmask)
    {
        this.netmask = netmask;
    }

    public void setNetmask(String netmaskString)
        throws UnknownHostException, ParseException
    {
        setNetmask(IPaddr.parse(netmaskString));
    }

    @Transient
    public String getNetmaskString()
    {
        if (netmask == null || netmask.isEmpty()) return "";

        return netmask.toString();
    }

    /**
     * Divider up to which this applies, 0 is the highest and not recommended.
     *
     * @return the port to redirect to.
     */
    @Column(nullable=false)
    public float getDivider()
    {
        /* This will "fix" dividers that have values that are not the
         * enumeration */
        getDividerString();
        return this.divider;
    }

    public void setDivider(float divider)
    {
        this.divider = divider;
        getDividerString();
    }

    /* These are all guarded to guarantee the user doesn't set to an
     * invalid value */
    @Transient
    public String getDividerString()
    {
        String dividerString = (String)DIVIDER_MAP_FLOAT_TO_STRING
            .get(this.divider);
        if (dividerString == null) {
            this.divider  = DIVIDER_VALUES[0];
            dividerString = DIVIDER_ENUMERATION[0];
        }

        return dividerString;
    }

    public void setDivider(String divider)
    {
        Float dividerValue = (Float)DIVIDER_MAP_STRING_TO_FLOAT.get(divider);
        if (dividerValue == null) {
            this.divider = DIVIDER_VALUES[0];
        } else {
            this.divider = dividerValue;
        }
    }

    @Transient
    public static String[] getDividerEnumeration()
    {
        return DIVIDER_ENUMERATION;
    }

    @Transient
    public static String getDividerDefault()
    {
        return DIVIDER_ENUMERATION[0];
    }

    static
    {
        DIVIDER_ENUMERATION = new String[] {
            "5 users",
            "25 users",
            "50 users",
            "100 users"
        };

        /* A little bit of a tapering off starting at 50 for 100
         * (capped in netcap to the value NC_SHIELD_DIVIDER_MAX */
        DIVIDER_VALUES = new float[] { 5.0f, 25.0f, 40.0f, 75.0f };

        for (int c = 0 ; c < DIVIDER_ENUMERATION.length ; c++) {
            DIVIDER_MAP_FLOAT_TO_STRING.put(DIVIDER_VALUES[c],
                                            DIVIDER_ENUMERATION[c]);
            DIVIDER_MAP_STRING_TO_FLOAT.put(DIVIDER_ENUMERATION[c],
                                            DIVIDER_VALUES[c]);
        }
    }
}
