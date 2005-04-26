/*
 * Copyright (c) 2003,2004 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.mvvm.argon;

public class SessionMatcherFactory
{
    private SessionMatcherFactory()
    {
    }

    static final SessionMatcher NULL_MATCHER = new SessionMatcher() {
            public boolean isMatch( IPSessionDesc session ) 
            {
                return false;
            }
        };

    static final SessionMatcher ALL_MATCHER = new SessionMatcher() {
            public boolean isMatch( IPSessionDesc session ) 
            {
                return true;
            }
        };

    static final SessionMatcher TCP_MATCHER = new SessionMatcher() {
            public boolean isMatch( IPSessionDesc session ) 
            {
                return ( session.protocol() == IPSessionDesc.IPPROTO_TCP ) ? true : false;
            }
        };
    

    static final SessionMatcher UDP_MATCHER = new SessionMatcher() {
            public boolean isMatch( IPSessionDesc session ) 
            {
                
                return ( session.protocol() == IPSessionDesc.IPPROTO_UDP ) ? true : false;
            }
        };

    public static SessionMatcher getNullInstance()
    {
        return NULL_MATCHER;
    }

    public static SessionMatcher getAllInstance()
    {
        return ALL_MATCHER;
    }

    public static SessionMatcher getTcpInstance()
    {
        return TCP_MATCHER;
    }

    public static SessionMatcher getUdpInstance()
    {
        return UDP_MATCHER;
    }

}
