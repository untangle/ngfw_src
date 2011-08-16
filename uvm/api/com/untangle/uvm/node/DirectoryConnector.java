/*
 * $Id: DirectoryConnector.java,v 1.00 2011/08/15 14:13:23 dmorris Exp $
 */
package com.untangle.uvm.node;

import com.untangle.uvm.user.IpUsernameMap;

public interface DirectoryConnector 
{
    public IpUsernameMap getIpUsernameMap();

    public boolean isMemberOf( String username, String group );

    public boolean adAuthenticate( String username, String group );

    public boolean radiusAuthenticate( String username, String group );
}
