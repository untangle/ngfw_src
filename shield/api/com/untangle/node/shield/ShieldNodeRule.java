/*
 * $HeadURL$
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

package com.untangle.node.shield;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.IPAddress;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.Rule;


/**
 * Rule for shield node settings.
 *
 * @author <a href="mailto:rbscott@untangle.com">Robert Scott</a>
 * @version 1.0
 */
@Entity
@Table(name="n_shield_node_rule", schema="settings")
@SuppressWarnings("serial")
public class ShieldNodeRule extends Rule 
{

    /* ip address this is configuring */
    private IPAddress address;

    /* Netmask that this rule applies to */
    private IPAddress netmask;

    /* divider for this rule (between0 and whatever, not inclusive) */
    private float divider = DIVIDER_VALUES[0];

    /* Enumeration of all of the possible dividers */
    private static final String DIVIDER_ENUMERATION[];
    private static final float  DIVIDER_VALUES[];

    private static final Map<Float,String> DIVIDER_MAP_FLOAT_TO_STRING = new HashMap<Float,String>();
    private static final Map<String,Float> DIVIDER_MAP_STRING_TO_FLOAT = new HashMap<String,Float>();

    /* Hibernate constructor */
    public ShieldNodeRule() { }

    public ShieldNodeRule(boolean isLive, IPAddress address, IPAddress netmask,
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
    @Type(type="com.untangle.uvm.type.IPAddressUserType")
    public IPAddress getAddress()
    {
        return this.address;
    }

    public void setAddress(IPAddress address)
    {
        this.address = address;
    }

    public void setAddress(String addressString)
        throws UnknownHostException, ParseException
    {
        setAddress(IPAddress.parse(addressString));
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
    @Type(type="com.untangle.uvm.type.IPAddressUserType")
    public IPAddress getNetmask()
    {
        return this.netmask;
    }

    public void setNetmask(IPAddress netmask)
    {
        this.netmask = netmask;
    }

    public void setNetmask(String netmaskString)
        throws UnknownHostException, ParseException
    {
        setNetmask(IPAddress.parse(netmaskString));
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
        String dividerString = DIVIDER_MAP_FLOAT_TO_STRING
            .get(this.divider);
        if (dividerString == null) {
            this.divider  = DIVIDER_VALUES[0];
            dividerString = DIVIDER_ENUMERATION[0];
        }

        return dividerString;
    }

    public void setDivider(String divider)
    {
        Float dividerValue = DIVIDER_MAP_STRING_TO_FLOAT.get(divider);
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

    @Override
    public void update(Rule rule) {
    	super.update(rule);
    	if (rule instanceof ShieldNodeRule) {
			ShieldNodeRule shieldNodeRule = (ShieldNodeRule) rule;
	        this.address = shieldNodeRule.address;
	        this.netmask = shieldNodeRule.netmask;
	        this.divider = shieldNodeRule.divider;
		}
    }
    
    static
    {
        DIVIDER_ENUMERATION = new String[] {
            "5 users",
            "25 users",
            "50 users",
            "100 users",
            "unlimited"
        };

        /* A little bit of a tapering off starting at 50 for 100
         * (capped in netcap to the value NC_SHIELD_DIVIDER_MAX */
        DIVIDER_VALUES = new float[] { 5.0f, 25.0f, 40.0f, 75.0f, -1.0f };

        for (int c = 0 ; c < DIVIDER_ENUMERATION.length ; c++) {
            DIVIDER_MAP_FLOAT_TO_STRING.put(DIVIDER_VALUES[c],
                                            DIVIDER_ENUMERATION[c]);
            DIVIDER_MAP_STRING_TO_FLOAT.put(DIVIDER_ENUMERATION[c],
                                            DIVIDER_VALUES[c]);
        }
    }
}
