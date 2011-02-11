/* $HeadURL$ */
package com.untangle.uvm.type;

import com.untangle.uvm.node.ProtocolMatcher;
import com.untangle.uvm.type.StringBasedUserType;

@SuppressWarnings("serial")
public class ProtocolMatcherUserType extends StringBasedUserType
{
    public Class<ProtocolMatcher> returnedClass()
    {
        return ProtocolMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((ProtocolMatcher)v).toString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return new ProtocolMatcher( val );
    }
}
