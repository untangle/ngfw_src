/**
 * $Id$
 */
package com.untangle.uvm.argon;

import com.untangle.jnetcap.Netcap;

public interface ArgonIPSessionDesc extends com.untangle.uvm.node.IPSessionDesc, ArgonSessionDesc, SessionEndpoints
{
    public final short IPPROTO_TCP = Netcap.IPPROTO_TCP;
    public final short IPPROTO_UDP = Netcap.IPPROTO_UDP;

}
