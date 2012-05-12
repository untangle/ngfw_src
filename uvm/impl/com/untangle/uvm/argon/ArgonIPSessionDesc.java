/**
 * $Id$
 */
package com.untangle.uvm.argon;

import com.untangle.jnetcap.Netcap;

public interface ArgonIPSessionDesc extends com.untangle.uvm.node.SessionTuple, ArgonSessionDesc
{
    public final short IPPROTO_TCP = Netcap.IPPROTO_TCP;
    public final short IPPROTO_UDP = Netcap.IPPROTO_UDP;

}
