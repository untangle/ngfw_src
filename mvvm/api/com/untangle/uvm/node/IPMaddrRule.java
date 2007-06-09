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

package com.untangle.mvvm.tran;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
@Entity
@Table(name="ipmaddr_rule", schema="settings")
public class IPMaddrRule extends Rule
{
    private IPMaddr ipMaddr;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public IPMaddrRule() { }

    // XXX inconstant constuctor
    public IPMaddrRule(IPMaddr ipMaddr, String name, String category,
                       String description)
    {
        super(name, category, description);
        this.ipMaddr = ipMaddr;
    }

    // accessors --------------------------------------------------------------

    /**
     * An address or subnet.
     *
     * @return the IPMaddr.
     */
    @Type(type="com.untangle.mvvm.type.IPMaddrUserType")
    public IPMaddr getIpMaddr()
    {
        return ipMaddr;
    }

    public void setIpMaddr(IPMaddr ipMaddr)
    {
        this.ipMaddr = ipMaddr;
    }

    // Object methods ---------------------------------------------------------

    public boolean equals(Object o)
    {
        if (!(o instanceof IPMaddrRule)) {
            return false;
        }

        IPMaddrRule ir = (IPMaddrRule)o;
        return ipMaddr.equals(ir.ipMaddr);
    }

    public int hashCode()
    {
        return ipMaddr.hashCode();
    }
}
