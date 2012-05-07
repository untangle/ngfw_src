/**
 * $Id$
 */
package com.untangle.node.openvpn;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.untangle.uvm.node.AddressRange;
import com.untangle.uvm.node.AddressValidator;
import com.untangle.uvm.node.Validatable;
import com.untangle.uvm.node.ValidateException;

@SuppressWarnings("serial")
public class ExportList implements Serializable, Validatable
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
            checkList.add( AddressRange.makeNetwork( export.getNetwork().getAddr(), 
                                                     export.getNetmask().getAddr()));
        }

        return checkList;
    }

   
    /** 
     * Validate the object, throw an exception if it is not valid */
    public void validate() throws ValidateException
    {
        /* Determine if all of the addresses are unique */
        AddressValidator.getInstance().validateOverlap( buildAddressRange());
    }
}
