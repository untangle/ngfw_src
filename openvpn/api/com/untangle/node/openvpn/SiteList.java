/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

@SuppressWarnings("serial")
public class SiteList implements Serializable, Validatable
{

    List<VpnSite> siteList;

    public SiteList()
    {
        this( new LinkedList<VpnSite>());
    }

    public SiteList( List<VpnSite> siteList )
    {
        this.siteList = siteList;
    }

    public List<VpnSite> getSiteList()
    {
        return this.siteList;
    }

    public void setSiteList( List<VpnSite> siteList )
    {
        this.siteList = siteList;
    }

    List<AddressRange> buildAddressRange()
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>();

        for ( VpnSite site : this.siteList ) {
            for ( SiteNetwork siteNetwork : site.getExportedAddressList()) {
                checkList.add( AddressRange.makeNetwork( siteNetwork.getNetwork().getAddr(),
                                                         siteNetwork.getNetmask().getAddr()));
            }
        }

        return checkList;
    }

    /**
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        validate( null );
    }

    void validate( ClientList clientList ) throws ValidateException
    {
        Set<String> nameSet = new HashSet<String>();

        for ( VpnSite site : getSiteList()) {
            site.validate();
            String name = site.trans_getInternalName();
            if ( !nameSet.add( name )) {
                throw new ValidateException( "Client and site names must all be unique: '" + name + "'" );
            }
        }

        /* XXX This assumes that the client list is saved before the site list */
        if ( clientList != null ) {
            for ( VpnClient client : clientList.getClientList()) {
                String name = client.trans_getInternalName();
                if ( !nameSet.add( name )) {
                    throw new ValidateException( "Client and site names must all be unique: '" + name + "'");
                }
            }
        }

        /* XXX Check for overlap, and check for conflicts with the network settings */
        AddressValidator.getInstance().validateOverlap( buildAddressRange());
    }
}
