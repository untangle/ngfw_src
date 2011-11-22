/*
 * $Id$
 */
package com.untangle.uvm.localapi;

import com.untangle.uvm.node.IPSessionDesc;
import com.untangle.uvm.node.SessionEndpoints;
import com.untangle.uvm.policy.Policy;

public class SessionMatcherFactory
{
    private SessionMatcherFactory()
    {
    }

    static final SessionMatcher NULL_MATCHER = new SessionMatcher() {
            public boolean isMatch( Policy policy, IPSessionDesc client, IPSessionDesc server )
            {
                return false;
            }
        };

    static final SessionMatcher ALL_MATCHER = new SessionMatcher() {
            public boolean isMatch( Policy policy, IPSessionDesc client, IPSessionDesc server )
            {
                return true;
            }
        };

    static final SessionMatcher TCP_MATCHER = new SessionMatcher() {
            public boolean isMatch( Policy policy, IPSessionDesc client, IPSessionDesc server )
            {
                return ( client.protocol() == SessionEndpoints.PROTO_TCP ) ? true : false;
            }
        };

    static final SessionMatcher UDP_MATCHER = new SessionMatcher() {
            public boolean isMatch( Policy policy, IPSessionDesc client, IPSessionDesc server )
            {

                return ( client.protocol() == SessionEndpoints.PROTO_UDP ) ? true : false;
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

    public static SessionMatcher makePolicyInstance( final Policy policy )
    {
        /* null policy is a service, use an all matcher */
        if ( null == policy ) return getAllInstance();

        return new SessionMatcher() {
                public boolean isMatch( Policy sessionPolicy, IPSessionDesc client, IPSessionDesc server )
                {
                    return ( policy.equals( sessionPolicy ));
                }
            };
    }

}
