package com.untangle.node.cpd;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.firewall.ip.IPDBMatcher;
import com.untangle.uvm.node.firewall.ip.IPSimpleMatcher;

@MappedSuperclass
public abstract class PassedAddress extends Rule {
    private IPDBMatcher address = IPSimpleMatcher.getNilMatcher();
    
    @Column(name="address", nullable=false)
    @Type(type="com.untangle.uvm.type.firewall.IPMatcherUserType")
    public IPDBMatcher getAddress()
    {
        return this.address;
    }

    public void setAddress( IPDBMatcher newValue )
    {
        this.address = newValue;
    }

}
