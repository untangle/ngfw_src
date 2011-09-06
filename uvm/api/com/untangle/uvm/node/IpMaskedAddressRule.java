/*
 * $Id: IpMaskedAddressRule.java,v 1.00 2011/09/06 15:08:50 dmorris Exp $
 */
package com.untangle.uvm.node;

@SuppressWarnings("serial")
public class IpMaskedAddressRule extends BaseRule implements java.io.Serializable
{
    IPMaskedAddress address = null;
    
    public IpMaskedAddressRule() {}

    public IpMaskedAddressRule(IPMaskedAddress address, String name, String category, String description, boolean enabled, boolean blocked, boolean flagged)
    {
        super(name, category, description, enabled, blocked, flagged);
        this.address = address;
    }

    public IpMaskedAddressRule(IPMaskedAddress address, String name, String category, String description, boolean enabled)
    {
        super(name, category, description, enabled);
        this.address = address;
    }

    public IpMaskedAddressRule(IPMaskedAddress address, boolean enabled)
    {
        super(enabled);
        this.address = address;
    }

    public IPMaskedAddress getIpMaskedAddress()
    {
        return this.address;
    }

    public void setAddress(IPMaskedAddress Address)
    {
        this.address = address;
    }

    public String getAddress()
    {
        return this.address.toString();
    }

    public void setAddress(String addrStr)
    {
        this.address = new IPMaskedAddress(addrStr);
    }
    
    
}