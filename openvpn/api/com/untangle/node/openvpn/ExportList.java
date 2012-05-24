/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("serial")
public class ExportList implements Serializable
{ 
    
    List<SiteNetwork> exportList;

    public ExportList()
    {
        this( new LinkedList<SiteNetwork>());
    }

    public ExportList( List<SiteNetwork> exportList )
    {
        this.exportList = exportList;
    }

    public List<SiteNetwork> getExportList()
    {
        return this.exportList;
    }
    
    public void setExportList( List<SiteNetwork> exportList )
    {
        this.exportList = exportList;
    }

    List<AddressRange> buildAddressRange()
    {
        List<AddressRange> checkList = new LinkedList<AddressRange>();

        for ( SiteNetwork export : this.exportList ) {
            checkList.add( AddressRange.makeNetwork( export.getNetwork().getAddr(), export.getNetmask().getAddr()));
        }

        return checkList;
    }

    public void validate() throws Exception { }
}
