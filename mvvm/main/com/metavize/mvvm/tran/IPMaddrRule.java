/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id: IPMaddrRule.java,v 1.5 2005/02/01 09:59:22 amread Exp $
 */

package com.metavize.mvvm.tran;

/**
 * Rule for matching based on IP addresses and subnets.
 *
 * @author <a href="mailto:amread@metavize.com">Aaron Read</a>
 * @version 1.0
 * @hibernate.class
 * table="IPMADDR_RULE"
 */
public class IPMaddrRule extends Rule
{
    private IPMaddr ipMaddr;

    // constructors -----------------------------------------------------------

    /**
     * Hibernate constructor.
     */
    public IPMaddrRule() { }

    public IPMaddrRule(IPMaddr ipMaddr, String name, String description)
    {
        super(name, description);
        this.ipMaddr = ipMaddr;
    }

    // accessors --------------------------------------------------------------

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
