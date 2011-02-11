package com.untangle.node.cpd;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;

import com.untangle.uvm.node.Rule;
import com.untangle.uvm.node.IPMatcher;

@SuppressWarnings("serial")
@MappedSuperclass
public abstract class PassedAddress extends Rule
{
    private IPMatcher address = IPMatcher.getNilMatcher();
    
    @Column(name="address", nullable=false)
    @Type(type="com.untangle.uvm.type.IPMatcherUserType")
    public IPMatcher getAddress()
    {
        return this.address;
    }

    public void setAddress( IPMatcher newValue )
    {
        this.address = newValue;
    }
}
