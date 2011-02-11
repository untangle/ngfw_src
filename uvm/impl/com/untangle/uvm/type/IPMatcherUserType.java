/* $HeadURL$ */
package com.untangle.uvm.type;

import com.untangle.uvm.node.IPMatcher;
import com.untangle.uvm.type.StringBasedUserType;

@SuppressWarnings("serial")
public class IPMatcherUserType extends StringBasedUserType
{
    public Class<IPMatcher> returnedClass()
    {
        return IPMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((IPMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return new IPMatcher( val );
    }
}
