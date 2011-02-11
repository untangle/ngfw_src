/* $HeadURL$ */
package com.untangle.uvm.type;

import com.untangle.uvm.node.PortMatcher;
import com.untangle.uvm.type.StringBasedUserType;

@SuppressWarnings("serial")
public class PortMatcherUserType extends StringBasedUserType
{
    public Class<PortMatcher> returnedClass()
    {
        return PortMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((PortMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return new PortMatcher( val );
    }
}
