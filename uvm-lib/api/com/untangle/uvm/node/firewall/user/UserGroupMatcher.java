package com.untangle.uvm.node.firewall.user;

import java.util.Collections;
import java.util.List;

import com.untangle.uvm.client.RemoteUvmContextFactory;
import com.untangle.uvm.node.ParseException;
import com.untangle.uvm.node.firewall.Parser;

@SuppressWarnings("serial")
public class UserGroupMatcher extends UserDBMatcher {
    private final String group;

    private UserGroupMatcher( String group )
    {
        this.group = group;
    }

    public boolean isMatch( String user )
    {
        if ( user == null ) {
            return false;
        }
        
        user = user.toLowerCase();
        
        boolean isMemberOf = RemoteUvmContextFactory.context().appAddressBook().isMemberOf(user,this.group);
        
        return isMemberOf;
    }

    public List<String> toDatabaseList()
    {
        return Collections.nCopies( 1, toString());
    }
    
    public String toDatabaseString()
    {
        return UserMatcherConstants.MARKER_GROUP + this.group;
    }
    
    public String toString()
    {
        return UserMatcherConstants.MARKER_GROUP + this.group;
    }

    public static UserDBMatcher makeInstance( String groupName )
    {
        return new UserGroupMatcher( groupName.trim());
    }

    /* This is just for matching a list of interfaces */
    static final Parser<UserDBMatcher> PARSER = new Parser<UserDBMatcher>() 
    {
        public int priority()
        {
            return 10;
        }
        
        public boolean isParseable( String value )
        {
            return !value.contains( UserMatcherConstants.MARKER_SEPERATOR ) &&
            value.contains( UserMatcherConstants.MARKER_GROUP );
        }
        
        public UserDBMatcher parse( String value ) throws ParseException
        {
            if ( !isParseable( value )) {
                throw new ParseException( "Invalid single group matcher '" + value + "'" );
            }
            
            
            return makeInstance( value.replace(UserMatcherConstants.MARKER_GROUP, ""));
        }
    };

}
