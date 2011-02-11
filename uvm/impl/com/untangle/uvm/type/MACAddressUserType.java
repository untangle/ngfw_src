/* $HeadURL$ */

package com.untangle.uvm.type;

import com.untangle.uvm.node.MACAddress;
import com.untangle.uvm.type.StringBasedUserType;

@SuppressWarnings("serial")
public class MACAddressUserType extends StringBasedUserType
{
    public Class<MACAddress> returnedClass()
    {
        return MACAddress.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((MACAddress)v).toString();
    }

    public Object createUserType( String val )
    {
        try {
            return MACAddress.parse( val );
        } catch ( Exception e ) {
            throw new IllegalArgumentException( "Invalid MACAddress: " + e );
        }
    }
}
