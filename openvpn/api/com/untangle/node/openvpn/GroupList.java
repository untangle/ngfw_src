/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

@SuppressWarnings("serial")
public class GroupList implements Serializable
{

    List<VpnGroup> groupList;

    public GroupList()
    {
        this( new LinkedList<VpnGroup>());
    }

    public GroupList( List<VpnGroup> groupList )
    {
        this.groupList = groupList;
    }

    public List<VpnGroup> getGroupList()
    {
        return this.groupList;
    }
    
    public void setGroupList( List<VpnGroup> groupList )
    {
        this.groupList = groupList;
    }

    List<AddressRange> buildAddressRange()
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>();

        for ( VpnGroup group : this.groupList ) {
            checkList.add( AddressRange.makeNetwork( group.getAddress().getAddr(), group.getNetmask().getAddr()));
        }
        
        return checkList;
    }
}
