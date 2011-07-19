/*
 * $Id$
 */
package com.untangle.uvm.node;

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
@Table(name="u_ipmaddr_rule", schema="settings")
@SuppressWarnings("serial")
public class IPMaskedAddressRule extends Rule
{
    private IPMaskedAddress ipMaddr;

    /**
     * Hibernate constructor.
     */
    public IPMaskedAddressRule() { }

    public IPMaskedAddressRule(IPMaskedAddress ipMaddr, String name, String category, String description)
    {
        super(name, category, description);
        this.ipMaddr = ipMaddr;
    }

    /**
     * An address or subnet.
     *
     * @return the IPMaskedAddress.
     */
    @Type(type="com.untangle.uvm.type.IPMaskedAddressUserType")
    public IPMaskedAddress getIpMaddr()
    {
        return ipMaddr;
    }

    public void setIpMaddr(IPMaskedAddress ipMaddr)
    {
        this.ipMaddr = ipMaddr;
    }

    @Override
    public void update(Rule rule)
    {
    	super.update(rule);
    	if (rule instanceof IPMaskedAddressRule) {
    		IPMaskedAddressRule ipMaddrRule = (IPMaskedAddressRule) rule;
			this.ipMaddr = ipMaddrRule.ipMaddr;
		}
    }    
}
