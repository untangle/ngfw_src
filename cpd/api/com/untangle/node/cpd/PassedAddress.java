package com.untangle.node.cpd;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.ip.IPSimpleMatcher;

@MappedSuperclass
public abstract class PassedAddress extends Rule {
    private static final long serialVersionUID = 3383783136717843470L;
    private IPDBMatcher address = IPSimpleMatcher.getNilMatcher();
    
    @Column(name="address", nullable=false)
    public IPDBMatcher getAddress()
    {
        return this.address;
    }

    public void setAddress( IPDBMatcher newValue )
    {
        this.address = newValue;
    }

}
