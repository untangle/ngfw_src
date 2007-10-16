/*
 * $HeadURL$
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
      RemotePingManagerImpl.getInstance().ping( "1.2.!xq221223.4  " );
      }

      @Test(expected=ValidateException.class) public void unknownHost() throws Exception
      {
      RemotePingManagerImpl.getInstance().ping( "www.does.not.exist.com" );
      }
    */
}
