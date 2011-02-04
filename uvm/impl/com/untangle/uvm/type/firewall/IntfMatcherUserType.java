/* $HeadURL$ */
package com.untangle.uvm.type.firewall;

import com.untangle.uvm.node.firewall.intf.IntfMatcher;
import com.untangle.uvm.node.firewall.intf.IntfMatcherFactory;
import com.untangle.uvm.type.StringBasedUserType;

@SuppressWarnings("serial")
public class IntfMatcherUserType extends StringBasedUserType
{
    public Class<IntfMatcher> returnedClass()
    {
        return IntfMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((IntfMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return IntfMatcherFactory.parse( val );
    }
}
