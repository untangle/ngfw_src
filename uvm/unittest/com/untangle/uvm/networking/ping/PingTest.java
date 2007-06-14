/*
 * $HeadURL:$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.uvm.networking.ping;

import org.apache.log4j.BasicConfigurator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.untangle.uvm.node.ValidateException;

public class PingTest
{
    @Before public void initLog4j()
    {
        BasicConfigurator.configure();
    }
    
    @Test public void valid() throws Exception
    {
        /* Ping a good host and verify there are no results */
        PingResult pr = PingManagerImpl.getInstance().ping( "  bebe" );
        
        Assert.assertEquals( pr.getPercentAnswered(), 100 );

        System.out.println( "result: " + pr );
    }

    @Test public void invalid() throws Exception
    {
        /* Ping a bad host and verify there are no results */
        PingResult pr = PingManagerImpl.getInstance().ping( "1.2.3.4  " );

        Assert.assertEquals( pr.getPercentAnswered(), 0 );

        System.out.println( "result: " + pr );
    }

    @Test(expected=ValidateException.class) public void invalidHost() throws Exception
    {
        PingManagerImpl.getInstance().ping( "1.2.!xq221223.4  " );
    }

    @Test(expected=ValidateException.class) public void unknownHost() throws Exception
    {
        PingManagerImpl.getInstance().ping( "www.does.not.exist.com" );
    }
}
