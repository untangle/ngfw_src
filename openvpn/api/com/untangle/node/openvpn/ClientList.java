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
public class ClientList implements Serializable
{ 

    List<VpnClient> clientList;

    public ClientList()
    {
        this( new LinkedList<VpnClient>());
    }

    public ClientList( List<VpnClient> clientList )
    {
        this.clientList = clientList;
    }

    public List<VpnClient> getClientList()
    {
        return this.clientList;
    }
    
    public void setClientList( List<VpnClient> clientList )
    {
        this.clientList = clientList;
    }
   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws Exception
    {
        Set<String> nameSet = new HashSet<String>();
        for ( VpnClient client : this.clientList ) {
            client.validate();
            String name = client.trans_getInternalName();
            if ( !nameSet.add( name )) {
                throw new Exception( "Client and site names must all be unique: '" + name + "'" );
            }
        }
    }
}
