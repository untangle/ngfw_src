package com.untangle.uvm.node;

import com.untangle.uvm.node.firewall.intf.IntfDBMatcher;

public interface RemoteIntfManager
{
    public void loadInterfaceConfig();

    public IntfDBMatcher[] getIntfMatcherEnumeration();
}
