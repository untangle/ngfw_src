package com.untangle.node.cpd;

import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.IPMatcher;

public class PassedAddress extends Rule
{
    private IPMatcher address = IPMatcher.getNilMatcher();
    
    public IPMatcher getAddress()
    {
        return this.address;
    }

    public void setAddress( IPMatcher newValue )
    {
        this.address = newValue;
    }
}
