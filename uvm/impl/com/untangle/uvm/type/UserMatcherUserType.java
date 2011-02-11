/* $HeadURL$ */
package com.untangle.uvm.type;

import com.untangle.uvm.node.UserMatcher;
import com.untangle.uvm.type.StringBasedUserType;

@SuppressWarnings("serial")
public class UserMatcherUserType extends StringBasedUserType
{
    public Class<UserMatcher> returnedClass()
    {
        return UserMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((UserMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return new UserMatcher( val );
    }
}
