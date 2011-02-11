/* $HeadURL$ */
package com.untangle.uvm.type;

import com.untangle.uvm.node.DayOfWeekMatcher;
import com.untangle.uvm.type.StringBasedUserType;

@SuppressWarnings("serial")
public class DayOfWeekMatcherUserType extends StringBasedUserType
{
    public Class<DayOfWeekMatcher> returnedClass()
    {
        return DayOfWeekMatcher.class;
    }

    protected String userTypeToString( Object v )
    {
        return ((DayOfWeekMatcher)v).toDatabaseString();
    }

    public Object createUserType( String val ) throws Exception
    {
        return new DayOfWeekMatcher( val );
    }
}
