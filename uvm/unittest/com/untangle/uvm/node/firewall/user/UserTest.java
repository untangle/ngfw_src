/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id: PingTest.java 8515 2007-01-03 00:13:24Z amread $
 */

package com.untangle.uvm.node.firewall.user;

import com.untangle.uvm.node.firewall.ParsingConstants;
import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class UserTest
{
    @Before public void initLog4j()
    {
        BasicConfigurator.configure();
    }

    @Test public void parseSimple() throws Exception
    {
        UserDBMatcher um = UserMatcherFactory.parse(ParsingConstants.MARKER_ANY);
        Assert.assertEquals(um, UserSimpleMatcher.getAllMatcher());
        Assert.assertEquals(um.toString(), ParsingConstants.MARKER_ANY);
        Assert.assertTrue(um.isMatch("foo"));
        um = UserMatcherFactory.parse(ParsingConstants.MARKER_ALL);
        Assert.assertEquals(um, UserSimpleMatcher.getAllMatcher());
        Assert.assertEquals(um.toString(), ParsingConstants.MARKER_ANY);
        Assert.assertTrue(um.isMatch("bar"));
        um = UserMatcherFactory.parse(ParsingConstants.MARKER_WILDCARD);
        Assert.assertEquals(um, UserSimpleMatcher.getAllMatcher());
        Assert.assertEquals(um.toString(), ParsingConstants.MARKER_ANY);
        Assert.assertTrue(um.isMatch(null));
        um = UserMatcherFactory.parse("   " + ParsingConstants.MARKER_ANY);
        Assert.assertEquals(um, UserSimpleMatcher.getAllMatcher());
        Assert.assertEquals(um.toString(), ParsingConstants.MARKER_ANY);
        Assert.assertTrue(um.isMatch("No such; user, lots of punct!"));
        um = UserMatcherFactory.parse(" " + ParsingConstants.MARKER_ALL + "  ");
        Assert.assertEquals(um, UserSimpleMatcher.getAllMatcher());
        Assert.assertEquals(um.toString(), ParsingConstants.MARKER_ANY);
        um = UserMatcherFactory.parse(ParsingConstants.MARKER_WILDCARD + "     ");
        Assert.assertEquals(um, UserSimpleMatcher.getAllMatcher());
        Assert.assertEquals(um.toString(), ParsingConstants.MARKER_ANY);
        um = UserMatcherFactory.parse(ParsingConstants.MARKER_NOTHING);
        Assert.assertEquals(um, UserSimpleMatcher.getNilMatcher());
        Assert.assertEquals(um.toString(), ParsingConstants.MARKER_NOTHING);
        Assert.assertFalse(um.isMatch(null));
        um = UserMatcherFactory.parse("\t" + ParsingConstants.MARKER_NOTHING + " ");
        Assert.assertEquals(um, UserSimpleMatcher.getNilMatcher());
        Assert.assertEquals(um.toString(), ParsingConstants.MARKER_NOTHING);
        Assert.assertFalse(um.isMatch("foo"));
    }

    @Test public void parseSingle() throws Exception
    {
        UserDBMatcher um = UserMatcherFactory.parse("wow");
        Assert.assertFalse(um.equals(UserSimpleMatcher.getAllMatcher()));
        Assert.assertEquals(um.getClass(), UserSingleMatcher.class);
        Assert.assertEquals(um.toString(), "wow");
        Assert.assertTrue(um.isMatch("wow"));
        Assert.assertFalse(um.isMatch(null));
        Assert.assertFalse(um.isMatch("wow "));

        um = UserMatcherFactory.parse(" Wow");
        Assert.assertFalse(um.equals(UserSimpleMatcher.getNilMatcher()));
        Assert.assertEquals(um.getClass(), UserSingleMatcher.class);
        Assert.assertEquals(um.toString(), "Wow");
        Assert.assertTrue(um.isMatch("wow"));
        Assert.assertFalse(um.isMatch(" wow"));

        um = UserMatcherFactory.parse(" Wow, this is a really stupid hostname!  ");
        Assert.assertFalse(um.equals(UserSimpleMatcher.getAllMatcher()));
        Assert.assertEquals(um.getClass(), UserSingleMatcher.class);
        Assert.assertEquals(um.toString(), "Wow, this is a really stupid hostname!");
        Assert.assertTrue(um.isMatch("wow, this is a REALLY stupid hostname!"));
        Assert.assertFalse(um.isMatch("wow"));
        Assert.assertFalse(um.isMatch(null));
    }

    @Test public void parseSet() throws Exception
    {
        UserDBMatcher um = UserMatcherFactory.parse("one;two ; three");
        Assert.assertFalse(um.equals(UserSimpleMatcher.getAllMatcher()));
        Assert.assertEquals(um.getClass(), UserSetMatcher.class);
        Assert.assertEquals(um.toString(), "one ; three ; two");
        Assert.assertTrue(um.isMatch("one"));
        Assert.assertTrue(um.isMatch("two"));
        Assert.assertTrue(um.isMatch("TWO"));
        Assert.assertFalse(um.isMatch("threeh"));
        Assert.assertFalse(um.isMatch("barf"));

        um = UserMatcherFactory.parse("wow;bar");
        Assert.assertFalse(um.equals(UserSimpleMatcher.getAllMatcher()));
        Assert.assertEquals(um.getClass(), UserSetMatcher.class);
        Assert.assertEquals(um.toString(), "bar ; wow");
        Assert.assertTrue(um.isMatch("wow"));
        Assert.assertTrue(um.isMatch("bar"));
        Assert.assertTrue(um.isMatch("bAR"));
        Assert.assertFalse(um.isMatch("wow "));
        Assert.assertFalse(um.isMatch("barf"));

        um = UserMatcherFactory.parse(" \tBar; wOW");
        Assert.assertFalse(um.equals(UserSimpleMatcher.getAllMatcher()));
        Assert.assertEquals(um.getClass(), UserSetMatcher.class);
        Assert.assertEquals(um.toString(), "bar ; wow");
        Assert.assertTrue(um.isMatch("wow"));
        Assert.assertTrue(um.isMatch("bar"));
        Assert.assertTrue(um.isMatch("bAR"));
        Assert.assertFalse(um.isMatch("wow "));
        Assert.assertFalse(um.isMatch("barf"));
    }

    /*
      @Test(expected=ValidateException.class) public void invalidHost() throws Exception
      {
      PingManagerImpl.getInstance().ping( "1.2.!xq221223.4  " );
      }

      @Test(expected=ValidateException.class) public void unknownHost() throws Exception
      {
      PingManagerImpl.getInstance().ping( "www.does.not.exist.com" );
      }
    */
}
