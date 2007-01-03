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

package com.untangle.mvvm.networking.ping;

import org.apache.log4j.BasicConfigurator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.untangle.mvvm.tran.ValidateException;

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
