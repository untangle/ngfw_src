/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.mvvm.user;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;

import com.untangle.mvvm.tran.HostName;
import com.untangle.mvvm.user.Username;

public class PhoneBookTest
{
    private final TestAssistant assistants[] = {
        new TestAssistant( 1 ),
        new TestAssistant( 2 ),
        new TestAssistant( 15 ),
        new TestAssistant( 75 )
    };

    public PhoneBookTest()
    {
    }
    
    @Test public void basic() throws Exception
    {
        PhoneBookImpl pb = PhoneBookImpl.getInstance();
        pb.clearAssistants();

        pb.registerAssistant( assistants[0] );
        pb.registerAssistant( assistants[1] );

        HostName h = HostName.parse( "foob" );

        assistants[0].n.updateHostName = true;
        assistants[0].n.hostname = h;

        Username uid = Username.parse( "boof" );
        assistants[1].n.updateUsername = true;
        assistants[1].n.username = Username.parse( "boof" );

        UserInfo i = pb.lookup( InetAddress.getByName( "1.2.3.4" ));

        Assert.assertEquals( i.getHostname(), h );
        Assert.assertEquals( i.getUsername(), uid );

        /* Override */
        assistants[0].n.updateHostName = false;
        assistants[0].n.hostname = null;

        assistants[1].n.updateUsername = false;
        assistants[1].n.username = null;

        i = pb.lookup( InetAddress.getByName( "1.2.3.4" ));

        Assert.assertEquals( i.getHostname(), h );
        Assert.assertEquals( i.getUsername(), uid );
    }

    @Test public void register() throws Exception
    {
        
    }

    @Test public void unregister() throws Exception
    {
        
    }
    
    @Test public void registerTwice() throws Exception
    {
        
    }

            
    private static class NewUserInfo
    {
        boolean updateHostName = false;
        HostName hostname;
        boolean updateUsername = false;
        Username username;
        boolean updateState = false;
        UserInfo.LookupState state;

        NewUserInfo()
        {
        }
    }

    private static class TestAssistant implements Assistant
    {
        NewUserInfo  n = new NewUserInfo();
        final int priority;

        TestAssistant( int priority )
        {
            this.priority = priority;
        }

        public void lookup( UserInfo info )
        {
            if ( n.updateHostName ) info.setHostname( n.hostname );
            if ( n.updateUsername ) info.setUsername( n.username );
            if ( n.updateState ) info.setState( n.state );
        }
        
        public int priority()
        {
            return this.priority;
        }
        
    }
}

